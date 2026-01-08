package com.ecommerce.service;

import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling return requests in multivendor architecture
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnMediaRepository returnMediaRepository;
    private final ReturnItemRepository returnItemRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final RefundService refundService;
    private final CloudinaryService cloudinaryService;
    private final OrderTrackingTokenRepository orderTrackingTokenRepository;
    private final OrderActivityLogService activityLogService;

    private static final int DEFAULT_RETURN_DAYS = 15;
    private static final int MAX_IMAGES = 5;
    private static final int MAX_VIDEOS = 1;

    /**
     * Submit a new return request for authenticated users with media files
     */
    public ReturnRequestDTO submitReturnRequest(SubmitReturnRequestDTO submitDTO, MultipartFile[] mediaFiles) {
        log.info("Processing authenticated return request for customer {} and order {}",
                submitDTO.getCustomerId(), submitDTO.getOrderId());

        // Validate that this is an authenticated user request
        if (submitDTO.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required for authenticated return requests");
        }

        Order order = validateOrderForAuthenticatedUser(submitDTO.getOrderId(), submitDTO.getCustomerId());

        validateReturnEligibility(order);
        validateCustomerReturnHistory(submitDTO.getCustomerId());
        validateReturnItems(submitDTO.getReturnItems(), order);
        validateReturnItemsEligibility(submitDTO.getReturnItems(), order);
        validateReturnRequestLimit(submitDTO.getReturnItems());
        if (mediaFiles != null && mediaFiles.length > 0) {
            validateMediaFiles(mediaFiles);
        }

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrderId(submitDTO.getOrderId());
        returnRequest.setCustomerId(submitDTO.getCustomerId());
        returnRequest.setReason(submitDTO.getReason());
        returnRequest.setStatus(ReturnRequest.ReturnStatus.PENDING);
        returnRequest.setSubmittedAt(LocalDateTime.now());

        ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);

        createReturnItems(savedRequest, submitDTO.getReturnItems(), order);

        if (mediaFiles != null && mediaFiles.length > 0) {
            try {
                processMediaAttachments(savedRequest.getId(), mediaFiles);
            } catch (IOException e) {
                log.error("Failed to process media attachments for return request {}: {}",
                        savedRequest.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to upload media files", e);
            }
        }

        notificationService.notifyReturnSubmitted(savedRequest);
        log.info("Authenticated return request {} submitted successfully for order {}",
                savedRequest.getId(), submitDTO.getOrderId());

        // LOG ACTIVITY: Return Requested
        String customerName = order.getUser() != null
                ? order.getUser().getFirstName() + " " + order.getUser().getLastName()
                : order.getOrderCustomerInfo().getFullName();
        activityLogService.logReturnRequested(
                order.getOrderId(),
                customerName,
                submitDTO.getReason(),
                savedRequest.getId());

        return convertToDTO(savedRequest);
    }

    /**
     * Submit a return request using tracking token (for guest users)
     */
    public ReturnRequestDTO submitTokenizedReturnRequest(TokenizedReturnRequestDTO submitDTO,
            MultipartFile[] mediaFiles) {
        log.info("Processing tokenized return request for order number {} with tracking token",
                submitDTO.getOrderNumber());

        // Validate that this is a tokenized request
        if (submitDTO.getOrderNumber() == null || submitDTO.getOrderNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Order number is required for tokenized return requests");
        }
        if (submitDTO.getTrackingToken() == null || submitDTO.getTrackingToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Tracking token is required for tokenized return requests");
        }

        // Validate tracking token and get associated email
        String email = validateTrackingToken(submitDTO.getTrackingToken());

        // Find order by order number
        Order order = orderRepository.findByOrderCode(submitDTO.getOrderNumber())
                .orElseThrow(
                        () -> new RuntimeException("Order not found with order number: " + submitDTO.getOrderNumber()));

        // Verify the order belongs to the email associated with the token
        if (order.getOrderCustomerInfo() == null ||
                !email.equalsIgnoreCase(order.getOrderCustomerInfo().getEmail())) {
            throw new RuntimeException("Order does not belong to the email associated with this tracking token");
        }

        // Ensure this is actually a guest order (no associated user)
        if (order.getUser() != null) {
            throw new RuntimeException(
                    "This order belongs to a registered user and cannot be returned using tracking token");
        }

        validateReturnEligibility(order);
        validateReturnItems(submitDTO.getReturnItems(), order);
        validateReturnItemsEligibility(submitDTO.getReturnItems(), order);
        validateReturnRequestLimit(submitDTO.getReturnItems());
        if (mediaFiles != null && mediaFiles.length > 0) {
            validateMediaFiles(mediaFiles);
        }

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrderId(order.getOrderId());
        returnRequest.setCustomerId(null);
        returnRequest.setReason(submitDTO.getReason());
        returnRequest.setStatus(ReturnRequest.ReturnStatus.PENDING);
        returnRequest.setSubmittedAt(LocalDateTime.now());

        ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);

        createReturnItems(savedRequest, submitDTO.getReturnItems(), order);

        if (mediaFiles != null && mediaFiles.length > 0) {
            try {
                processMediaAttachments(savedRequest.getId(), mediaFiles);
            } catch (IOException e) {
                log.error("Failed to process media attachments for tokenized return request {}: {}",
                        savedRequest.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to upload media files", e);
            }
        }

        notificationService.notifyReturnSubmitted(savedRequest);
        log.info("Tokenized return request {} submitted successfully for order {}",
                savedRequest.getId(), submitDTO.getOrderNumber());

        // LOG ACTIVITY: Return Requested (Guest)
        String guestCustomerName = order.getOrderCustomerInfo() != null
                ? order.getOrderCustomerInfo().getFullName()
                : "Guest Customer";
        activityLogService.logReturnRequested(
                order.getOrderId(),
                guestCustomerName + " (Guest)",
                submitDTO.getReason(),
                savedRequest.getId());

        return convertToDTO(savedRequest);
    }

    public ReturnRequestDTO reviewReturnRequest(ReturnDecisionDTO decisionDTO) {
        ReturnRequest returnRequest = returnRequestRepository.findById(decisionDTO.getReturnRequestId())
                .orElseThrow(
                        () -> new RuntimeException("Return request not found: " + decisionDTO.getReturnRequestId()));

        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.PENDING) {
            throw new RuntimeException("Return request is not in pending status");
        }

        if ("APPROVED".equals(decisionDTO.getDecision())) {
            approveReturnRequest(returnRequest, decisionDTO);
        } else if ("DENIED".equals(decisionDTO.getDecision())) {
            denyReturnRequest(returnRequest, decisionDTO);
        } else {
            throw new IllegalArgumentException("Invalid decision: " + decisionDTO.getDecision());
        }

        ReturnRequest updatedRequest = returnRequestRepository.save(returnRequest);

        return convertToDTO(updatedRequest);
    }

    /**
     * Get return requests by customer with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReturnRequestDTO> getReturnRequestsByCustomer(UUID customerId, Pageable pageable) {
        Page<ReturnRequest> requests = returnRequestRepository.findByCustomerIdWithDetails(customerId, pageable);
        return requests.map(this::convertToDTO);
    }

    /**
     * Get return requests by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReturnRequestDTO> getReturnRequestsByStatus(ReturnRequest.ReturnStatus status, Pageable pageable) {
        Page<ReturnRequest> requests = returnRequestRepository.findByStatusWithDetails(status, pageable);
        return requests.map(this::convertToDTO);
    }

    /**
     * Get return requests by order ID for authenticated users
     */
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getReturnRequestsByOrderId(Long orderId, UUID customerId) {
        log.info("Fetching return requests for order {} and customer {}", orderId, customerId);

        // Validate order belongs to customer
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getUser() == null || !order.getUser().getId().equals(customerId)) {
            throw new RuntimeException("Order does not belong to this customer");
        }

        // Get all return requests for this order
        List<ReturnRequest> requests = returnRequestRepository.findByOrderIdOrderBySubmittedAtDesc(orderId);

        log.info("Found {} return requests for order {}", requests.size(), orderId);

        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get return request by ID with all related data
     */
    @Transactional(readOnly = true)
    public ReturnRequestDTO getReturnRequestById(Long id) {
        ReturnRequest request = returnRequestRepository.findByIdWithBasicData(id)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + id));

        return convertToDTO(request);
    }

    /**
     * Get return requests for guest orders (admin use only)
     */
    @Transactional(readOnly = true)
    public Page<ReturnRequestDTO> getGuestReturnRequests(Pageable pageable) {
        Page<ReturnRequest> requests = returnRequestRepository.findGuestReturnRequests(pageable);
        return requests.map(this::convertToDTO);
    }

    /**
     * Get return requests by order number and tracking token (for guest users)
     */
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getReturnRequestsByOrderNumberAndToken(String orderNumber, String token) {
        log.info("Fetching return requests for guest order {}", orderNumber);
        
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Order number is required");
        }
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Tracking token is required");
        }
        
        // Validate tracking token
        String email = validateTrackingToken(token);
        
        // Find order by order number
        Order order = orderRepository.findByOrderCode(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with order number: " + orderNumber));
        
        // Verify the order belongs to the email associated with the token
        if (order.getOrderCustomerInfo() == null ||
                !email.equalsIgnoreCase(order.getOrderCustomerInfo().getEmail())) {
            throw new RuntimeException("Order does not belong to the email associated with this tracking token");
        }
        
        // Ensure this is actually a guest order (no associated user)
        if (order.getUser() != null) {
            throw new RuntimeException("This order belongs to a registered user and cannot be accessed using tracking token");
        }
        
        // Get all return requests for this order
        List<ReturnRequest> requests = returnRequestRepository.findByOrderIdOrderBySubmittedAtDesc(order.getOrderId());
        
        log.info("Found {} return requests for guest order {}", requests.size(), orderNumber);
        
        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all return requests with comprehensive filtering (Admin use only)
     */
    @Transactional(readOnly = true)
    public Page<ReturnRequestDTO> getAllReturnRequestsWithFilters(
            ReturnRequest.ReturnStatus status,
            String customerType,
            String search,
            String dateFrom,
            String dateTo,
            Pageable pageable) {

        log.info("Retrieving return requests with filters - status: {}, customerType: {}, search: {}",
                status, customerType, search);

        // Get all return requests using standard findAll to avoid query issues
        List<ReturnRequest> allRequestsList = returnRequestRepository.findAll();

        // Apply filtering in Java
        List<ReturnRequest> filteredRequests = allRequestsList.stream()
                .filter(rr -> {
                    // Status filter
                    if (status != null && !rr.getStatus().equals(status)) {
                        return false;
                    }

                    // Customer type filter
                    if (customerType != null && !"ALL".equals(customerType)) {
                        if ("REGISTERED".equals(customerType) && rr.getCustomerId() == null) {
                            return false;
                        }
                        if ("GUEST".equals(customerType) && rr.getCustomerId() != null) {
                            return false;
                        }
                    }

                    // Search filter
                    if (search != null && !search.trim().isEmpty()) {
                        String searchLower = search.toLowerCase();
                        boolean matches = false;

                        // Search in order code (with safe access)
                        try {
                            if (rr.getOrder() != null && rr.getOrder().getOrderCode() != null) {
                                matches |= rr.getOrder().getOrderCode().toLowerCase().contains(searchLower);
                            }
                        } catch (Exception e) {
                            // Ignore lazy loading exceptions for search
                        }

                        if (!matches) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted((rr1, rr2) -> rr2.getSubmittedAt().compareTo(rr1.getSubmittedAt()))
                .collect(Collectors.toList());

        // Create a new Page with filtered results
        int start = Math.min((int) pageable.getOffset(), filteredRequests.size());
        int end = Math.min((start + pageable.getPageSize()), filteredRequests.size());
        List<ReturnRequest> pageContent = start < filteredRequests.size() ? filteredRequests.subList(start, end)
                : new ArrayList<>();

        Page<ReturnRequest> filteredPage = new PageImpl<>(pageContent, pageable, filteredRequests.size());

        return filteredPage.map(this::convertToDTO);
    }

    /**
     * Complete quality control check (wrapper for processQualityControl)
     */
    public void completeQualityControl(QualityControlDTO qcDTO) {
        processQualityControl(qcDTO);
    }

    /**
     * Process quality control check
     */
    public void processQualityControl(QualityControlDTO qcDTO) {
        log.info("Processing quality control for return request {}", qcDTO.getReturnRequestId());

        ReturnRequest returnRequest = returnRequestRepository.findById(qcDTO.getReturnRequestId())
                .orElseThrow(() -> new RuntimeException("Return request not found: " + qcDTO.getReturnRequestId()));

        if ("PASSED".equals(qcDTO.getQcResult())) {
            processQCPassed(returnRequest, qcDTO);
        } else if ("FAILED".equals(qcDTO.getQcResult())) {
            processQCFailed(returnRequest, qcDTO);
        } else {
            throw new IllegalArgumentException("Invalid QC result: " + qcDTO.getQcResult());
        }
    }

    private void processQCPassed(ReturnRequest returnRequest, QualityControlDTO qcDTO) {
        // Product passed QC, can be restocked
        log.info("QC passed for return request {}, proceeding with restocking", returnRequest.getId());
        // Trigger restocking process
    }

    private void processQCFailed(ReturnRequest returnRequest, QualityControlDTO qcDTO) {
        // Product failed QC, mark as non-resellable
        log.info("QC failed for return request {}, marking as non-resellable", returnRequest.getId());
        // Update batch status based on failure reason
    }

    /**
     * Count return requests by status
     */
    public long countReturnRequestsByStatus(ReturnRequest.ReturnStatus status) {
        return returnRequestRepository.countByStatus(status);
    }

    // Private helper methods

    /**
     * Validate tracking token and return associated email
     */
    private String validateTrackingToken(String trackingToken) {
        log.info("Validating tracking token for return request submission");

        // Find valid token
        Optional<OrderTrackingToken> tokenOpt = orderTrackingTokenRepository
                .findValidToken(trackingToken, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired tracking token");
        }

        OrderTrackingToken token = tokenOpt.get();
        String email = token.getEmail();

        log.info("Tracking token validated successfully for email: {}", email);
        return email;
    }

    /**
     * Validate order for authenticated user using orderId and userId
     * Updated for multivendor architecture
     */
    private Order validateOrderForAuthenticatedUser(Long orderId, UUID customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getUser() == null) {
            throw new RuntimeException(
                    "This order was placed by a guest and cannot be returned by authenticated users");
        }

        if (!customerId.equals(order.getUser().getId())) {
            throw new RuntimeException("Order does not belong to this customer");
        }

        // Check if order is delivered (using the aggregated status)
        if (!"DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("Order must be delivered to be eligible for return");
        }

        if (order.getOrderTransaction().getStatus() != OrderTransaction.TransactionStatus.COMPLETED) {
            throw new RuntimeException("Order transaction is not completed");
        }
        return order;
    }

    private void validateReturnEligibility(Order order) {
        // For multivendor, we need to check if any shop order is delivered
        boolean hasDeliveredItems = order.getShopOrders().stream()
                .anyMatch(so -> so.getStatus() == ShopOrder.ShopOrderStatus.DELIVERED);

        if (!hasDeliveredItems) {
            throw new RuntimeException("No delivered items found for return");
        }
    }

    private void validateCustomerReturnHistory(UUID customerId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<ReturnRequest> recentReturns = returnRequestRepository.findRecentByCustomerId(customerId, thirtyDaysAgo);

        if (recentReturns.size() > 5) {
            log.warn("Customer {} has {} returns in the last 30 days - flagging for review",
                    customerId, recentReturns.size());
        }
    }

    private void approveReturnRequest(ReturnRequest returnRequest, ReturnDecisionDTO decisionDTO) {
        log.info("Approving return request {} with decision notes: {}",
                returnRequest.getId(), decisionDTO.getDecisionNotes());

        returnRequest.approve(decisionDTO.getDecisionNotes());

        notificationService.notifyReturnApproved(returnRequest);
        log.info("Return request {} approved successfully", returnRequest.getId());

        // LOG ACTIVITY: Return Approved
        activityLogService.logReturnApproved(
                returnRequest.getOrderId(),
                "Admin", // TODO: Get actual admin name from security context
                returnRequest.getId());
    }

    private void denyReturnRequest(ReturnRequest returnRequest, ReturnDecisionDTO decisionDTO) {
        log.info("Denying return request {} with decision notes: {}",
                returnRequest.getId(), decisionDTO.getDecisionNotes());

        returnRequest.deny(decisionDTO.getDecisionNotes());

        notificationService.notifyReturnDenied(returnRequest);

        log.info("Return request {} denied successfully", returnRequest.getId());

        // LOG ACTIVITY: Return Denied
        activityLogService.logReturnDenied(
                returnRequest.getOrderId(),
                "Admin", // TODO: Get actual admin name from security context
                decisionDTO.getDecisionNotes(),
                returnRequest.getId());
    }

    /**
     * Validate media files before processing
     */
    private void validateMediaFiles(MultipartFile[] mediaFiles) {
        if (mediaFiles == null || mediaFiles.length == 0) {
            return;
        }

        int imageCount = 0;
        int videoCount = 0;

        for (MultipartFile file : mediaFiles) {
            if (file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType();
            if (contentType == null) {
                throw new IllegalArgumentException("Unable to determine file type");
            }

            if (contentType.startsWith("image/")) {
                imageCount++;
                if (imageCount > MAX_IMAGES) {
                    throw new IllegalArgumentException("Maximum " + MAX_IMAGES + " images allowed");
                }

                // Validate image size (max 10MB)
                if (file.getSize() > 10 * 1024 * 1024) {
                    throw new IllegalArgumentException("Image file size must be less than 10MB");
                }
            } else if (contentType.startsWith("video/")) {
                videoCount++;
                if (videoCount > MAX_VIDEOS) {
                    throw new IllegalArgumentException("Maximum " + MAX_VIDEOS + " video allowed");
                }

                // Validate video size (max 50MB)
                if (file.getSize() > 50 * 1024 * 1024) {
                    throw new IllegalArgumentException("Video file size must be less than 50MB");
                }
            } else {
                throw new IllegalArgumentException("Only image and video files are allowed");
            }
        }
    }

    /**
     * Process media attachments for return request
     */
    private void processMediaAttachments(Long returnRequestId, MultipartFile[] mediaFiles) throws IOException {
        for (MultipartFile file : mediaFiles) {
            if (file.isEmpty()) {
                continue;
            }

            // Upload to cloud storage (simplified for now)
            String mediaUrl = "https://example.com/media/" + file.getOriginalFilename();

            // Save media record (simplified - adjust based on your ReturnMedia entity)
            ReturnMedia media = new ReturnMedia();
            media.setReturnRequestId(returnRequestId);
            // Note: Adjust these method calls based on your actual ReturnMedia entity
            // structure
            // media.setUrl(mediaUrl);
            // media.setType(file.getContentType().startsWith("image/") ? "IMAGE" :
            // "VIDEO");
            // media.setName(file.getOriginalFilename());
            // media.setSize(file.getSize());
            media.setUploadedAt(LocalDateTime.now());

            returnMediaRepository.save(media);
        }
    }

    /**
     * Create return items for the return request
     */
    private void createReturnItems(ReturnRequest returnRequest, List<ReturnItemDTO> returnItemDTOs, Order order) {
        for (ReturnItemDTO itemDTO : returnItemDTOs) {
            // Find the order item
            OrderItem orderItem = order.getAllItems().stream() // Use getAllItems() for multivendor
                    .filter(oi -> oi.getOrderItemId().equals(itemDTO.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Order item not found: " + itemDTO.getOrderItemId()));

            ReturnItem returnItem = new ReturnItem();
            returnItem.setReturnRequest(returnRequest);
            returnItem.setOrderItem(orderItem);
            returnItem.setReturnQuantity(itemDTO.getReturnQuantity());
            // Note: Adjust these method calls based on your actual ReturnItemDTO structure
            // returnItem.setReason(itemDTO.getReason());
            // returnItem.setCondition(itemDTO.getCondition());
            returnItem.setCreatedAt(LocalDateTime.now());

            returnItemRepository.save(returnItem);
        }
    }

    /**
     * Validate return items against order
     */
    private void validateReturnItems(List<ReturnItemDTO> returnItems, Order order) {
        if (returnItems == null || returnItems.isEmpty()) {
            throw new IllegalArgumentException("At least one item must be selected for return");
        }

        for (ReturnItemDTO itemDTO : returnItems) {
            // Check if order item exists
            boolean itemExists = order.getAllItems().stream() // Use getAllItems() for multivendor
                    .anyMatch(oi -> oi.getOrderItemId().equals(itemDTO.getOrderItemId()));

            if (!itemExists) {
                throw new RuntimeException("Order item not found: " + itemDTO.getOrderItemId());
            }

            // Validate return quantity
            if (itemDTO.getReturnQuantity() <= 0) {
                throw new IllegalArgumentException("Return quantity must be greater than 0");
            }
        }
    }

    /**
     * Validate return items eligibility (quantity, return window, etc.)
     */
    private void validateReturnItemsEligibility(List<ReturnItemDTO> returnItems, Order order) {
        for (ReturnItemDTO itemDTO : returnItems) {
            OrderItem orderItem = order.getAllItems().stream() // Use getAllItems() for multivendor
                    .filter(oi -> oi.getOrderItemId().equals(itemDTO.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Order item not found: " + itemDTO.getOrderItemId()));

            // Check return quantity doesn't exceed ordered quantity
            if (itemDTO.getReturnQuantity() > orderItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Return quantity cannot exceed ordered quantity for item: " + itemDTO.getOrderItemId());
            }

            // Check if item is already fully returned
            Integer alreadyReturned = returnItemRepository
                    .getTotalApprovedReturnQuantityForOrderItem(orderItem.getOrderItemId());

            if (alreadyReturned + itemDTO.getReturnQuantity() > orderItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Total return quantity cannot exceed ordered quantity for item: " + itemDTO.getOrderItemId());
            }
        }
    }

    /**
     * Validate return request limits
     */
    private void validateReturnRequestLimit(List<ReturnItemDTO> returnItems) {
        if (returnItems.size() > 20) {
            throw new IllegalArgumentException("Maximum 20 items can be returned in a single request");
        }
    }

    /**
     * Convert ReturnRequest entity to DTO
     */
    private ReturnRequestDTO convertToDTO(ReturnRequest returnRequest) {
        // This would be implemented based on your DTO structure
        // For now, returning a placeholder
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setId(returnRequest.getId());
        dto.setOrderId(returnRequest.getOrderId());
        dto.setCustomerId(returnRequest.getCustomerId());
        dto.setReason(returnRequest.getReason());
        dto.setStatus(returnRequest.getStatus());
        dto.setSubmittedAt(returnRequest.getSubmittedAt());
        return dto;
    }
}