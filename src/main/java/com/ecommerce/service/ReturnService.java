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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private static final int DEFAULT_RETURN_DAYS = 15;
    private static final int MAX_IMAGES = 5;
    private static final int MAX_VIDEOS = 1;
    private static final int MAX_VIDEO_DURATION_SECONDS = 30;

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

        if (returnRequestRepository.existsByOrderIdAndCustomerId(submitDTO.getOrderId(),
                submitDTO.getCustomerId())) {
            throw new RuntimeException("Return request already exists for order " + submitDTO.getOrderId());
        }

        validateReturnEligibility(order);
        validateCustomerReturnHistory(submitDTO.getCustomerId());
        validateReturnItems(submitDTO.getReturnItems(), order);
        validateReturnItemsEligibility(submitDTO.getReturnItems(), order);
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

        return convertToDTO(savedRequest);
    }

    /**
     * Submit a return request using tracking token (for guest users with email verification)
     */
    public ReturnRequestDTO submitTokenizedReturnRequest(TokenizedReturnRequestDTO submitDTO, MultipartFile[] mediaFiles) {
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
                .orElseThrow(() -> new RuntimeException("Order not found with order number: " + submitDTO.getOrderNumber()));

        // Verify the order belongs to the email associated with the token
        if (order.getOrderCustomerInfo() == null || 
            !email.equalsIgnoreCase(order.getOrderCustomerInfo().getEmail())) {
            throw new RuntimeException("Order does not belong to the email associated with this tracking token");
        }

        // Ensure this is actually a guest order (no associated user)
        if (order.getUser() != null) {
            throw new RuntimeException("This order belongs to a registered user and cannot be returned using tracking token");
        }

        if (returnRequestRepository.existsByOrderIdAndCustomerId(order.getOrderId(), null)) {
            throw new RuntimeException("Return request already exists for order " + submitDTO.getOrderNumber());
        }

        validateReturnEligibility(order);
        validateReturnItems(submitDTO.getReturnItems(), order);
        validateReturnItemsEligibility(submitDTO.getReturnItems(), order);
        if (mediaFiles != null && mediaFiles.length > 0) {
            validateMediaFiles(mediaFiles);
        }

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrderId(order.getOrderId());
        returnRequest.setCustomerId(null);
        returnRequest.setReason(submitDTO.getReason());
        returnRequest.setStatus(ReturnRequest.ReturnStatus.PENDING);
        returnRequest.setSubmittedAt(LocalDateTime.now());

        log.info("Creating tokenized return request with customerId=null for order {}", order.getOrderId());

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
     * Process warehouse assignment for approved return
     */
    public void processWarehouseAssignment(WarehouseAssignmentDTO assignmentDTO) {
        log.info("Processing warehouse assignment for return request {}",
                assignmentDTO.getReturnRequestId());

        ReturnRequest returnRequest = returnRequestRepository.findById(assignmentDTO.getReturnRequestId())
                .orElseThrow(
                        () -> new RuntimeException("Return request not found: " + assignmentDTO.getReturnRequestId()));

        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.APPROVED) {
            throw new RuntimeException("Return request must be approved for warehouse assignment");
        }

        // Note: Uncomment when WarehouseRepository is available
        // Warehouse warehouse =
        // warehouseRepository.findById(assignmentDTO.getWarehouseId())
        // .orElseThrow(() -> new RuntimeException("Warehouse not found: " +
        // assignmentDTO.getWarehouseId()));

        log.info("Warehouse assignment for return request {} to warehouse {}",
                returnRequest.getId(), assignmentDTO.getWarehouseId());

        if (assignmentDTO.isShouldRestock()) {
            processRestocking(returnRequest, assignmentDTO);
        } else {
            // Mark as non-resellable, don't add back to stock
            log.info("Return request {} marked as non-resellable, not restocking",
                    returnRequest.getId());
        }
        // Notify customer of progress
        notificationService.notifyReturnReceived(returnRequest);
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

    /**
     * Get return requests by customer with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReturnRequestDTO> getReturnRequestsByCustomer(UUID customerId, Pageable pageable) {
        Page<ReturnRequest> requests = returnRequestRepository.findByCustomerIdWithDetails(customerId, pageable);
        return requests.map(this::convertToDTO);
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
     * Get return requests by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReturnRequestDTO> getReturnRequestsByStatus(ReturnRequest.ReturnStatus status, Pageable pageable) {
        Page<ReturnRequest> requests = returnRequestRepository.findByStatusWithDetails(status, pageable);
        return requests.map(this::convertToDTO);
    }

    /**
     * Get return request by ID with all related data
     */
    @Transactional(readOnly = true)
    public ReturnRequestDTO getReturnRequestById(Long id) {
        ReturnRequest request = returnRequestRepository.findByIdWithBasicData(id)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + id));

        User requestOwner = returnRequestRepository.findCustomerByReturnRequestId(id).orElse(null);
        if (requestOwner != null) {
            request.setCustomer(requestOwner);
            request.setCustomerId(requestOwner.getId());
        }
        log.info("The basic info loaded successfully" + request);
        // Load return media separately
        ReturnRequest requestWithMedia = returnRequestRepository.findByIdWithMedia(id).orElse(null);
        if (requestWithMedia != null && requestWithMedia.getReturnMedia() != null) {
            request.setReturnMedia(requestWithMedia.getReturnMedia());
        }
        log.info("The media loaded successfully" + requestWithMedia);
        // Load return items separately
        ReturnRequest requestWithItems = returnRequestRepository.findByIdWithItems(id).orElse(null);
        if (requestWithItems != null && requestWithItems.getReturnItems() != null) {
            request.setReturnItems(requestWithItems.getReturnItems());
        }
        log.info("The items loaded successfully" + requestWithItems);
        // Load appeal data separately if it exists
        ReturnRequest requestWithAppeal = returnRequestRepository.findByIdWithAppealData(id).orElse(null);
        if (requestWithAppeal != null && requestWithAppeal.getReturnAppeal() != null) {
            request.setReturnAppeal(requestWithAppeal.getReturnAppeal());
        }
        log.info("The appeal loaded successfully" + requestWithAppeal);

        return convertToDTO(request);
    }


    /**
     * Get all return requests with pagination (Admin use only)
     */
    @Transactional(readOnly = true)
    public Page<ReturnRequestDTO> getAllReturnRequests(Pageable pageable) {
        Page<ReturnRequest> requests = returnRequestRepository.findAllWithDetails(pageable);
        return requests.map(this::convertToDTO);
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

                        // Search in customer info (with safe access)
                        try {
                            if (rr.getCustomer() != null) {
                                matches |= (rr.getCustomer().getFirstName() != null &&
                                        rr.getCustomer().getFirstName().toLowerCase().contains(searchLower));
                                matches |= (rr.getCustomer().getLastName() != null &&
                                        rr.getCustomer().getLastName().toLowerCase().contains(searchLower));
                                matches |= (rr.getCustomer().getUserEmail() != null &&
                                        rr.getCustomer().getUserEmail().toLowerCase().contains(searchLower));
                            }
                        } catch (Exception e) {
                            // Ignore lazy loading exceptions for search
                        }

                        // Search in guest customer info (with safe access)
                        try {
                            if (rr.getOrder() != null && rr.getOrder().getOrderCustomerInfo() != null) {
                                var guestInfo = rr.getOrder().getOrderCustomerInfo();
                                matches |= (guestInfo.getEmail() != null &&
                                        guestInfo.getEmail().toLowerCase().contains(searchLower));
                                matches |= (guestInfo.getFirstName() != null &&
                                        guestInfo.getFirstName().toLowerCase().contains(searchLower));
                                matches |= (guestInfo.getLastName() != null &&
                                        guestInfo.getLastName().toLowerCase().contains(searchLower));
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
                .sorted((rr1, rr2) -> {
                    // Apply sorting based on pageable sort
                    if (pageable.getSort().isSorted()) {
                        // Default to submittedAt DESC if no specific sort
                        return rr2.getSubmittedAt().compareTo(rr1.getSubmittedAt());
                    }
                    return rr2.getSubmittedAt().compareTo(rr1.getSubmittedAt());
                })
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

        if (order.getOrderStatus() != Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("Order must be delivered to be eligible for return");
        }
        if (order.getOrderStatus() == Order.OrderStatus.RETURNED) {
            throw new RuntimeException("Order is already returned");
        }

        if (order.getOrderTransaction().getStatus() != OrderTransaction.TransactionStatus.COMPLETED) {
            throw new RuntimeException("Order transaction is not completed");
        }
        return order;
    }


    private void validateReturnEligibility(Order order) {
        LocalDateTime deliveryDate = order.getDeliveredAt();
        if (deliveryDate == null) {
            throw new RuntimeException("Order not delivered yet for return");
        }
        // Note: Individual item return period validation is now handled in
        // validateReturnItemsEligibility()
    }

    private int getReturnDaysForOrder(Order order) {
        // Check if any product in the order has a specific return window
        // For now, use default - can be enhanced to check product-specific rules
        return DEFAULT_RETURN_DAYS;
    }

    private void validateCustomerReturnHistory(UUID customerId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<ReturnRequest> recentReturns = returnRequestRepository.findRecentByCustomerId(customerId, thirtyDaysAgo);

        if (recentReturns.size() > 5) { // Configurable threshold
            log.warn("Customer {} has {} returns in the last 30 days - flagging for review",
                    customerId, recentReturns.size());
        }
    }

    private void approveReturnRequest(ReturnRequest returnRequest, ReturnDecisionDTO decisionDTO) {
        log.info("Approving return request {} with decision notes: {}",
                returnRequest.getId(), decisionDTO.getDecisionNotes());

        returnRequest.approve(decisionDTO.getDecisionNotes());

        if (decisionDTO.getRefundDetails() != null) {
            refundService.processRefund(returnRequest, decisionDTO.getRefundDetails());
        }

        // Send comprehensive approval notification with HTML email
        notificationService.notifyReturnApproved(returnRequest);

        log.info("Return request {} approved successfully", returnRequest.getId());
    }

    private void denyReturnRequest(ReturnRequest returnRequest, ReturnDecisionDTO decisionDTO) {
        log.info("Denying return request {} with decision notes: {}",
                returnRequest.getId(), decisionDTO.getDecisionNotes());

        returnRequest.deny(decisionDTO.getDecisionNotes());

        // Send comprehensive denial notification with HTML email (customer can appeal)
        notificationService.notifyReturnDenied(returnRequest);

        log.info("Return request {} denied successfully", returnRequest.getId());
    }

    private void processRestocking(ReturnRequest returnRequest, WarehouseAssignmentDTO assignmentDTO) {
        // Find original stock batches for this order
        // This would require tracking which batches were used for the original order
        // For now, create new batch or add to existing batch

        // Implementation would depend on your stock batch tracking system
        log.info("Processing restocking for return request {} to warehouse {}",
                returnRequest.getId(), assignmentDTO.getWarehouseId());

        // Update batch quantities and status as needed
        // This is a complex operation that would need to integrate with your existing
        // stock system
    }

    private void processQCPassed(ReturnRequest returnRequest, QualityControlDTO qcDTO) {
        // Product passed QC, can be restocked
        log.info("QC passed for return request {}, proceeding with restocking", returnRequest.getId());

        // Trigger restocking process
        // Update batch status to ACTIVE if needed
    }

    private void processQCFailed(ReturnRequest returnRequest, QualityControlDTO qcDTO) {
        // Product failed QC, mark as non-resellable
        log.info("QC failed for return request {}, marking as non-resellable", returnRequest.getId());

        // Update batch status based on failure reason
        if (qcDTO.getNewBatchStatus() != null) {
            // Update batch status (EXPIRED, RECALLED, DAMAGED)
        }
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
                throw new IllegalArgumentException("File content type cannot be determined");
            }

            if (contentType.startsWith("image/")) {
                imageCount++;
                if (imageCount > MAX_IMAGES) {
                    throw new IllegalArgumentException("Maximum " + MAX_IMAGES + " images allowed");
                }

                // Validate image file size (max 10MB)
                if (file.getSize() > 10 * 1024 * 1024) {
                    throw new IllegalArgumentException("Image file size cannot exceed 10MB");
                }

            } else if (contentType.startsWith("video/")) {
                videoCount++;
                if (videoCount > MAX_VIDEOS) {
                    throw new IllegalArgumentException("Maximum " + MAX_VIDEOS + " video allowed");
                }

                // Validate video file size (max 50MB)
                if (file.getSize() > 50 * 1024 * 1024) {
                    throw new IllegalArgumentException("Video file size cannot exceed 50MB");
                }

                // Note: Video duration validation would require additional processing
                // For now, we rely on client-side validation for 30-second limit
                log.info("Video file uploaded: {} ({})", file.getOriginalFilename(), contentType);

            } else {
                throw new IllegalArgumentException("Only image and video files are allowed");
            }
        }

        log.info("Media file validation passed: {} images, {} videos", imageCount, videoCount);
    }

    /**
     * Process media attachments by uploading to Cloudinary and saving metadata
     */
    private void processMediaAttachments(Long returnRequestId, MultipartFile[] mediaFiles) throws IOException {
        List<ProcessedMediaFileDTO> processedFiles = new ArrayList<>();

        for (MultipartFile file : mediaFiles) {
            if (file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType();
            Map<String, String> uploadResult;

            try {
                if (contentType != null && contentType.startsWith("image/")) {
                    uploadResult = cloudinaryService.uploadImage(file);
                } else if (contentType != null && contentType.startsWith("video/")) {
                    uploadResult = cloudinaryService.uploadVideo(file);
                } else {
                    log.warn("Skipping unsupported file type: {}", contentType);
                    continue;
                }

                // Create and save return media record
                ReturnMedia media = new ReturnMedia();
                media.setReturnRequestId(returnRequestId);
                media.setFileUrl(uploadResult.get("secure_url"));
                media.setPublicId(uploadResult.get("public_id"));
                media.setFileType(
                        contentType.startsWith("image/") ? ReturnMedia.FileType.IMAGE : ReturnMedia.FileType.VIDEO);
                media.setMimeType(contentType);
                media.setFileSize(file.getSize());
                media.setUploadedAt(LocalDateTime.now());

                // Add dimensions for images
                if (uploadResult.containsKey("width")) {
                    media.setWidth(Integer.parseInt(uploadResult.get("width")));
                }
                if (uploadResult.containsKey("height")) {
                    media.setHeight(Integer.parseInt(uploadResult.get("height")));
                }

                returnMediaRepository.save(media);

                log.info("Successfully uploaded media file for return request {}: {} -> {}",
                        returnRequestId, file.getOriginalFilename(), uploadResult.get("secure_url"));

            } catch (IOException e) {
                log.error("Failed to upload media file {} for return request {}: {}",
                        file.getOriginalFilename(), returnRequestId, e.getMessage(), e);
                throw e;
            }
        }

        log.info("Processed {} media attachments for return request {}",
                mediaFiles.length, returnRequestId);
    }

    private ReturnRequestDTO convertToDTO(ReturnRequest returnRequest) {
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setId(returnRequest.getId());
        dto.setOrderId(returnRequest.getOrderId());
        if (returnRequest.getCustomerId() != null) {
            dto.setCustomerId(returnRequest.getCustomerId());
        }
        dto.setReason(returnRequest.getReason());
        dto.setStatus(returnRequest.getStatus());
        dto.setSubmittedAt(returnRequest.getSubmittedAt());
        dto.setDecisionAt(returnRequest.getDecisionAt());
        dto.setDecisionNotes(returnRequest.getDecisionNotes());
        dto.setCreatedAt(returnRequest.getCreatedAt());
        dto.setUpdatedAt(returnRequest.getUpdatedAt());
        dto.setCanBeAppealed(returnRequest.canBeAppealed());

        // Convert return media to DTOs with safe access
        try {
            if (returnRequest.getReturnMedia() != null && !returnRequest.getReturnMedia().isEmpty()) {
                dto.setReturnMedia(returnRequest.getReturnMedia().stream()
                        .map(this::convertReturnMediaToDTO)
                        .toList());
            }
        } catch (Exception e) {
            log.warn("Could not load return media for return request {}: {}", returnRequest.getId(), e.getMessage());
        }

        // Convert return items to DTOs with safe access
        try {
            if (returnRequest.getReturnItems() != null && !returnRequest.getReturnItems().isEmpty()) {
                dto.setReturnItems(returnRequest.getReturnItems().stream()
                        .map(this::convertReturnItemToDTO)
                        .toList());
            }
        } catch (Exception e) {
            log.warn("Could not load return items for return request {}: {}", returnRequest.getId(), e.getMessage());
        }

        // Convert return appeal to DTO with safe access
        try {
            if (returnRequest.getReturnAppeal() != null) {
                dto.setReturnAppeal(convertReturnAppealToDTO(returnRequest.getReturnAppeal()));
            }
        } catch (Exception e) {
            log.warn("Could not load return appeal for return request {}: {}", returnRequest.getId(), e.getMessage());
        }

        // Add customer info and order info with safe access
        try {
            if (returnRequest.getOrder() != null) {
                dto.setOrderNumber(returnRequest.getOrder().getOrderCode());

                // For registered customers
                if (returnRequest.getCustomerId() != null && returnRequest.getCustomer() != null) {
                    dto.setCustomerName(returnRequest.getCustomer().getFirstName() + " "
                            + returnRequest.getCustomer().getLastName());
                    dto.setCustomerEmail(returnRequest.getCustomer().getUserEmail());
                }
                // For guest customers - get info from OrderCustomerInfo
                else if (returnRequest.getCustomerId() == null
                        && returnRequest.getOrder().getOrderCustomerInfo() != null) {
                    OrderCustomerInfo customerInfo = returnRequest.getOrder().getOrderCustomerInfo();
                    dto.setCustomerName(customerInfo.getFullName());
                    dto.setCustomerEmail(customerInfo.getEmail());
                }
            }
        } catch (Exception e) {
            log.warn("Could not load order/customer information for return request {}: {}", returnRequest.getId(),
                    e.getMessage());
            // Set basic info if available
            dto.setOrderNumber("Order #" + returnRequest.getOrderId());
        }

        return dto;
    }

    /**
     * Convert ReturnMedia entity to DTO
     */
    private ReturnMediaDTO convertReturnMediaToDTO(ReturnMedia media) {
        ReturnMediaDTO dto = new ReturnMediaDTO();
        dto.setId(media.getId());
        dto.setReturnRequestId(media.getReturnRequestId());
        dto.setFileUrl(media.getFileUrl());
        dto.setPublicId(media.getPublicId());
        dto.setFileType(media.getFileType());
        dto.setMimeType(media.getMimeType());
        dto.setFileSize(media.getFileSize());
        dto.setWidth(media.getWidth());
        dto.setHeight(media.getHeight());
        dto.setUploadedAt(media.getUploadedAt());
        dto.setCreatedAt(media.getCreatedAt());
        dto.setUpdatedAt(media.getUpdatedAt());
        return dto;
    }

    /**
     * Convert ReturnItem entity to DTO
     */
    private ReturnItemDTO convertReturnItemToDTO(ReturnItem item) {
        ReturnItemDTO dto = new ReturnItemDTO();
        dto.setOrderItemId(item.getOrderItem().getOrderItemId());
        dto.setReturnQuantity(item.getReturnQuantity());
        dto.setItemReason(item.getItemReason());
        dto.setProductId(item.getEffectiveProduct().getProductId());
        dto.setVariantId(item.getEffectiveVariantId());
        dto.setProductName(item.getEffectiveProduct().getProductName());
        dto.setVariantName(item.isVariantBased() ? item.getProductVariant().getVariantName() : null);
        return dto;
    }

    /**
     * Convert ReturnAppeal entity to DTO
     */
    private ReturnAppealDTO convertReturnAppealToDTO(ReturnAppeal appeal) {
        ReturnAppealDTO dto = new ReturnAppealDTO();
        dto.setId(appeal.getId());
        dto.setReturnRequestId(appeal.getReturnRequestId());
        dto.setLevel(appeal.getLevel());
        dto.setDescription(appeal.getDescription());
        dto.setStatus(appeal.getStatus());
        dto.setSubmittedAt(appeal.getSubmittedAt());
        dto.setDecisionAt(appeal.getDecisionAt());
        dto.setDecisionNotes(appeal.getDecisionNotes());
        dto.setCreatedAt(appeal.getCreatedAt());
        dto.setUpdatedAt(appeal.getUpdatedAt());

        // Convert appeal media if available
        if (appeal.getAppealMedia() != null && !appeal.getAppealMedia().isEmpty()) {
            dto.setAppealMedia(appeal.getAppealMedia().stream()
                    .map(this::convertAppealMediaToDTO)
                    .toList());
        }

        return dto;
    }

    /**
     * Convert AppealMedia entity to DTO
     */
    private AppealMediaDTO convertAppealMediaToDTO(AppealMedia media) {
        AppealMediaDTO dto = new AppealMediaDTO();
        dto.setId(media.getId());
        dto.setAppealId(media.getAppealId());
        dto.setFileUrl(media.getFileUrl());
        dto.setFileType(media.getFileType());
        dto.setUploadedAt(media.getUploadedAt());
        dto.setCreatedAt(media.getCreatedAt());
        dto.setUpdatedAt(media.getUpdatedAt());
        return dto;
    }

    /**
     * Validate that all return items are valid for the given order
     */
    private void validateReturnItems(List<ReturnItemDTO> returnItems, Order order) {
        if (returnItems == null || returnItems.isEmpty()) {
            throw new IllegalArgumentException("At least one item must be specified for return");
        }

        // Get all order items for this order
        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            throw new RuntimeException("No items found in the order");
        }

        for (ReturnItemDTO returnItemDTO : returnItems) {
            validateSingleReturnItem(returnItemDTO, orderItems);
        }

        // Check for duplicate return items
        Set<Long> orderItemIds = returnItems.stream()
                .map(ReturnItemDTO::getOrderItemId)
                .collect(Collectors.toSet());

        if (orderItemIds.size() != returnItems.size()) {
            throw new IllegalArgumentException("Duplicate items found in return request");
        }
    }

    /**
     * Validate a single return item against order items
     */
    private void validateSingleReturnItem(ReturnItemDTO returnItemDTO, List<OrderItem> orderItems) {
        // Find the corresponding order item
        OrderItem orderItem = orderItems.stream()
                .filter(oi -> oi.getOrderItemId().equals(returnItemDTO.getOrderItemId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order item with ID " + returnItemDTO.getOrderItemId() + " not found in this order"));

        // Validate return quantity
        if (returnItemDTO.getReturnQuantity() <= 0) {
            throw new IllegalArgumentException("Return quantity must be greater than 0");
        }

        if (returnItemDTO.getReturnQuantity() > orderItem.getQuantity()) {
            throw new IllegalArgumentException(
                    String.format("Cannot return %d items of %s. Only %d were ordered.",
                            returnItemDTO.getReturnQuantity(),
                            getItemDisplayName(orderItem),
                            orderItem.getQuantity()));
        }

        // Check if there are already existing returns for this order item
        Integer alreadyReturned = returnItemRepository.getTotalReturnQuantityForOrderItem(orderItem.getOrderItemId());
        int availableForReturn = orderItem.getQuantity() - alreadyReturned;

        if (returnItemDTO.getReturnQuantity() > availableForReturn) {
            throw new IllegalArgumentException(
                    String.format("Cannot return %d items of %s. Only %d available for return (already returned: %d).",
                            returnItemDTO.getReturnQuantity(),
                            getItemDisplayName(orderItem),
                            availableForReturn,
                            alreadyReturned));
        }

        // Populate additional info for validation response
        returnItemDTO.setProductId(orderItem.getEffectiveProduct().getProductId());
        returnItemDTO.setVariantId(orderItem.isVariantBased() ? orderItem.getProductVariant().getId() : null);
        returnItemDTO.setMaxQuantity(availableForReturn);
        returnItemDTO.setProductName(orderItem.getEffectiveProduct().getProductName());
        returnItemDTO
                .setVariantName(orderItem.isVariantBased() ? orderItem.getProductVariant().getVariantName() : null);
    }

    /**
     * Create return items for a saved return request
     */
    private void createReturnItems(ReturnRequest returnRequest, List<ReturnItemDTO> returnItemDTOs, Order order) {
        List<OrderItem> orderItems = order.getOrderItems();

        for (ReturnItemDTO returnItemDTO : returnItemDTOs) {
            // Find the corresponding order item
            OrderItem orderItem = orderItems.stream()
                    .filter(oi -> oi.getOrderItemId().equals(returnItemDTO.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Order item not found: " + returnItemDTO.getOrderItemId()));

            // Create return item
            ReturnItem returnItem = new ReturnItem();
            returnItem.setReturnRequest(returnRequest);
            returnItem.setOrderItem(orderItem);
            returnItem.setReturnQuantity(returnItemDTO.getReturnQuantity());
            returnItem.setItemReason(returnItemDTO.getItemReason());

            // Debug logging to understand the order item structure
            log.info("Processing return item for order item {}: isVariantBased={}, hasProduct={}, hasVariant={}",
                    orderItem.getOrderItemId(),
                    orderItem.isVariantBased(),
                    orderItem.getProduct() != null,
                    orderItem.getProductVariant() != null);

            // Set product or variant reference to exactly match order item structure
            if (orderItem.isVariantBased()) {
                // For variant-based items, set only the variant (product should be null to
                // match order item)
                returnItem.setProductVariant(orderItem.getProductVariant());
                returnItem.setProduct(null);
                log.info("Set return item as variant-based: variant={}, product=null",
                        orderItem.getProductVariant().getId());
            } else {
                // For non-variant items, set only the product (variant should be null)
                returnItem.setProduct(orderItem.getProduct());
                returnItem.setProductVariant(null);
                log.info("Set return item as product-based: product={}, variant=null",
                        orderItem.getProduct().getProductId());
            }

            returnItemRepository.save(returnItem);
        }

        log.info("Created {} return items for return request {}",
                returnItemDTOs.size(), returnRequest.getId());
    }

    /**
     * Validate return period eligibility for each return item
     */
    private void validateReturnItemsEligibility(List<ReturnItemDTO> returnItems, Order order) {
        LocalDateTime deliveryDate = order.getDeliveredAt();
        if (deliveryDate == null) {
            throw new RuntimeException("Order not delivered yet for return");
        }

        List<OrderItem> orderItems = order.getOrderItems();

        for (ReturnItemDTO returnItemDTO : returnItems) {
            // Find the corresponding order item
            OrderItem orderItem = orderItems.stream()
                    .filter(oi -> oi.getOrderItemId().equals(returnItemDTO.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Order item not found: " + returnItemDTO.getOrderItemId()));

            // Get the product (either direct product or variant's parent product)
            Product product = orderItem.getEffectiveProduct();

            // Get return days for this specific product
            Integer productReturnDays = product.getMaximumDaysForReturn();
            if (productReturnDays == null || productReturnDays <= 0) {
                productReturnDays = DEFAULT_RETURN_DAYS;
            }

            // Calculate return deadline for this specific item
            LocalDateTime returnDeadline = deliveryDate.plusDays(productReturnDays);

            if (LocalDateTime.now().isAfter(returnDeadline)) {
                String itemName = getItemDisplayName(orderItem);
                throw new IllegalArgumentException(
                        String.format(
                                "Return period has expired for %s. Returns must be submitted within %d days of delivery (deadline was %s).",
                                itemName,
                                productReturnDays,
                                returnDeadline.toLocalDate()));
            }
        }

    }

    /**
     * Get display name for an order item
     */
    private String getItemDisplayName(OrderItem orderItem) {
        if (orderItem.isVariantBased()) {
            return orderItem.getEffectiveProduct().getProductName() + " ("
                    + orderItem.getProductVariant().getVariantName() + ")";
        }
        return orderItem.getEffectiveProduct().getProductName();
    }

    /**
     * Count return requests by status
     */
    public long countReturnRequestsByStatus(ReturnRequest.ReturnStatus status) {
        return returnRequestRepository.countByStatus(status);
    }
}