package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.ReturnPickupRequestDTO;
import com.ecommerce.dto.ReturnPickupResponseDTO;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.MoneyFlowService;
import com.ecommerce.service.ReturnPickupService;
import com.ecommerce.service.RewardService;
import com.ecommerce.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service implementation for handling return pickup operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnPickupServiceImpl implements ReturnPickupService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final OrderItemBatchRepository orderItemBatchRepository;
    private final StockBatchRepository stockBatchRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;
    private final StripeService stripeService;
    private final ShippingCostRepository shippingCostRepository;
    private final MoneyFlowService moneyFlowService;

    @Override
    public ReturnPickupResponseDTO processReturnPickup(ReturnPickupRequestDTO pickupRequest, UUID deliveryAgentId) {
        try {
            log.info("Processing return pickup for request {} by delivery agent {}",
                    pickupRequest.getReturnRequestId(), deliveryAgentId);

            validateReturnPickup(pickupRequest, deliveryAgentId);

            ReturnRequest returnRequest = returnRequestRepository
                    .findByIdWithCompleteDeliveryDetails(pickupRequest.getReturnRequestId())
                    .orElseThrow(() -> new RuntimeException(
                            "Return request not found with ID: " + pickupRequest.getReturnRequestId()));

            List<ReturnPickupResponseDTO.ReturnItemProcessingResult> itemResults = new ArrayList<>();

            for (ReturnPickupRequestDTO.ReturnItemPickupDTO pickupItem : pickupRequest.getReturnItems()) {
                ReturnPickupResponseDTO.ReturnItemProcessingResult result = processReturnItem(
                        pickupItem, returnRequest);
                itemResults.add(result);
            }

            updateOrderStatus(returnRequest.getOrder());

            updateReturnRequestStatus(returnRequest);

            log.info("Successfully completed return pickup for request {}", pickupRequest.getReturnRequestId());

            return new ReturnPickupResponseDTO(
                    pickupRequest.getReturnRequestId(),
                    "Return pickup completed successfully",
                    LocalDateTime.now(),
                    itemResults);

        } catch (Exception e) {
            log.error("Error processing return pickup for request {}: {}",
                    pickupRequest.getReturnRequestId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process return pickup: " + e.getMessage());
        }
    }


    private void validateDeliveryAgentAssignment(UUID deliveryAgentId, ReturnRequest returnRequest) {
        // 1. Verify delivery agent exists
        userRepository.findById(deliveryAgentId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Delivery agent not found with ID: " + deliveryAgentId));

        // 2. Check if return request is assigned to this delivery agent
        if (returnRequest.getDeliveryAgent() == null) {
            throw new IllegalStateException("No delivery agent assigned to this return request");
        }

        if (!returnRequest.getDeliveryAgent().getId().equals(deliveryAgentId)) {
            throw new IllegalStateException(String.format(
                    "Return request is assigned to different delivery agent. Expected: %s, Actual: %s",
                    deliveryAgentId, returnRequest.getDeliveryAgent().getId()));
        }

        log.debug("Delivery agent assignment validation passed for agent {} and request {}",
                deliveryAgentId, returnRequest.getId());
    }

    @Override
    public void validateReturnPickup(ReturnPickupRequestDTO pickupRequest, UUID deliveryAgentId) {
        validateReturnPickupRequest(pickupRequest);

        ReturnRequest returnRequest = returnRequestRepository
                .findByIdWithCompleteDeliveryDetails(pickupRequest.getReturnRequestId())
                .orElseThrow(() -> new RuntimeException(
                        "Return request not found with ID: " + pickupRequest.getReturnRequestId()));

        validateDeliveryAgentAssignment(deliveryAgentId, returnRequest);

        validateReturnRequestState(returnRequest);

        validateReturnItems(pickupRequest, returnRequest);

        validateReturnWindows(pickupRequest, returnRequest);

        log.debug("Return pickup validation passed for request {}", pickupRequest.getReturnRequestId());
    }

    private void validateReturnWindows(ReturnPickupRequestDTO pickupRequest, ReturnRequest returnRequest) {
        LocalDateTime orderDate = returnRequest.getOrder().getCreatedAt();
        LocalDateTime now = LocalDateTime.now();

        for (ReturnPickupRequestDTO.ReturnItemPickupDTO pickupItem : pickupRequest.getReturnItems()) {
            ReturnItem returnItem = returnItemRepository.findById(pickupItem.getReturnItemId())
                    .orElseThrow(() -> new RuntimeException("Return item not found: " + pickupItem.getReturnItemId()));

            int returnWindowDays = 30; // Default return window
            if (returnItem.getOrderItem() != null &&
                    returnItem.getOrderItem().getProduct() != null) {
                Integer productReturnDays = returnItem.getEffectiveProduct().getMaximumDaysForReturn();
                if (productReturnDays != null && productReturnDays > 0) {
                    returnWindowDays = productReturnDays;
                }
            }

            long daysSinceOrder = java.time.temporal.ChronoUnit.DAYS.between(orderDate.toLocalDate(),
                    now.toLocalDate());

            if (daysSinceOrder > returnWindowDays) {
                throw new RuntimeException(String.format(
                        "Return window expired for item %d. Order was placed %d days ago, return window is %d days",
                        pickupItem.getReturnItemId(), daysSinceOrder, returnWindowDays));
            }
        }
    }

    private ReturnPickupResponseDTO.ReturnItemProcessingResult processReturnItem(
            ReturnPickupRequestDTO.ReturnItemPickupDTO pickupItem,
            ReturnRequest returnRequest) {

        try {
            log.debug("Processing return item {} with status {}",
                    pickupItem.getReturnItemId(), pickupItem.getPickupStatus());

            // Get the return item
            ReturnItem returnItem = returnItemRepository.findById(pickupItem.getReturnItemId())
                    .orElseThrow(() -> new RuntimeException("Return item not found: " + pickupItem.getReturnItemId()));

            // Get the order item batches used for this return item
            List<OrderItemBatch> orderItemBatches = orderItemBatchRepository
                    .findByReturnItemId(pickupItem.getReturnItemId());

            if (orderItemBatches.isEmpty()) {
                log.warn("No order item batches found for return item {}", pickupItem.getReturnItemId());
                return createFailedResult(returnItem, "No batch information found for this item");
            }

            // Process restocking based on pickup status
            boolean restockedSuccessfully = false;
            String warehouseName = "";
            String batchNumber = "";
            String message = "";

            if (shouldRestock(pickupItem.getPickupStatus())) {
                RestockResult restockResult = restockReturnedItem(returnItem, orderItemBatches,
                        pickupItem.getPickupStatus());
                restockedSuccessfully = restockResult.isSuccess();
                warehouseName = restockResult.getWarehouseName();
                batchNumber = restockResult.getBatchNumber();
                message = restockResult.getMessage();
            } else {
                message = "Item not restocked due to status: " + pickupItem.getPickupStatus();
                // Get warehouse info for display
                if (!orderItemBatches.isEmpty()) {
                    warehouseName = orderItemBatches.get(0).getWarehouse().getName();
                }
            }

            return new ReturnPickupResponseDTO.ReturnItemProcessingResult(
                    pickupItem.getReturnItemId(),
                    getProductName(returnItem),
                    getVariantName(returnItem),
                    returnItem.getReturnQuantity(),
                    pickupItem.getPickupStatus(),
                    restockedSuccessfully,
                    warehouseName,
                    batchNumber,
                    message);

        } catch (Exception e) {
            log.error("Error processing return item {}: {}", pickupItem.getReturnItemId(), e.getMessage(), e);
            ReturnItem returnItem = returnItemRepository.findById(pickupItem.getReturnItemId()).orElse(null);
            return createFailedResult(returnItem, "Processing failed: " + e.getMessage());
        }
    }

    private boolean shouldRestock(ReturnPickupRequestDTO.ReturnItemPickupStatus status) {
        return status == ReturnPickupRequestDTO.ReturnItemPickupStatus.UNDAMAGED;
    }

    private RestockResult restockReturnedItem(ReturnItem returnItem, List<OrderItemBatch> orderItemBatches,
            ReturnPickupRequestDTO.ReturnItemPickupStatus pickupStatus) {
        try {
            int remainingQuantityToRestock = returnItem.getReturnQuantity();
            StringBuilder batchNumbers = new StringBuilder();
            String warehouseName = "";

            for (OrderItemBatch orderItemBatch : orderItemBatches) {
                if (remainingQuantityToRestock <= 0) {
                    break;
                }

                StockBatch stockBatch = orderItemBatch.getStockBatch();
                int quantityToRestock = Math.min(remainingQuantityToRestock, orderItemBatch.getQuantityUsed());

                // Increase the stock batch quantity
                stockBatch.increaseQuantity(quantityToRestock);
                stockBatchRepository.save(stockBatch);

                remainingQuantityToRestock -= quantityToRestock;

                if (batchNumbers.length() > 0) {
                    batchNumbers.append(", ");
                }
                batchNumbers.append(stockBatch.getBatchNumber());

                if (warehouseName.isEmpty()) {
                    warehouseName = orderItemBatch.getWarehouse().getName();
                }

                log.debug("Restocked {} units to batch {} in warehouse {}",
                        quantityToRestock, stockBatch.getBatchNumber(), warehouseName);
            }

            if (remainingQuantityToRestock > 0) {
                log.warn("Could not restock {} units for return item {}",
                        remainingQuantityToRestock, returnItem.getId());
                return new RestockResult(false, warehouseName, batchNumbers.toString(),
                        "Partially restocked. " + remainingQuantityToRestock + " units could not be restocked.");
            }

            return new RestockResult(true, warehouseName, batchNumbers.toString(),
                    "Successfully restocked " + returnItem.getReturnQuantity() + " units");

        } catch (Exception e) {
            log.error("Error restocking return item {}: {}", returnItem.getId(), e.getMessage(), e);
            return new RestockResult(false, "", "", "Restocking failed: " + e.getMessage());
        }
    }

    private void updateReturnRequestStatus(ReturnRequest returnRequest) {
        try {
            log.debug("Updating return request {} status from {} to COMPLETED",
                    returnRequest.getId(), returnRequest.getStatus());
            log.debug("Updating delivery status from {} to PICKUP_COMPLETED",
                    returnRequest.getDeliveryStatus());

            // Validate current state before update
            if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.APPROVED) {
                throw new IllegalStateException(String.format(
                        "Cannot update status to COMPLETED. Current status is %s, expected APPROVED",
                        returnRequest.getStatus()));
            }

            if (returnRequest.getDeliveryStatus() != ReturnRequest.DeliveryStatus.ASSIGNED) {
                throw new IllegalStateException(String.format(
                        "Cannot update delivery status to PICKUP_COMPLETED. Current status is %s, expected ASSIGNED",
                        returnRequest.getDeliveryStatus()));
            }

            LocalDateTime now = LocalDateTime.now();

            // Update statuses
            returnRequest.setStatus(ReturnRequest.ReturnStatus.COMPLETED);
            returnRequest.setDeliveryStatus(ReturnRequest.DeliveryStatus.PICKUP_COMPLETED);
            returnRequest.setUpdatedAt(now);

            // Set all required timestamps for COMPLETED status
            returnRequest.setPickupCompletedAt(now);

            // Ensure actual pickup time is set (might be required by constraint)
            if (returnRequest.getActualPickupTime() == null) {
                returnRequest.setActualPickupTime(now);
                log.debug("Set actualPickupTime to current timestamp");
            }

            if (returnRequest.getPickupStartedAt() == null) {
                returnRequest.setPickupStartedAt(now);
                log.debug("Set pickupStartedAt to current timestamp");
            }

            log.debug("Set all required timestamps for COMPLETED status");
            validateStatusCombination(returnRequest);
            returnRequestRepository.save(returnRequest);
            try {
                processRefundForCompletedReturn(returnRequest);
            } catch (Exception refundException) {
                log.error("Refund processing failed for return request {}: {}",
                        returnRequest.getId(), refundException.getMessage(), refundException);
            }

            log.info("Successfully updated return request {} status to COMPLETED with delivery status PICKUP_COMPLETED",
                    returnRequest.getId());

        } catch (Exception e) {
            log.error("Failed to update return request {} status: {}", returnRequest.getId(), e.getMessage());
            throw new RuntimeException("Failed to update return request status: " + e.getMessage(), e);
        }
    }

    private void updateOrderStatus(Order order) {
        try {
            log.debug("Updating order {} status to reflect return completion",
                    order.getOrderId());

            // Validate that no shop orders are cancelled
            if (order.getShopOrders() != null) {
                boolean hasCancelled = order.getShopOrders().stream()
                        .anyMatch(so -> so.getStatus() == com.ecommerce.entity.ShopOrder.ShopOrderStatus.CANCELLED);
                if (hasCancelled) {
                    throw new IllegalStateException(String.format(
                            "Cannot update order %s with cancelled shop orders", order.getOrderId()));
                }
            }

            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            log.info("Successfully updated order {} after return completion", order.getOrderId());

        } catch (Exception e) {
            log.error("Failed to update order {} status: {}", order.getOrderId(), e.getMessage());
            throw new RuntimeException("Failed to update order status: " + e.getMessage(), e);
        }
    }

    private String getProductName(ReturnItem returnItem) {
        if (returnItem.getOrderItem() != null && returnItem.getOrderItem().getProduct() != null) {
            return returnItem.getOrderItem().getProduct().getProductName();
        }
        return "Unknown Product";
    }

    private String getVariantName(ReturnItem returnItem) {
        if (returnItem.getOrderItem() != null && returnItem.getOrderItem().getProductVariant() != null) {
            return returnItem.getOrderItem().getProductVariant().getVariantName();
        }
        return "No Variant";
    }

    private ReturnPickupResponseDTO.ReturnItemProcessingResult createFailedResult(ReturnItem returnItem,
            String errorMessage) {
        return new ReturnPickupResponseDTO.ReturnItemProcessingResult(
                returnItem != null ? returnItem.getId() : null,
                returnItem != null ? getProductName(returnItem) : "Unknown",
                returnItem != null ? getVariantName(returnItem) : "Unknown",
                returnItem != null ? returnItem.getReturnQuantity() : 0,
                null,
                false,
                "",
                "",
                errorMessage);
    }

    /**
     * Validates the pickup request DTO
     */
    private void validateReturnPickupRequest(ReturnPickupRequestDTO pickupRequest) {
        if (pickupRequest == null) {
            throw new IllegalArgumentException("Pickup request cannot be null");
        }

        if (pickupRequest.getReturnRequestId() == null) {
            throw new IllegalArgumentException("Return request ID is required");
        }

        if (pickupRequest.getReturnItems() == null || pickupRequest.getReturnItems().isEmpty()) {
            throw new IllegalArgumentException("Return items list cannot be empty");
        }

        // Validate each pickup item
        for (ReturnPickupRequestDTO.ReturnItemPickupDTO item : pickupRequest.getReturnItems()) {
            if (item.getReturnItemId() == null) {
                throw new IllegalArgumentException("Return item ID is required for all items");
            }
            if (item.getPickupStatus() == null) {
                throw new IllegalArgumentException("Pickup status is required for all items");
            }
        }

        log.debug("Pickup request validation passed for return request {}", pickupRequest.getReturnRequestId());
    }

    /**
     * Validates the return request state before processing
     */
    private void validateReturnRequestState(ReturnRequest returnRequest) {
        if (returnRequest == null) {
            throw new IllegalArgumentException("Return request cannot be null");
        }

        // Check return request status
        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.APPROVED) {
            throw new IllegalStateException(String.format(
                    "Return request must be APPROVED to process pickup. Current status: %s",
                    returnRequest.getStatus()));
        }

        // Check delivery status
        if (returnRequest.getDeliveryStatus() != ReturnRequest.DeliveryStatus.ASSIGNED) {
            throw new IllegalStateException(String.format(
                    "Return request delivery status must be ASSIGNED to process pickup. Current status: %s",
                    returnRequest.getDeliveryStatus()));
        }

        // Check if delivery agent is assigned
        if (returnRequest.getDeliveryAgent() == null) {
            throw new IllegalStateException("No delivery agent assigned to this return request");
        }

        log.debug("Return request state validation passed for request {}", returnRequest.getId());
    }

    /**
     * Validates all return items in the pickup request
     */
    private void validateReturnItems(ReturnPickupRequestDTO pickupRequest, ReturnRequest returnRequest) {
        List<Long> requestReturnItemIds = pickupRequest.getReturnItems().stream()
                .map(ReturnPickupRequestDTO.ReturnItemPickupDTO::getReturnItemId)
                .toList();

        List<Long> actualReturnItemIds = returnRequest.getReturnItems().stream()
                .map(ReturnItem::getId)
                .toList();

        // Check if all requested items exist in the return request
        for (Long requestedId : requestReturnItemIds) {
            if (!actualReturnItemIds.contains(requestedId)) {
                throw new IllegalArgumentException(String.format(
                        "Return item with ID %d not found in return request %d",
                        requestedId, returnRequest.getId()));
            }
        }

        // Validate each return item individually
        for (ReturnPickupRequestDTO.ReturnItemPickupDTO pickupItem : pickupRequest.getReturnItems()) {
            ReturnItem returnItem = returnItemRepository.findById(pickupItem.getReturnItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Return item not found with ID: " + pickupItem.getReturnItemId()));

            // Check if return item belongs to the correct return request
            if (!returnItem.getReturnRequest().getId().equals(returnRequest.getId())) {
                throw new IllegalArgumentException(String.format(
                        "Return item %d does not belong to return request %d",
                        pickupItem.getReturnItemId(), returnRequest.getId()));
            }
        }

        log.debug("Return items validation passed for {} items", requestReturnItemIds.size());
    }

    /**
     * Validates that status update is allowed
     */
    private void validateStatusUpdate(ReturnRequest returnRequest) {
        // Check current status
        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.APPROVED) {
            throw new IllegalStateException(String.format(
                    "Cannot complete pickup for return request with status: %s. Must be APPROVED.",
                    returnRequest.getStatus()));
        }

        // Check current delivery status
        if (returnRequest.getDeliveryStatus() != ReturnRequest.DeliveryStatus.ASSIGNED) {
            throw new IllegalStateException(String.format(
                    "Cannot complete pickup for return request with delivery status: %s. Must be ASSIGNED.",
                    returnRequest.getDeliveryStatus()));
        }

        // Check if order exists and is in valid state
        if (returnRequest.getOrder() == null) {
            throw new IllegalStateException("Return request has no associated order");
        }

        // Validate order status allows return completion - check if any shop order is cancelled
        Order order = returnRequest.getOrder();
        if (order.getShopOrders() != null) {
            boolean hasCancelled = order.getShopOrders().stream()
                    .anyMatch(so -> so.getStatus() == com.ecommerce.entity.ShopOrder.ShopOrderStatus.CANCELLED);
            if (hasCancelled) {
                throw new IllegalStateException(String.format(
                        "Cannot complete return pickup for order with cancelled shop orders"));
            }
        }

        log.debug("Status update validation passed for return request {}", returnRequest.getId());
    }

    /**
     * Validates that the status combination is allowed by database constraints
     */
    private void validateStatusCombination(ReturnRequest returnRequest) {
        ReturnRequest.ReturnStatus status = returnRequest.getStatus();
        ReturnRequest.DeliveryStatus deliveryStatus = returnRequest.getDeliveryStatus();

        log.debug("Validating status combination: status={}, deliveryStatus={}", status, deliveryStatus);

        // Check for invalid combinations based on the database constraint
        if (status == ReturnRequest.ReturnStatus.COMPLETED) {
            if (deliveryStatus != ReturnRequest.DeliveryStatus.PICKUP_COMPLETED) {
                throw new IllegalStateException(String.format(
                        "Invalid status combination: COMPLETED status requires PICKUP_COMPLETED delivery status, but got %s",
                        deliveryStatus));
            }
        }

        if (deliveryStatus == ReturnRequest.DeliveryStatus.PICKUP_COMPLETED) {
            if (status != ReturnRequest.ReturnStatus.COMPLETED) {
                throw new IllegalStateException(String.format(
                        "Invalid status combination: PICKUP_COMPLETED delivery status requires COMPLETED status, but got %s",
                        status));
            }
        }

        // Validate that required fields are not null for COMPLETED status
        if (status == ReturnRequest.ReturnStatus.COMPLETED) {
            if (returnRequest.getDeliveryAgent() == null && returnRequest.getDeliveryAgentId() == null) {
                throw new IllegalStateException("COMPLETED status requires a delivery agent to be assigned");
            }

            if (returnRequest.getDecisionAt() == null) {
                throw new IllegalStateException("COMPLETED status requires decision timestamp");
            }

            if (returnRequest.getAssignedAt() == null) {
                throw new IllegalStateException("COMPLETED status requires assignment timestamp");
            }

            if (returnRequest.getPickupCompletedAt() == null) {
                throw new IllegalStateException("COMPLETED status requires pickup completion timestamp");
            }

            // Additional validation for pickup-related timestamps
            if (deliveryStatus == ReturnRequest.DeliveryStatus.PICKUP_COMPLETED) {
                if (returnRequest.getActualPickupTime() == null) {
                    throw new IllegalStateException("PICKUP_COMPLETED delivery status requires actual pickup time");
                }

                if (returnRequest.getPickupStartedAt() == null) {
                    throw new IllegalStateException("PICKUP_COMPLETED delivery status requires pickup started time");
                }
            }
        }

        log.debug("Status combination validation passed");
    }

    /**
     * Process refund for completed return request
     */
    @Transactional
    public void processRefundForCompletedReturn(ReturnRequest returnRequest) {
        log.info("Processing refund for completed return request {}", returnRequest.getId());

        try {
            List<ReturnItem> returnableItems = returnItemRepository.findByReturnRequestId(returnRequest.getId())
                    .stream()
                    .filter(item -> item.getIsReturnable() != null && item.getIsReturnable())
                    .toList();

            if (returnableItems.isEmpty()) {
                log.info("No returnable items found for return request {}, skipping refund", returnRequest.getId());
                return;
            }

            RefundResult refundResult = processReturnRefund(returnRequest, returnableItems);

            if (refundResult.isSuccess()) {
                log.info("Refund processed successfully for return request {}: Total refund value: {}, " +
                        "Card refund: {}, Points refunded: {}",
                        returnRequest.getId(),
                        refundResult.getTotalRefundValue(),
                        refundResult.getCardRefundAmount(),
                        refundResult.getPointsRefunded());

                // Update return request with refund information
                returnRequest.setRefundProcessed(true);
                returnRequest.setRefundAmount(refundResult.getTotalRefundValue());
                returnRequest.setRefundProcessedAt(LocalDateTime.now());
                returnRequestRepository.save(returnRequest);

            } else {
                log.error("Refund processing failed for return request {}: {}",
                        returnRequest.getId(), refundResult.getMessage());
                throw new RuntimeException("Refund processing failed: " + refundResult.getMessage());
            }

        } catch (Exception e) {
            log.error("Error processing refund for completed return {}: {}",
                    returnRequest.getId(), e.getMessage(), e);
            throw new RuntimeException("Refund processing error: " + e.getMessage(), e);
        }
    }

    /**
     * Public method to manually trigger refund processing for a return request
     * This can be used for testing or manual refund processing
     */
    @Transactional
    public RefundResult processManualRefund(Long returnRequestId) {
        log.info("Processing manual refund for return request {}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + returnRequestId));

        List<ReturnItem> returnableItems = returnItemRepository.findByReturnRequestId(returnRequestId)
                .stream()
                .filter(item -> item.getIsReturnable() != null && item.getIsReturnable())
                .toList();

        if (returnableItems.isEmpty()) {
            throw new RuntimeException("No returnable items found for return request: " + returnRequestId);
        }

        return processReturnRefund(returnRequest, returnableItems);
    }

    /**
     * Process comprehensive refund for returned items based on payment method
     */
    @Transactional
    public RefundResult processReturnRefund(ReturnRequest returnRequest, List<ReturnItem> returnedItems) {
        log.info("Processing refund for return request {} with {} items",
                returnRequest.getId(), returnedItems.size());

        try {
            // Get order and transaction details
            Order order = orderRepository.findById(returnRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + returnRequest.getOrderId()));

            OrderTransaction transaction = order.getOrderTransaction();
            if (transaction == null) {
                throw new RuntimeException("No transaction found for order: " + order.getOrderId());
            }

            // Check if all items in the order are being returned
            boolean isFullOrderReturn = isFullOrderReturn(order, returnedItems);

            RefundCalculation refundCalc = calculateRefundAmounts(order, returnedItems, isFullOrderReturn);

            // Process refund based on payment method
            RefundResult result = switch (transaction.getPaymentMethod()) {
                case POINTS -> processPointsOnlyRefund(order, transaction, refundCalc, isFullOrderReturn);
                case HYBRID -> processHybridRefund(order, transaction, refundCalc, isFullOrderReturn);
                case CREDIT_CARD, DEBIT_CARD -> processCardRefund(order, transaction, refundCalc, isFullOrderReturn);
                default ->
                    throw new IllegalArgumentException("Unsupported payment method: " + transaction.getPaymentMethod());
            };

            if (isFullOrderReturn || refundCalc.getTotalRefundAmount().compareTo(transaction.getOrderAmount()) >= 0) {
                transaction.setStatus(OrderTransaction.TransactionStatus.REFUNDED);
                transaction.setUpdatedAt(LocalDateTime.now());
            }

            log.info("Refund processed successfully for return request {}: {}",
                    returnRequest.getId(), result);

            return result;

        } catch (Exception e) {
            log.error("Failed to process refund for return request {}: {}",
                    returnRequest.getId(), e.getMessage(), e);
            throw new RuntimeException("Refund processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate refund amounts for returned items including shipping costs
     */
    private RefundCalculation calculateRefundAmounts(Order order, List<ReturnItem> returnedItems,
            boolean isFullOrderReturn) {
        log.info("Calculating refund amounts for {} items, full order return: {}",
                returnedItems.size(), isFullOrderReturn);

        BigDecimal totalItemRefund = BigDecimal.ZERO;
        BigDecimal totalShippingRefund = BigDecimal.ZERO;
        Map<Long, RefundItemDetail> itemDetails = new HashMap<>();

        // If full order return, refund everything
        if (isFullOrderReturn) {
            totalItemRefund = order.getOrderInfo().getTotalAmount().subtract(
                    order.getOrderInfo().getShippingCost() != null ? order.getOrderInfo().getShippingCost()
                            : BigDecimal.ZERO);
            totalShippingRefund = order.getOrderInfo().getShippingCost() != null
                    ? order.getOrderInfo().getShippingCost()
                    : BigDecimal.ZERO;
        } else {
            // Calculate partial refund
            for (ReturnItem returnItem : returnedItems) {
                OrderItem orderItem = returnItem.getOrderItem();
                if (orderItem == null) {
                    log.warn("Order item not found for return item: {}", returnItem.getId());
                    continue;
                }

                // Use the actual price paid (from OrderItem.price) not current product price
                BigDecimal itemPrice = orderItem.getPrice();
                int returnQuantity = returnItem.getReturnQuantity();
                BigDecimal itemRefundAmount = itemPrice.multiply(BigDecimal.valueOf(returnQuantity));

                totalItemRefund = totalItemRefund.add(itemRefundAmount);

                // Calculate shipping cost for this item based on weight
                BigDecimal itemShippingCost = calculateItemShippingCost(orderItem, returnQuantity);
                totalShippingRefund = totalShippingRefund.add(itemShippingCost);

                itemDetails.put(orderItem.getOrderItemId(), new RefundItemDetail(
                        orderItem.getOrderItemId(),
                        returnQuantity,
                        itemPrice,
                        itemRefundAmount,
                        itemShippingCost));

                log.debug("Item refund calculated - OrderItem: {}, Quantity: {}, Price: {}, Refund: {}, Shipping: {}",
                        orderItem.getOrderItemId(), returnQuantity, itemPrice, itemRefundAmount, itemShippingCost);
            }
        }

        BigDecimal totalRefund = totalItemRefund.add(totalShippingRefund);

        RefundCalculation calculation = new RefundCalculation(
                totalItemRefund,
                totalShippingRefund,
                totalRefund,
                itemDetails,
                isFullOrderReturn);

        log.info("Refund calculation completed - Items: {}, Shipping: {}, Total: {}",
                totalItemRefund, totalShippingRefund, totalRefund);

        return calculation;
    }

    /**
     * Calculate shipping cost for a specific item based on weight
     */
    private BigDecimal calculateItemShippingCost(OrderItem orderItem, int quantity) {
        try {
            Product product = orderItem.getEffectiveProduct();
            if (product == null || product.getProductDetail() == null) {
                return BigDecimal.ZERO;
            }

            BigDecimal itemWeight = product.getProductDetail().getWeightKg();
            if (itemWeight == null || itemWeight.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }

            // Get active shipping cost configuration
            Optional<ShippingCost> shippingCostOpt = shippingCostRepository.findFirstByIsActiveTrue();
            if (shippingCostOpt.isEmpty()) {
                log.warn("No active shipping cost configuration found");
                return BigDecimal.ZERO;
            }

            ShippingCost shippingCost = shippingCostOpt.get();
            BigDecimal weightKgCost = shippingCost.getWeightKgCost();
            if (weightKgCost == null) {
                return BigDecimal.ZERO;
            }

            // Calculate: item_weight * quantity * weight_kg_cost
            BigDecimal totalWeight = itemWeight.multiply(BigDecimal.valueOf(quantity));
            BigDecimal shippingRefund = totalWeight.multiply(weightKgCost);

            log.debug("Shipping cost calculated - Weight: {}kg, Quantity: {}, Rate: {}, Cost: {}",
                    itemWeight, quantity, weightKgCost, shippingRefund);

            return shippingRefund;

        } catch (Exception e) {
            log.error("Error calculating shipping cost for order item {}: {}",
                    orderItem.getOrderItemId(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Process refund for points-only payment
     */
    private RefundResult processPointsOnlyRefund(Order order, OrderTransaction transaction,
            RefundCalculation refundCalc, boolean isFullOrderReturn) {
        log.info("Processing points-only refund for order {}", order.getOrderId());

        try {
            Integer pointsUsed = transaction.getPointsUsed();
            if (pointsUsed == null || pointsUsed <= 0) {
                throw new RuntimeException("No points found to refund");
            }

            User user = order.getUser();
            if (user == null) {
                throw new RuntimeException("User not found for order");
            }

            Integer pointsToRefund;
            if (isFullOrderReturn) {
                // Refund all points used
                pointsToRefund = pointsUsed;
            } else {
                // Calculate proportional points refund
                BigDecimal refundRatio = refundCalc.getTotalRefundAmount().divide(
                        transaction.getOrderAmount(), 4, RoundingMode.HALF_UP);
                pointsToRefund = BigDecimal.valueOf(pointsUsed)
                        .multiply(refundRatio)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            }

            rewardService.refundPointsForCancelledOrder(
                    user.getId(),
                    pointsToRefund,
                    String.format("Refund for return request - Order #%s", order.getOrderCode()));

            log.info("Refunded {} points to user {} for order {}",
                    pointsToRefund, user.getId(), order.getOrderId());

            return new RefundResult(
                    true,
                    "Points refund completed successfully",
                    BigDecimal.ZERO, // No monetary refund
                    pointsToRefund,
                    BigDecimal.valueOf(pointsToRefund).multiply(getPointValue()),
                    refundCalc);

        } catch (Exception e) {
            log.error("Failed to process points refund: {}", e.getMessage(), e);
            return new RefundResult(
                    false,
                    "Points refund failed: " + e.getMessage(),
                    BigDecimal.ZERO,
                    0,
                    BigDecimal.ZERO,
                    refundCalc);
        }
    }

    /**
     * Process refund for hybrid payment (points + card)
     */
    private RefundResult processHybridRefund(Order order, OrderTransaction transaction,
            RefundCalculation refundCalc, boolean isFullOrderReturn) {
        log.info("Processing hybrid refund for order {}", order.getOrderId());

        try {
            BigDecimal totalRefundAmount = refundCalc.getTotalRefundAmount();
            BigDecimal pointsValue = transaction.getPointsValue() != null ? transaction.getPointsValue()
                    : BigDecimal.ZERO;
            Integer pointsUsed = transaction.getPointsUsed() != null ? transaction.getPointsUsed() : 0;
            BigDecimal cardAmount = transaction.getOrderAmount().subtract(pointsValue);

            Integer pointsToRefund = 0;
            BigDecimal cardRefundAmount = BigDecimal.ZERO;
            BigDecimal remainingRefund = totalRefundAmount;

            if (isFullOrderReturn) {
                // Full refund - refund all points and card amount
                pointsToRefund = pointsUsed;
                cardRefundAmount = cardAmount;
            } else {
                // Partial refund - prioritize points first, then card
                if (pointsValue.compareTo(BigDecimal.ZERO) > 0 && remainingRefund.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pointsRefundValue = remainingRefund.min(pointsValue);
                    BigDecimal pointsRefundRatio = pointsRefundValue.divide(pointsValue, 4, RoundingMode.HALF_UP);
                    pointsToRefund = BigDecimal.valueOf(pointsUsed)
                            .multiply(pointsRefundRatio)
                            .setScale(0, RoundingMode.HALF_UP)
                            .intValue();

                    remainingRefund = remainingRefund.subtract(pointsRefundValue);
                }

                // Refund remaining amount via card
                if (remainingRefund.compareTo(BigDecimal.ZERO) > 0) {
                    cardRefundAmount = remainingRefund.min(cardAmount);
                }
            }

            // Process points refund
            if (pointsToRefund > 0) {
                User user = order.getUser();
                if (user != null) {
                    rewardService.refundPointsForCancelledOrder(
                            user.getId(),
                            pointsToRefund,
                            String.format("Hybrid refund for return - Order #%s", order.getOrderCode()));
                    log.info("Refunded {} points to user {}", pointsToRefund, user.getId());
                }
            }

            // Process card refund
            boolean cardRefundSuccess = true;
            if (cardRefundAmount.compareTo(BigDecimal.ZERO) > 0) {
                cardRefundSuccess = processStripeRefund(transaction, cardRefundAmount);
                
                // Record money flow OUT for successful card portion of hybrid refund
                if (cardRefundSuccess) {
                    recordMoneyFlowForRefund(order, cardRefundAmount, "Hybrid refund (card portion) for return");
                }
            }

            boolean overallSuccess = (pointsToRefund == 0 || pointsToRefund > 0) && cardRefundSuccess;

            return new RefundResult(
                    overallSuccess,
                    overallSuccess ? "Hybrid refund completed successfully" : "Hybrid refund partially failed",
                    cardRefundAmount,
                    pointsToRefund,
                    BigDecimal.valueOf(pointsToRefund).multiply(getPointValue()),
                    refundCalc);

        } catch (Exception e) {
            log.error("Failed to process hybrid refund: {}", e.getMessage(), e);
            return new RefundResult(
                    false,
                    "Hybrid refund failed: " + e.getMessage(),
                    BigDecimal.ZERO,
                    0,
                    BigDecimal.ZERO,
                    refundCalc);
        }
    }

    /**
     * Process refund for card-only payment
     */
    private RefundResult processCardRefund(Order order, OrderTransaction transaction,
            RefundCalculation refundCalc, boolean isFullOrderReturn) {
        log.info("Processing card refund for order {}", order.getOrderId());

        try {
            BigDecimal refundAmount = isFullOrderReturn ? transaction.getOrderAmount()
                    : refundCalc.getTotalRefundAmount();

            boolean success = processStripeRefund(transaction, refundAmount);
            
            // Record money flow OUT for successful card refund
            if (success) {
                recordMoneyFlowForRefund(order, refundAmount, "Card refund for return");
            }

            return new RefundResult(
                    success,
                    success ? "Card refund completed successfully" : "Card refund failed",
                    success ? refundAmount : BigDecimal.ZERO,
                    0,
                    BigDecimal.ZERO,
                    refundCalc);

        } catch (Exception e) {
            log.error("Failed to process card refund: {}", e.getMessage(), e);
            return new RefundResult(
                    false,
                    "Card refund failed: " + e.getMessage(),
                    BigDecimal.ZERO,
                    0,
                    BigDecimal.ZERO,
                    refundCalc);
        }
    }

    /**
     * Process Stripe refund
     */
    private boolean processStripeRefund(OrderTransaction transaction, BigDecimal refundAmount) {
        try {
            if (transaction.getStripePaymentIntentId() == null) {
                log.error("No Stripe payment intent ID found for transaction {}",
                        transaction.getOrderTransactionId());
                return false;
            }

            // Convert to cents for Stripe
            long refundAmountCents = refundAmount.multiply(BigDecimal.valueOf(100)).longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(transaction.getStripePaymentIntentId())
                    .setAmount(refundAmountCents)
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .build();

            Refund refund = Refund.create(params);

            log.info("Stripe refund created successfully - ID: {}, Amount: {}, Status: {}",
                    refund.getId(), refundAmount, refund.getStatus());

            return "succeeded".equals(refund.getStatus()) || "pending".equals(refund.getStatus());

        } catch (StripeException e) {
            log.error("Stripe refund failed for transaction {}: {}",
                    transaction.getOrderTransactionId(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during Stripe refund: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if all items in the order are being returned
     */
    private boolean isFullOrderReturn(Order order, List<ReturnItem> returnedItems) {
        Map<Long, Integer> orderItemQuantities = new HashMap<>();
        for (OrderItem orderItem : order.getAllItems()) {
            orderItemQuantities.put(orderItem.getOrderItemId(), orderItem.getQuantity());
        }

        Map<Long, Integer> returnItemQuantities = new HashMap<>();
        for (ReturnItem returnItem : returnedItems) {
            Long orderItemId = returnItem.getOrderItem().getOrderItemId();
            returnItemQuantities.merge(orderItemId, returnItem.getReturnQuantity(), Integer::sum);
        }

        // Check if all order items are being returned in full quantities
        for (Map.Entry<Long, Integer> entry : orderItemQuantities.entrySet()) {
            Long orderItemId = entry.getKey();
            Integer orderQuantity = entry.getValue();
            Integer returnQuantity = returnItemQuantities.getOrDefault(orderItemId, 0);

            if (!returnQuantity.equals(orderQuantity)) {
                return false;
            }
        }

        return orderItemQuantities.size() == returnItemQuantities.size();
    }

    /**
     * Find order item by ID
     */
    private OrderItem findOrderItemById(Order order, Long orderItemId) {
        return order.getAllItems().stream()
                .filter(item -> item.getOrderItemId().equals(orderItemId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get current point value from reward system
     */
    private BigDecimal getPointValue() {
        try {
            var rewardSystem = rewardService.getActiveRewardSystem();
            return rewardSystem != null ? rewardSystem.getPointValue() : BigDecimal.valueOf(0.01); // Default 1 cent per
                                                                                                   // point
        } catch (Exception e) {
            log.warn("Could not get point value, using default: {}", e.getMessage());
            return BigDecimal.valueOf(0.01);
        }
    }

    /**
     * Refund calculation result
     */
    public static class RefundCalculation {
        private final BigDecimal itemRefundAmount;
        private final BigDecimal shippingRefundAmount;
        private final BigDecimal totalRefundAmount;
        private final Map<Long, RefundItemDetail> itemDetails;
        private final boolean isFullOrderReturn;

        public RefundCalculation(BigDecimal itemRefundAmount, BigDecimal shippingRefundAmount,
                BigDecimal totalRefundAmount, Map<Long, RefundItemDetail> itemDetails,
                boolean isFullOrderReturn) {
            this.itemRefundAmount = itemRefundAmount;
            this.shippingRefundAmount = shippingRefundAmount;
            this.totalRefundAmount = totalRefundAmount;
            this.itemDetails = itemDetails;
            this.isFullOrderReturn = isFullOrderReturn;
        }

        // Getters
        public BigDecimal getItemRefundAmount() {
            return itemRefundAmount;
        }

        public BigDecimal getShippingRefundAmount() {
            return shippingRefundAmount;
        }

        public BigDecimal getTotalRefundAmount() {
            return totalRefundAmount;
        }

        public Map<Long, RefundItemDetail> getItemDetails() {
            return itemDetails;
        }

        public boolean isFullOrderReturn() {
            return isFullOrderReturn;
        }
    }

    /**
     * Individual item refund details
     */
    public static class RefundItemDetail {
        private final Long orderItemId;
        private final Integer quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal itemRefundAmount;
        private final BigDecimal shippingRefundAmount;

        public RefundItemDetail(Long orderItemId, Integer quantity, BigDecimal unitPrice,
                BigDecimal itemRefundAmount, BigDecimal shippingRefundAmount) {
            this.orderItemId = orderItemId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.itemRefundAmount = itemRefundAmount;
            this.shippingRefundAmount = shippingRefundAmount;
        }

        // Getters
        public Long getOrderItemId() {
            return orderItemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getItemRefundAmount() {
            return itemRefundAmount;
        }

        public BigDecimal getShippingRefundAmount() {
            return shippingRefundAmount;
        }
    }

    /**
     * Refund processing result
     */
    public static class RefundResult {
        private final boolean success;
        private final String message;
        private final BigDecimal cardRefundAmount;
        private final Integer pointsRefunded;
        private final BigDecimal pointsRefundValue;
        private final RefundCalculation calculation;

        public RefundResult(boolean success, String message, BigDecimal cardRefundAmount,
                Integer pointsRefunded, BigDecimal pointsRefundValue, RefundCalculation calculation) {
            this.success = success;
            this.message = message;
            this.cardRefundAmount = cardRefundAmount;
            this.pointsRefunded = pointsRefunded;
            this.pointsRefundValue = pointsRefundValue;
            this.calculation = calculation;
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public BigDecimal getCardRefundAmount() {
            return cardRefundAmount;
        }

        public Integer getPointsRefunded() {
            return pointsRefunded;
        }

        public BigDecimal getPointsRefundValue() {
            return pointsRefundValue;
        }

        public RefundCalculation getCalculation() {
            return calculation;
        }

        public BigDecimal getTotalRefundValue() {
            return cardRefundAmount.add(pointsRefundValue);
        }

        @Override
        public String toString() {
            return String.format(
                    "RefundResult{success=%s, cardRefund=%s, pointsRefunded=%d, pointsValue=%s, totalValue=%s}",
                    success, cardRefundAmount, pointsRefunded, pointsRefundValue, getTotalRefundValue());
        }
    }


    private void recordMoneyFlowForRefund(Order order, BigDecimal refundAmount, String description) {
        try {
            com.ecommerce.dto.CreateMoneyFlowDTO createMoneyFlowDTO = com.ecommerce.dto.CreateMoneyFlowDTO.builder()
                    .type(com.ecommerce.enums.MoneyFlowType.OUT)
                    .amount(refundAmount)
                    .description(String.format("%s - Order #%s", description, order.getOrderCode()))
                    .build();
            com.ecommerce.entity.MoneyFlow savedFlow = moneyFlowService.save(createMoneyFlowDTO);
            
            log.info("Recorded money flow OUT: Amount={}, Order={}, MoneyFlow ID={}, New Balance={}, CreatedAt={}", 
                    refundAmount, order.getOrderCode(), savedFlow.getId(), 
                    savedFlow.getRemainingBalance(), savedFlow.getCreatedAt());
                    
        } catch (Exception e) {
            log.error("Failed to record money flow for refund on order {}: {}", 
                    order.getOrderCode(), e.getMessage(), e);
        }
    }

    /**
     * Helper class for restock operation results
     */
    private static class RestockResult {
        private final boolean success;
        private final String warehouseName;
        private final String batchNumber;
        private final String message;

        public RestockResult(boolean success, String warehouseName, String batchNumber, String message) {
            this.success = success;
            this.warehouseName = warehouseName;
            this.batchNumber = batchNumber;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getWarehouseName() {
            return warehouseName;
        }

        public String getBatchNumber() {
            return batchNumber;
        }

        public String getMessage() {
            return message;
        }
    }
}
