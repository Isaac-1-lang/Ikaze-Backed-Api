package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.ReturnRequestDTO;
import com.ecommerce.dto.DeliveryAgentReturnTableDTO;
import com.ecommerce.dto.DeliveryAgentReturnDetailsDTO;
import com.ecommerce.entity.ReturnRequest;
import com.ecommerce.entity.User;
import com.ecommerce.repository.ReturnRequestRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.DeliveryAgentReturnService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeliveryAgentReturnServiceImpl implements DeliveryAgentReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryAgentReturnTableDTO> getAssignedReturnRequests(
            UUID deliveryAgentId,
            Pageable pageable,
            String returnStatus,
            String deliveryStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String customerName,
            String orderNumber) {
        try {
            // Verify delivery agent exists
            User deliveryAgent = userRepository.findById(deliveryAgentId)
                    .orElseThrow(() -> new RuntimeException("Delivery agent not found"));

            // Build specification for filtering
            Specification<ReturnRequest> spec = buildFilterSpecification(
                    deliveryAgentId, returnStatus, deliveryStatus, startDate, endDate, customerName, orderNumber);

            // Get paginated results
            Page<ReturnRequest> returnRequests = returnRequestRepository.findAll(spec, pageable);

            // Convert to table DTOs
            return returnRequests.map(this::convertToTableDTO);

        } catch (Exception e) {
            log.error("Error retrieving assigned return requests for delivery agent {}: {}",
                    deliveryAgentId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve return requests: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestDTO getReturnRequestById(Long returnRequestId, UUID deliveryAgentId) {
        try {
            ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                    .orElseThrow(() -> new RuntimeException("Return request not found"));

            // Verify the return request is assigned to this delivery agent
            if (returnRequest.getDeliveryAgent() == null ||
                    !returnRequest.getDeliveryAgent().getId().equals(deliveryAgentId)) {
                throw new RuntimeException("Return request not assigned to this delivery agent");
            }

            return convertToDTO(returnRequest);

        } catch (Exception e) {
            log.error("Error retrieving return request {} for delivery agent {}: {}",
                    returnRequestId, deliveryAgentId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve return request: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentReturnDetailsDTO getReturnRequestDetails(Long returnRequestId, UUID deliveryAgentId) {
        try {
            // Verify delivery agent exists
            User deliveryAgent = userRepository.findById(deliveryAgentId)
                    .orElseThrow(() -> new RuntimeException("Delivery agent not found"));

            // Find return request with all necessary relationships including OrderAddress
            ReturnRequest returnRequest = returnRequestRepository.findByIdWithCompleteDeliveryDetails(returnRequestId)
                    .orElseThrow(() -> new RuntimeException("Return request not found"));

            log.debug("Found return request {} with order {}", returnRequest.getId(),
                    returnRequest.getOrder() != null ? returnRequest.getOrder().getId() : "null");

            // Check if OrderAddress exists in database
            boolean hasOrderAddress = returnRequestRepository.hasOrderAddressForReturnRequest(returnRequestId);
            log.debug("Database check - OrderAddress exists for return request {}: {}", returnRequestId,
                    hasOrderAddress);

            if (returnRequest.getOrder() != null) {
                log.debug("Order {} has OrderAddress loaded: {}", returnRequest.getOrder().getId(),
                        returnRequest.getOrder().getOrderAddress() != null ? "YES" : "NO");
            }

            // Verify the return request is assigned to this delivery agent
            if (returnRequest.getDeliveryAgent() == null ||
                    !returnRequest.getDeliveryAgent().getId().equals(deliveryAgentId)) {
                throw new RuntimeException("Return request is not assigned to this delivery agent");
            }

            // Fetch return items separately to avoid MultipleBagFetchException
            ReturnRequest returnRequestWithItems = returnRequestRepository.findByIdWithItems(returnRequestId)
                    .orElse(returnRequest);

            // Copy the items to our main return request
            if (returnRequestWithItems.getReturnItems() != null) {
                returnRequest.setReturnItems(returnRequestWithItems.getReturnItems());
            }

            // Convert to detailed DTO
            return convertToDetailsDTO(returnRequest);

        } catch (Exception e) {
            log.error("Error retrieving return request details {}: {}", returnRequestId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve return request details: " + e.getMessage());
        }
    }

    @Override
    public ReturnRequestDTO updateDeliveryStatus(
            Long returnRequestId,
            UUID deliveryAgentId,
            ReturnRequest.DeliveryStatus deliveryStatus,
            String notes) {
        try {
            ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                    .orElseThrow(() -> new RuntimeException("Return request not found"));

            // Verify the return request is assigned to this delivery agent
            if (returnRequest.getDeliveryAgent() == null ||
                    !returnRequest.getDeliveryAgent().getId().equals(deliveryAgentId)) {
                throw new RuntimeException("Return request not assigned to this delivery agent");
            }

            // Validate status transition
            validateDeliveryStatusTransition(returnRequest.getDeliveryStatus(), deliveryStatus);

            // Update delivery status
            returnRequest.setDeliveryStatus(deliveryStatus);
            returnRequest.setUpdatedAt(LocalDateTime.now());

            // Add notes if provided
            if (notes != null && !notes.trim().isEmpty()) {
                String existingNotes = returnRequest.getDecisionNotes() != null ? returnRequest.getDecisionNotes() : "";
                String timestamp = LocalDateTime.now().toString();
                String newNote = String.format("[%s] Delivery Agent Update: %s", timestamp, notes.trim());
                returnRequest.setDecisionNotes(existingNotes.isEmpty() ? newNote : existingNotes + "\n" + newNote);
            }

            // Save updated return request
            ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);

            log.info("Updated delivery status for return request {} to {} by delivery agent {}",
                    returnRequestId, deliveryStatus, deliveryAgentId);

            return convertToDTO(savedRequest);

        } catch (Exception e) {
            log.error("Error updating delivery status for return request {}: {}",
                    returnRequestId, e.getMessage(), e);
            throw new RuntimeException("Failed to update delivery status: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentStats getDeliveryAgentStats(UUID deliveryAgentId) {
        try {
            // Verify delivery agent exists
            User deliveryAgent = userRepository.findById(deliveryAgentId)
                    .orElseThrow(() -> new RuntimeException("Delivery agent not found"));

            // Get counts for each delivery status
            long totalAssigned = returnRequestRepository.countByDeliveryAgentId(deliveryAgentId);
            long pickupScheduled = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatus(
                    deliveryAgentId, ReturnRequest.DeliveryStatus.PICKUP_SCHEDULED);
            long pickupInProgress = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatus(
                    deliveryAgentId, ReturnRequest.DeliveryStatus.PICKUP_IN_PROGRESS);
            long pickupCompleted = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatus(
                    deliveryAgentId, ReturnRequest.DeliveryStatus.PICKUP_COMPLETED);
            long pickupFailed = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatus(
                    deliveryAgentId, ReturnRequest.DeliveryStatus.PICKUP_FAILED);

            // Calculate success rate
            double successRate = totalAssigned > 0 ? (double) pickupCompleted / totalAssigned * 100 : 0.0;

            return new DeliveryAgentStats(
                    totalAssigned,
                    pickupScheduled,
                    pickupInProgress,
                    pickupCompleted,
                    pickupFailed,
                    successRate);

        } catch (Exception e) {
            log.error("Error retrieving delivery agent stats for {}: {}", deliveryAgentId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve statistics: " + e.getMessage());
        }
    }

    /**
     * Build JPA Specification for filtering return requests
     */
    private Specification<ReturnRequest> buildFilterSpecification(
            UUID deliveryAgentId,
            String returnStatus,
            String deliveryStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String customerName,
            String orderNumber) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by delivery agent ID (required)
            predicates.add(criteriaBuilder.equal(root.get("deliveryAgent").get("id"), deliveryAgentId));

            // Filter by return status
            if (returnStatus != null && !returnStatus.trim().isEmpty()) {
                try {
                    ReturnRequest.ReturnStatus status = ReturnRequest.ReturnStatus.valueOf(returnStatus.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid return status filter: {}", returnStatus);
                }
            }

            // Filter by delivery status
            if (deliveryStatus != null && !deliveryStatus.trim().isEmpty()) {
                try {
                    ReturnRequest.DeliveryStatus status = ReturnRequest.DeliveryStatus
                            .valueOf(deliveryStatus.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("deliveryStatus"), status));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid delivery status filter: {}", deliveryStatus);
                }
            }

            // Filter by date range
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            // Filter by customer name
            if (customerName != null && !customerName.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("order").get("user").get("firstName")),
                        "%" + customerName.toLowerCase() + "%"));
            }

            // Filter by order number
            if (orderNumber != null && !orderNumber.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("order").get("orderNumber")),
                        "%" + orderNumber.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Validate delivery status transition
     */
    private void validateDeliveryStatusTransition(
            ReturnRequest.DeliveryStatus currentStatus,
            ReturnRequest.DeliveryStatus newStatus) {
        // Define valid transitions
        boolean isValidTransition = switch (currentStatus) {
            case ASSIGNED -> newStatus == ReturnRequest.DeliveryStatus.PICKUP_SCHEDULED ||
                    newStatus == ReturnRequest.DeliveryStatus.PICKUP_IN_PROGRESS ||
                    newStatus == ReturnRequest.DeliveryStatus.CANCELLED;
            case PICKUP_SCHEDULED -> newStatus == ReturnRequest.DeliveryStatus.PICKUP_IN_PROGRESS ||
                    newStatus == ReturnRequest.DeliveryStatus.CANCELLED;
            case PICKUP_IN_PROGRESS -> newStatus == ReturnRequest.DeliveryStatus.PICKUP_COMPLETED ||
                    newStatus == ReturnRequest.DeliveryStatus.PICKUP_FAILED;
            case PICKUP_COMPLETED, PICKUP_FAILED, CANCELLED -> false;
            default -> false;
        };

        if (!isValidTransition) {
            throw new RuntimeException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }

    /**
     * Convert ReturnRequest entity to DTO
     */
    private ReturnRequestDTO convertToDTO(ReturnRequest returnRequest) {
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setId(returnRequest.getId());
        dto.setReason(returnRequest.getReason());
        dto.setStatus(returnRequest.getStatus());
        dto.setDeliveryStatus(returnRequest.getDeliveryStatus());
        dto.setCreatedAt(returnRequest.getCreatedAt());
        dto.setUpdatedAt(returnRequest.getUpdatedAt());
        dto.setDecisionNotes(returnRequest.getDecisionNotes());

        // Set order information
        if (returnRequest.getOrder() != null) {
            dto.setOrderId(returnRequest.getOrder().getId());
            dto.setOrderNumber(returnRequest.getOrder().getOrderCode());
            dto.setOrderDate(returnRequest.getOrder().getCreatedAt());
            dto.setTotalAmount(returnRequest.getOrder().getTotalAmount());

            // Set customer information
            if (returnRequest.getOrder().getUser() != null) {
                dto.setCustomerId(returnRequest.getOrder().getUser().getId());
                dto.setCustomerName(returnRequest.getOrder().getOrderCustomerInfo().getFirstName() + " " +
                        returnRequest.getOrder().getOrderCustomerInfo().getLastName());
                dto.setCustomerEmail(returnRequest.getOrder().getOrderCustomerInfo().getEmail());
                dto.setCustomerPhone(returnRequest.getOrder().getOrderCustomerInfo().getPhoneNumber());
            }

            // Set shipping address
            if (returnRequest.getOrder().getOrderAddress() != null) {
                dto.setShippingAddress(returnRequest.getOrder().getOrderAddress());
            }
        }

        // Set delivery agent information
        if (returnRequest.getDeliveryAgent() != null) {
            dto.setDeliveryAgentId(returnRequest.getDeliveryAgent().getId());
            dto.setDeliveryAgentName(returnRequest.getDeliveryAgent().getFirstName() + " " +
                    returnRequest.getDeliveryAgent().getLastName());
        }

        return dto;
    }

    /**
     * Convert ReturnRequest entity to table DTO (streamlined for table display)
     */
    private DeliveryAgentReturnTableDTO convertToTableDTO(ReturnRequest returnRequest) {
        DeliveryAgentReturnTableDTO dto = new DeliveryAgentReturnTableDTO();

        // Basic return request info
        dto.setId(returnRequest.getId());
        dto.setReason(returnRequest.getReason());
        dto.setStatus(returnRequest.getStatus());
        dto.setDeliveryStatus(returnRequest.getDeliveryStatus());
        dto.setCreatedAt(returnRequest.getCreatedAt());

        // Order information
        if (returnRequest.getOrder() != null) {
            dto.setOrderId(returnRequest.getOrder().getId());
            dto.setOrderNumber(returnRequest.getOrder().getOrderCode());
            dto.setOrderDate(returnRequest.getOrder().getCreatedAt());

            // Customer information from OrderCustomerInfo
            if (returnRequest.getOrder().getOrderCustomerInfo() != null) {
                dto.setCustomerName(returnRequest.getOrder().getOrderCustomerInfo().getFirstName() + " " +
                        returnRequest.getOrder().getOrderCustomerInfo().getLastName());
                dto.setCustomerEmail(returnRequest.getOrder().getOrderCustomerInfo().getEmail());
                dto.setCustomerPhone(returnRequest.getOrder().getOrderCustomerInfo().getPhoneNumber());

                // Build customer address from OrderCustomerInfo
                StringBuilder address = new StringBuilder();
                if (returnRequest.getOrder().getOrderCustomerInfo().getStreetAddress() != null) {
                    address.append(returnRequest.getOrder().getOrderCustomerInfo().getStreetAddress());
                }
                if (returnRequest.getOrder().getOrderCustomerInfo().getCity() != null) {
                    if (address.length() > 0)
                        address.append(", ");
                    address.append(returnRequest.getOrder().getOrderCustomerInfo().getCity());
                }
                if (returnRequest.getOrder().getOrderCustomerInfo().getState() != null) {
                    if (address.length() > 0)
                        address.append(", ");
                    address.append(returnRequest.getOrder().getOrderCustomerInfo().getState());
                }
                dto.setCustomerAddress(address.toString());
            }
        }

        // Delivery agent information
        if (returnRequest.getDeliveryAgent() != null) {
            dto.setDeliveryAgentId(returnRequest.getDeliveryAgent().getId());
            dto.setDeliveryAgentName(returnRequest.getDeliveryAgent().getFirstName() + " " +
                    returnRequest.getDeliveryAgent().getLastName());
        }

        return dto;
    }

    /**
     * Convert ReturnRequest entity to detailed DTO for delivery agent details page
     */
    private DeliveryAgentReturnDetailsDTO convertToDetailsDTO(ReturnRequest returnRequest) {
        DeliveryAgentReturnDetailsDTO dto = new DeliveryAgentReturnDetailsDTO();

        // Basic return request info
        dto.setId(returnRequest.getId());
        dto.setReason(returnRequest.getReason());
        dto.setStatus(returnRequest.getStatus());
        dto.setDeliveryStatus(returnRequest.getDeliveryStatus());
        dto.setCreatedAt(returnRequest.getCreatedAt());
        dto.setUpdatedAt(returnRequest.getUpdatedAt());
        dto.setSubmittedAt(returnRequest.getSubmittedAt());
        dto.setDecisionAt(returnRequest.getDecisionAt());
        dto.setDecisionNotes(returnRequest.getDecisionNotes());

        // Order information
        if (returnRequest.getOrder() != null) {
            dto.setOrderId(returnRequest.getOrder().getId());
            dto.setOrderNumber(returnRequest.getOrder().getOrderCode());
            dto.setOrderDate(returnRequest.getOrder().getCreatedAt());
            dto.setOrderTotal(returnRequest.getOrder().getTotalAmount());

            // Customer information
            if (returnRequest.getOrder().getOrderCustomerInfo() != null) {
                DeliveryAgentReturnDetailsDTO.CustomerInfoDTO customer = new DeliveryAgentReturnDetailsDTO.CustomerInfoDTO();
                customer.setName(returnRequest.getOrder().getOrderCustomerInfo().getFirstName() + " " +
                        returnRequest.getOrder().getOrderCustomerInfo().getLastName());
                customer.setEmail(returnRequest.getOrder().getOrderCustomerInfo().getEmail());
                customer.setPhone(returnRequest.getOrder().getOrderCustomerInfo().getPhoneNumber());
                dto.setCustomer(customer);
            }

            // Pickup address with coordinates from OrderAddress
            DeliveryAgentReturnDetailsDTO.PickupAddressDTO address = new DeliveryAgentReturnDetailsDTO.PickupAddressDTO();
            
            if (returnRequest.getOrder() != null && returnRequest.getOrder().getOrderAddress() != null) {
                com.ecommerce.entity.OrderAddress orderAddress = returnRequest.getOrder().getOrderAddress();
                log.debug("Found OrderAddress: street={}, country={}, regions={}, lat={}, lng={}",
                        orderAddress.getStreet(), orderAddress.getCountry(), orderAddress.getRegions(),
                        orderAddress.getLatitude(), orderAddress.getLongitude());
               
                address.setStreet(orderAddress.getStreet());
                address.setCountry(orderAddress.getCountry());
                address.setRegions(orderAddress.getRegions());
                address.setRoadName(orderAddress.getRoadName());
                address.setLatitude(orderAddress.getLatitude());
                address.setLongitude(orderAddress.getLongitude());

                // Build full address
                StringBuilder fullAddress = new StringBuilder();
                if (orderAddress.getStreet() != null)
                    fullAddress.append(orderAddress.getStreet());
                if (orderAddress.getRoadName() != null) {
                    if (fullAddress.length() > 0)
                        fullAddress.append(", ");
                    fullAddress.append(orderAddress.getRoadName());
                }
                if (orderAddress.getRegions() != null) {
                    if (fullAddress.length() > 0)
                        fullAddress.append(", ");
                    fullAddress.append(orderAddress.getRegions());
                }
                if (orderAddress.getCountry() != null) {
                    if (fullAddress.length() > 0)
                        fullAddress.append(", ");
                    fullAddress.append(orderAddress.getCountry());
                }
                address.setFullAddress(fullAddress.toString());
                log.debug("Mapped pickup address: {}", address.getFullAddress());
            } else {
                // Handle missing OrderAddress with proper fallback values
                log.warn("OrderAddress is null for return request {}", returnRequest.getId());
                if (returnRequest.getOrder() == null) {
                    log.warn("Order is null for return request {}", returnRequest.getId());
                } else {
                    log.warn("Order {} exists but has no OrderAddress", returnRequest.getOrder().getId());
                }

                // Set fallback values when OrderAddress is not available
                address.setStreet("Address not available");
                address.setCountry("Not specified");
                address.setRegions("Not specified");
                address.setRoadName("Not specified");
                address.setLatitude(null);
                address.setLongitude(null);
                address.setFullAddress("Pickup address not available - Please contact customer for address details");
                log.debug("Set fallback pickup address for return request {}", returnRequest.getId());
            }
            
            dto.setPickupAddress(address);
        }

        // Return items with product details
        if (returnRequest.getReturnItems() != null && !returnRequest.getReturnItems().isEmpty()) {
            List<DeliveryAgentReturnDetailsDTO.ReturnItemDetailsDTO> itemDTOs = new ArrayList<>();

            for (com.ecommerce.entity.ReturnItem returnItem : returnRequest.getReturnItems()) {
                DeliveryAgentReturnDetailsDTO.ReturnItemDetailsDTO itemDTO = new DeliveryAgentReturnDetailsDTO.ReturnItemDetailsDTO();

                itemDTO.setId(returnItem.getId());
                itemDTO.setReturnQuantity(returnItem.getReturnQuantity());
                itemDTO.setItemReason(returnItem.getItemReason());

                // Order item information
                if (returnItem.getOrderItem() != null) {
                    itemDTO.setOrderQuantity(returnItem.getOrderItem().getQuantity());
                    itemDTO.setUnitPrice(returnItem.getOrderItem().getPrice());
                    itemDTO.setTotalPrice(returnItem.getOrderItem().getPrice().multiply(
                            BigDecimal.valueOf(returnItem.getReturnQuantity())));
                }

                // Product information
                com.ecommerce.entity.Product effectiveProduct = returnItem.getEffectiveProduct();
                if (effectiveProduct != null) {
                    DeliveryAgentReturnDetailsDTO.ProductInfoDTO productDTO = new DeliveryAgentReturnDetailsDTO.ProductInfoDTO();
                    productDTO.setProductId(effectiveProduct.getProductId());
                    productDTO.setName(effectiveProduct.getProductName());
                    productDTO.setDescription(effectiveProduct.getDescription());

                    // Brand information
                    if (effectiveProduct.getBrand() != null) {
                        productDTO.setBrand(effectiveProduct.getBrand().getBrandName());
                    }

                    // Category information
                    if (effectiveProduct.getCategory() != null) {
                        productDTO.setCategory(effectiveProduct.getCategory().getName());
                    }

                    // Product images
                    if (effectiveProduct.getImages() != null && !effectiveProduct.getImages().isEmpty()) {
                        List<String> imageUrls = effectiveProduct.getImages().stream()
                                .map(img -> img.getImageUrl())
                                .collect(java.util.stream.Collectors.toList());
                        productDTO.setImageUrls(imageUrls);
                    } else {
                        productDTO.setImageUrls(new ArrayList<>());
                    }

                    // Return policy information - set defaults for now
                    productDTO.setReturnable(true);
                    productDTO.setReturnWindowDays(30);
                    itemDTO.setReturnable(true);

                    itemDTO.setProduct(productDTO);
                }

                // Variant information if applicable
                if (returnItem.getProductVariant() != null) {
                    DeliveryAgentReturnDetailsDTO.VariantInfoDTO variantDTO = new DeliveryAgentReturnDetailsDTO.VariantInfoDTO();
                    variantDTO.setVariantId(returnItem.getProductVariant().getId());
                    variantDTO.setVariantName(returnItem.getProductVariant().getVariantName());
                    variantDTO.setVariantPrice(returnItem.getProductVariant().getPrice());

                    // Set default values for missing fields
                    variantDTO.setColor("N/A");
                    variantDTO.setSize("N/A");
                    variantDTO.setMaterial("N/A");

                    // Variant images
                    if (returnItem.getProductVariant().getImages() != null &&
                            !returnItem.getProductVariant().getImages().isEmpty()) {
                        List<String> variantImageUrls = returnItem.getProductVariant().getImages().stream()
                                .map(img -> img.getImageUrl())
                                .collect(java.util.stream.Collectors.toList());
                        variantDTO.setVariantImageUrls(variantImageUrls);
                    } else {
                        variantDTO.setVariantImageUrls(new ArrayList<>());
                    }

                    itemDTO.setVariant(variantDTO);
                }

                itemDTOs.add(itemDTO);
            }

            dto.setReturnItems(itemDTOs);
        }

        // Delivery agent information
        if (returnRequest.getDeliveryAgent() != null) {
            dto.setDeliveryAgentId(returnRequest.getDeliveryAgent().getId());
            dto.setDeliveryAgentName(returnRequest.getDeliveryAgent().getFirstName() + " " +
                    returnRequest.getDeliveryAgent().getLastName());
        }

        // Pickup tracking timestamps - set defaults for now since these fields may not
        // exist
        dto.setAssignedAt(returnRequest.getCreatedAt()); // Use creation time as assigned time
        dto.setPickupScheduledAt(null);
        dto.setPickupStartedAt(null);
        dto.setPickupCompletedAt(null);

        return dto;
    }
}
