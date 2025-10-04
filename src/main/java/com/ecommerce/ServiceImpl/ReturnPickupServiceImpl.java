package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.ReturnPickupRequestDTO;
import com.ecommerce.dto.ReturnPickupResponseDTO;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.ReturnPickupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public ReturnPickupResponseDTO processReturnPickup(ReturnPickupRequestDTO pickupRequest, UUID deliveryAgentId) {
        try {
            log.info("Processing return pickup for request {} by delivery agent {}",
                    pickupRequest.getReturnRequestId(), deliveryAgentId);

            validateReturnPickup(pickupRequest, deliveryAgentId);
            
            // Get the return request with all relationships (already validated above)
            ReturnRequest returnRequest = returnRequestRepository
                    .findByIdWithCompleteDeliveryDetails(pickupRequest.getReturnRequestId())
                    .orElseThrow(() -> new RuntimeException("Return request not found with ID: " + pickupRequest.getReturnRequestId()));

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

    /**
     * Validates that the delivery agent is properly assigned to the return request
     */
    private void validateDeliveryAgentAssignment(UUID deliveryAgentId, ReturnRequest returnRequest) {
        // 1. Verify delivery agent exists
        userRepository.findById(deliveryAgentId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery agent not found with ID: " + deliveryAgentId));

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
        // Comprehensive validation using our new validation methods
        validateReturnPickupRequest(pickupRequest);
        
        // Get return request
        ReturnRequest returnRequest = returnRequestRepository
                .findByIdWithCompleteDeliveryDetails(pickupRequest.getReturnRequestId())
                .orElseThrow(() -> new RuntimeException("Return request not found with ID: " + pickupRequest.getReturnRequestId()));
        
        // Validate delivery agent assignment
        validateDeliveryAgentAssignment(deliveryAgentId, returnRequest);
        
        // Validate return request state
        validateReturnRequestState(returnRequest);
        
        // Validate return items
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

            int returnWindowDays = 30;
            if (returnItem.getOrderItem() != null &&
                    returnItem.getOrderItem().getProduct() != null) {
                returnWindowDays = returnItem.getEffectiveProduct().getMaximumDaysForReturn();
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
        // Only restock undamaged items
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
            
            // Ensure pickup started time is set if null
            if (returnRequest.getPickupStartedAt() == null) {
                returnRequest.setPickupStartedAt(now);
                log.debug("Set pickupStartedAt to current timestamp");
            }
            
            log.debug("Set all required timestamps for COMPLETED status");

            // Validate the combination before saving
            validateStatusCombination(returnRequest);
            
            // Debug logging before save
            log.debug("About to save return request with values:");
            log.debug("  ID: {}", returnRequest.getId());
            log.debug("  Status: {}", returnRequest.getStatus());
            log.debug("  DeliveryStatus: {}", returnRequest.getDeliveryStatus());
            log.debug("  DeliveryAgentId: {}", returnRequest.getDeliveryAgentId());
            log.debug("  DecisionAt: {}", returnRequest.getDecisionAt());
            log.debug("  AssignedAt: {}", returnRequest.getAssignedAt());
            log.debug("  PickupCompletedAt: {}", returnRequest.getPickupCompletedAt());
            log.debug("  ActualPickupTime: {}", returnRequest.getActualPickupTime());
            log.debug("  PickupStartedAt: {}", returnRequest.getPickupStartedAt());
            
            returnRequestRepository.save(returnRequest);
            log.info("Successfully updated return request {} status to COMPLETED with delivery status PICKUP_COMPLETED", 
                returnRequest.getId());
            
        } catch (Exception e) {
            log.error("Failed to update return request {} status: {}", returnRequest.getId(), e.getMessage());
            throw new RuntimeException("Failed to update return request status: " + e.getMessage(), e);
        }
    }

    private void updateOrderStatus(Order order) {
        try {
            log.debug("Updating order {} status from {} to RETURNED", 
                order.getOrderId(), order.getOrderStatus());
            
            // Validate current order status
            Order.OrderStatus currentStatus = order.getOrderStatus();
            if (currentStatus == Order.OrderStatus.CANCELLED) {
                throw new IllegalStateException(String.format(
                    "Cannot update cancelled order %s to RETURNED status", order.getOrderId()));
            }
            
            if (currentStatus == Order.OrderStatus.RETURNED) {
                log.warn("Order {} is already marked as RETURNED, skipping update", order.getOrderId());
                return;
            }
            
            order.setOrderStatus(Order.OrderStatus.RETURNED);
            order.setUpdatedAt(LocalDateTime.now());

            orderRepository.save(order);
            log.info("Successfully updated order {} status to RETURNED", order.getOrderId());
            
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
        
        // Validate order status allows return completion
        Order.OrderStatus currentOrderStatus = returnRequest.getOrder().getOrderStatus();
        if (currentOrderStatus == Order.OrderStatus.CANCELLED || 
            currentOrderStatus == Order.OrderStatus.RETURNED) {
            throw new IllegalStateException(String.format(
                "Cannot complete return pickup for order with status: %s", currentOrderStatus));
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
