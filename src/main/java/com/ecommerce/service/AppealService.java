package com.ecommerce.service;

import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.repository.AppealMediaRepository;
import com.ecommerce.repository.ReturnAppealRepository;
import com.ecommerce.repository.ReturnRequestRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderTrackingTokenRepository;
import com.ecommerce.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppealService {

    private final ReturnAppealRepository returnAppealRepository;
    private final AppealMediaRepository appealMediaRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final CloudinaryService cloudinaryService;
    private final OrderTrackingTokenRepository orderTrackingTokenRepository;

    // Notification and audit services
    private final NotificationService notificationService;
    private final EmailService emailService;

    private static final int DEFAULT_RETURN_DAYS = 15;
    private static final int MAX_IMAGES = 5;
    private static final int MAX_VIDEOS = 1;

    /**
     * Submit an appeal for a denied return request with media files
     */
    public ReturnAppealDTO submitAppeal(SubmitAppealRequestDTO submitDTO, MultipartFile[] mediaFiles) {
        log.info("Processing appeal submission for return request {} by customer {}",
                submitDTO.getReturnRequestId(), submitDTO.getCustomerId());

        ReturnRequest returnRequest = validateReturnForAppeal(submitDTO.getReturnRequestId(),
                submitDTO.getCustomerId());

        if (returnAppealRepository.existsByReturnRequestId(submitDTO.getReturnRequestId())) {
            throw new com.ecommerce.Exception.ReturnException.AppealAlreadyExistsException(
                    "Appeal already exists for return request " + submitDTO.getReturnRequestId());
        }

        validateAppealEligibility(returnRequest);

        if (mediaFiles != null && mediaFiles.length > 0) {
            validateMediaFiles(mediaFiles);
        }

        // Create appeal
        ReturnAppeal appeal = new ReturnAppeal();
        appeal.setReturnRequestId(submitDTO.getReturnRequestId());
        appeal.setCustomerId(submitDTO.getCustomerId()); // This can be null for guest customers
        appeal.setLevel(1); 
        appeal.setReason(submitDTO.getReason());
        appeal.setDescription(submitDTO.getDescription());
        appeal.setStatus(ReturnAppeal.AppealStatus.PENDING);
        appeal.setSubmittedAt(LocalDateTime.now());

        log.debug("Creating appeal with customerId: {} for return request: {}", 
                submitDTO.getCustomerId(), submitDTO.getReturnRequestId());

        ReturnAppeal savedAppeal = returnAppealRepository.save(appeal);
        if (mediaFiles != null && mediaFiles.length > 0) {
            try {
                processAppealMediaAttachments(savedAppeal.getId(), mediaFiles);
            } catch (IOException e) {
                log.error("Failed to process media attachments for appeal {}: {}",
                        savedAppeal.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to upload media files", e);
            }
        }

        sendAppealConfirmationEmailToCustomer(savedAppeal, returnRequest);

        log.info("Appeal {} submitted successfully for return request {}",
                savedAppeal.getId(), submitDTO.getReturnRequestId());

        return convertToDTO(savedAppeal);
    }

    /**
     * Submit an appeal for a denied return request using tracking token (for guest users)
     */
    public ReturnAppealDTO submitTokenizedAppeal(TokenizedAppealRequestDTO submitDTO, MultipartFile[] mediaFiles) {
        log.info("Processing tokenized appeal submission for return request {} with token",
                submitDTO.getReturnRequestId());

        // Validate tracking token first
        String customerEmail = validateTrackingToken(submitDTO.getTrackingToken());
        
        ReturnRequest returnRequest = validateReturnForTokenizedAppeal(submitDTO.getReturnRequestId(), customerEmail);

        if (returnAppealRepository.existsByReturnRequestId(submitDTO.getReturnRequestId())) {
            throw new com.ecommerce.Exception.ReturnException.AppealAlreadyExistsException(
                    "Appeal already exists for return request " + submitDTO.getReturnRequestId());
        }

        validateAppealEligibility(returnRequest);

        if (mediaFiles != null && mediaFiles.length > 0) {
            validateMediaFiles(mediaFiles);
        }

        // Create appeal (customerId will be null for guest users)
        ReturnAppeal appeal = new ReturnAppeal();
        appeal.setReturnRequestId(submitDTO.getReturnRequestId());
        appeal.setCustomerId(null); // Guest users don't have customerId
        appeal.setLevel(1); 
        appeal.setReason(submitDTO.getReason());
        appeal.setDescription(submitDTO.getDescription());
        appeal.setStatus(ReturnAppeal.AppealStatus.PENDING);
        appeal.setSubmittedAt(LocalDateTime.now());

        log.debug("Creating tokenized appeal for return request: {} by email: {}", 
                submitDTO.getReturnRequestId(), customerEmail);

        ReturnAppeal savedAppeal = returnAppealRepository.save(appeal);
        
        if (mediaFiles != null && mediaFiles.length > 0) {
            try {
                processAppealMediaAttachments(savedAppeal.getId(), mediaFiles);
            } catch (IOException e) {
                log.error("Failed to process media attachments for tokenized appeal {}: {}",
                        savedAppeal.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to upload media files", e);
            }
        }

        sendAppealConfirmationEmailToCustomer(savedAppeal, returnRequest);

        log.info("Tokenized appeal {} submitted successfully for return request {}",
                savedAppeal.getId(), submitDTO.getReturnRequestId());

        return convertToDTO(savedAppeal);
    }

    public ReturnAppealDTO reviewAppeal(AppealDecisionDTO decisionDTO) {
        log.info("Processing appeal decision for appeal {}: {}",
                decisionDTO.getAppealId(), decisionDTO.getDecision());

        ReturnAppeal appeal = returnAppealRepository.findByIdWithReturnRequest(decisionDTO.getAppealId())
                .orElseThrow(() -> new com.ecommerce.Exception.ReturnException.ReturnNotFoundException(
                        "Appeal not found: " + decisionDTO.getAppealId()));

        if (appeal.getStatus() != ReturnAppeal.AppealStatus.PENDING) {
            throw new com.ecommerce.Exception.ReturnException.InvalidReturnStatusException(
                    "Appeal is not in pending status");
        }

        if ("APPROVED".equals(decisionDTO.getDecision())) {
            approveAppeal(appeal, decisionDTO);
        } else if ("DENIED".equals(decisionDTO.getDecision())) {
            denyAppeal(appeal, decisionDTO);
        } else {
            throw new IllegalArgumentException("Invalid decision: " + decisionDTO.getDecision());
        }

        ReturnAppeal updatedAppeal = returnAppealRepository.save(appeal);

        // Send email notification to customer
        sendAppealDecisionEmailToCustomer(updatedAppeal, decisionDTO);

        log.info("Appeal {} decision completed: {}",
                updatedAppeal.getId(), decisionDTO.getDecision());

        return convertToDTO(updatedAppeal);
    }

    /**
     * Escalate appeal to higher level review
     */
    public ReturnAppealDTO escalateAppeal(Long appealId, String escalationReason, String escalatedBy) {
        log.info("Escalating appeal {} for higher level review", appealId);

        ReturnAppeal appeal = returnAppealRepository.findById(appealId)
                .orElseThrow(() -> new com.ecommerce.Exception.ReturnException.ReturnNotFoundException(
                        "Appeal not found: " + appealId));

        if (appeal.getStatus() != ReturnAppeal.AppealStatus.PENDING) {
            throw new com.ecommerce.Exception.ReturnException.InvalidReturnStatusException(
                    "Only pending appeals can be escalated");
        }
        log.info("Appeal {} escalated successfully", appealId);

        return convertToDTO(appeal);
    }

    /**
     * Get appeals by customer with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReturnAppealDTO> getAppealsByCustomer(UUID customerId, Pageable pageable) {
        Page<ReturnAppeal> appeals = returnAppealRepository.findByCustomerId(customerId, pageable);
        return appeals.map(this::convertToDTO);
    }

    /**
     * Get appeals by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReturnAppealDTO> getAppealsByStatus(ReturnAppeal.AppealStatus status, Pageable pageable) {
        Page<ReturnAppeal> appeals = returnAppealRepository.findByStatus(status, pageable);
        return appeals.map(this::convertToDTO);
    }

    /**
     * Get appeal by ID with all related data
     */
    @Transactional(readOnly = true)
    public ReturnAppealDTO getAppealById(Long id) {
        ReturnAppeal appeal = returnAppealRepository.findByIdWithAllData(id)
                .orElseThrow(() -> new com.ecommerce.Exception.ReturnException.ReturnNotFoundException(
                        "Appeal not found: " + id));
        return convertToDTO(appeal);
    }

    /**
     * Get appeal by return request ID
     */
    @Transactional(readOnly = true)
    public ReturnAppealDTO getAppealByReturnRequestId(Long returnRequestId) {
        ReturnAppeal appeal = returnAppealRepository.findByReturnRequestId(returnRequestId)
                .orElseThrow(() -> new com.ecommerce.Exception.ReturnException.ReturnNotFoundException(
                        "Appeal not found for return request: " + returnRequestId));
        return convertToDTO(appeal);
    }

    /**
     * Get appeals that need decision (pending for more than specified days)
     */
    @Transactional(readOnly = true)
    public List<ReturnAppealDTO> getAppealsNeedingDecision(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<ReturnAppeal> appeals = returnAppealRepository.findAppealsNeedingDecision(cutoffDate);
        return appeals.stream().map(this::convertToDTO).toList();
    }

    /**
     * Get appeal statistics for dashboard
     */
    @Transactional(readOnly = true)
    public AppealStatisticsDTO getAppealStatistics() {
        AppealStatisticsDTO stats = new AppealStatisticsDTO();

        stats.setPendingCount(returnAppealRepository.countByStatus(ReturnAppeal.AppealStatus.PENDING));
        stats.setApprovedCount(returnAppealRepository.countByStatus(ReturnAppeal.AppealStatus.APPROVED));
        stats.setDeniedCount(returnAppealRepository.countByStatus(ReturnAppeal.AppealStatus.DENIED));

        // Recent appeals (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<ReturnAppeal> recentAppeals = returnAppealRepository.findRecentAppeals(thirtyDaysAgo);
        stats.setRecentCount(recentAppeals.size());

        // Appeals needing urgent attention (pending for more than 7 days)
        List<ReturnAppeal> urgentAppeals = returnAppealRepository.findAppealsNeedingDecision(
                LocalDateTime.now().minusDays(7));
        stats.setUrgentCount(urgentAppeals.size());

        // Calculate approval rate
        long totalDecided = stats.getApprovedCount() + stats.getDeniedCount();
        if (totalDecided > 0) {
            stats.setApprovalRate((double) stats.getApprovedCount() / totalDecided * 100);
        } else {
            stats.setApprovalRate(0.0);
        }

        return stats;
    }

    // Private helper methods

    private ReturnRequest validateReturnForAppeal(Long returnRequestId, UUID customerId) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new com.ecommerce.Exception.ReturnException.ReturnNotFoundException(
                        "Return request not found: " + returnRequestId));

        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.DENIED) {
            throw new com.ecommerce.Exception.ReturnException.AppealNotAllowedException(
                    "Only denied return requests can be appealed");
        }

        // For guest customers, customerId might be null, so we skip customer validation
        // The return request itself contains the customer information we need

        return returnRequest;
    }

    /**
     * Validate tracking token and return associated email
     */
    private String validateTrackingToken(String trackingToken) {
        Optional<OrderTrackingToken> tokenOpt = orderTrackingTokenRepository
                .findValidToken(trackingToken, LocalDateTime.now());
        
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired tracking token");
        }
        
        return tokenOpt.get().getEmail();
    }

    /**
     * Validate return request for tokenized appeal (guest users)
     */
    private ReturnRequest validateReturnForTokenizedAppeal(Long returnRequestId, String customerEmail) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new com.ecommerce.Exception.ReturnException.ReturnNotFoundException(
                        "Return request not found: " + returnRequestId));

        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.DENIED) {
            throw new com.ecommerce.Exception.ReturnException.AppealNotAllowedException(
                    "Only denied return requests can be appealed");
        }

        // Validate that the order belongs to the email associated with the token
        Order order = orderRepository.findById(returnRequest.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + returnRequest.getOrderId()));
        
        if (order.getOrderCustomerInfo() == null || 
            !customerEmail.equalsIgnoreCase(order.getOrderCustomerInfo().getEmail())) {
            throw new com.ecommerce.Exception.ReturnException.AppealNotAllowedException(
                    "Token does not match the order's customer email");
        }

        // Ensure this is actually a guest order (no registered customer)
        if (order.getUser() != null) {
            throw new com.ecommerce.Exception.ReturnException.AppealNotAllowedException(
                    "This order belongs to a registered customer. Please log in to submit an appeal.");
        }

        return returnRequest;
    }

    /**
     * Validate appeal eligibility based on product delivery days (similar to return
     * validation)
     */
    private void validateAppealEligibility(ReturnRequest returnRequest) {
        Order order = orderRepository.findById(returnRequest.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + returnRequest.getOrderId()));

        LocalDateTime deliveryDate = order.getDeliveredAt();
        if (deliveryDate == null) {
            throw new RuntimeException("Order delivery date not found");
        }

        List<ReturnItem> returnItems = returnRequest.getReturnItems();

        for (ReturnItem returnItem : returnItems) {
            OrderItem orderItem = returnItem.getOrderItem();
            if (orderItem == null) {
                throw new RuntimeException("Order item not found for return item: " + returnItem.getId());
            }

            Product product = returnItem.getEffectiveProduct();

            Integer productReturnDays = product.getMaximumDaysForReturn();
            if (productReturnDays == null || productReturnDays <= 0) {
                productReturnDays = DEFAULT_RETURN_DAYS;
            }

            // Calculate return deadline for this specific item
            LocalDateTime returnDeadline = deliveryDate.plusDays(productReturnDays);

            if (LocalDateTime.now().isAfter(returnDeadline)) {
                String itemName = getItemDisplayName(returnItem);
                throw new IllegalArgumentException(
                        String.format(
                                "Appeal period has expired for %s. Appeals must be submitted within %d days of delivery (deadline was %s).",
                                itemName,
                                productReturnDays,
                                returnDeadline.toLocalDate()));
            }
        }
    }

    /**
     * Get display name for a return item
     */
    private String getItemDisplayName(ReturnItem returnItem) {
        if (returnItem.isVariantBased()) {
            return returnItem.getEffectiveProduct().getProductName() + " ("
                    + returnItem.getProductVariant().getVariantName() + ")";
        } else {
            return returnItem.getEffectiveProduct().getProductName();
        }
    }

    /**
     * Validate media files for appeal submission
     */
    private void validateMediaFiles(MultipartFile[] mediaFiles) {
        if (mediaFiles.length > (MAX_IMAGES + MAX_VIDEOS)) {
            throw new IllegalArgumentException(
                    String.format("Too many files. Maximum allowed: %d images and %d video",
                            MAX_IMAGES, MAX_VIDEOS));
        }

        int imageCount = 0;
        int videoCount = 0;

        for (MultipartFile file : mediaFiles) {
            if (file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType();
            if (contentType == null) {
                throw new IllegalArgumentException("File content type is required");
            }

            if (contentType.startsWith("image/")) {
                imageCount++;
                if (imageCount > MAX_IMAGES) {
                    throw new IllegalArgumentException("Maximum " + MAX_IMAGES + " images allowed");
                }

                // Validate image size (10MB limit)
                if (file.getSize() > 10 * 1024 * 1024) {
                    throw new IllegalArgumentException("Image file size must be less than 10MB");
                }
            } else if (contentType.startsWith("video/")) {
                videoCount++;
                if (videoCount > MAX_VIDEOS) {
                    throw new IllegalArgumentException("Maximum " + MAX_VIDEOS + " video allowed");
                }

                // Validate video size (50MB limit)
                if (file.getSize() > 50 * 1024 * 1024) {
                    throw new IllegalArgumentException("Video file size must be less than 50MB");
                }
            } else {
                throw new IllegalArgumentException("Only image and video files are allowed");
            }
        }
    }

    /**
     * Process and upload media attachments to Cloudinary for appeal
     */
    private void processAppealMediaAttachments(Long appealId, MultipartFile[] mediaFiles) throws IOException {
        for (MultipartFile file : mediaFiles) {
            if (file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType();
            AppealMedia.FileType fileType;
            String cloudinaryUrl;

            if (contentType != null && contentType.startsWith("image/")) {
                fileType = AppealMedia.FileType.IMAGE;
                Map<String, String> uploadResult = cloudinaryService.uploadImage(file);
                cloudinaryUrl = uploadResult.get("secure_url");
                if (cloudinaryUrl == null) {
                    cloudinaryUrl = uploadResult.get("url");
                }
            } else if (contentType != null && contentType.startsWith("video/")) {
                fileType = AppealMedia.FileType.VIDEO;
                Map<String, String> uploadResult = cloudinaryService.uploadVideo(file);
                cloudinaryUrl = uploadResult.get("secure_url");
                if (cloudinaryUrl == null) {
                    cloudinaryUrl = uploadResult.get("url");
                }
            } else {
                log.warn("Skipping unsupported file type: {}", contentType);
                continue;
            }

            if (cloudinaryUrl == null) {
                log.error("Failed to get URL from Cloudinary upload result for file: {}", file.getOriginalFilename());
                throw new IOException("Failed to upload file to Cloudinary: " + file.getOriginalFilename());
            }

            AppealMedia media = new AppealMedia();
            media.setAppealId(appealId);
            media.setFileUrl(cloudinaryUrl);
            media.setFileType(fileType);
            media.setUploadedAt(LocalDateTime.now());

            appealMediaRepository.save(media);

            log.info("Uploaded {} file to Cloudinary for appeal {}: {}",
                    fileType.name().toLowerCase(), appealId, cloudinaryUrl);
        }

        log.info("Processed {} media attachments for appeal {}",
                mediaFiles.length, appealId);
    }

    private void approveAppeal(ReturnAppeal appeal, AppealDecisionDTO decisionDTO) {
        appeal.approve(decisionDTO.getDecisionNotes());

        // When appeal is approved, the original return request should also be approved
        ReturnRequest returnRequest = appeal.getReturnRequest();
        if (returnRequest != null) {
            returnRequest.approve("Appeal approved: " + decisionDTO.getDecisionNotes());
            returnRequestRepository.save(returnRequest);

            // Process refund if specified
            if (decisionDTO.getRefundDetails() != null) {
                // Integrate with refund service
                log.info("Processing refund for approved appeal {}", appeal.getId());
            }
        }

        // Send notifications
        notificationService.notifyAppealApproved(appeal, returnRequest);

        log.info("Appeal {} approved", appeal.getId());
    }

    private void denyAppeal(ReturnAppeal appeal, AppealDecisionDTO decisionDTO) {
        appeal.deny(decisionDTO.getDecisionNotes());
        notificationService.notifyAppealDenied(appeal, appeal.getReturnRequest());

        log.info("Appeal {} denied - final decision", appeal.getId());
    }

    private ReturnAppealDTO convertToDTO(ReturnAppeal appeal) {
        ReturnAppealDTO dto = new ReturnAppealDTO();
        dto.setId(appeal.getId());
        dto.setReturnRequestId(appeal.getReturnRequestId());
        dto.setCustomerId(appeal.getCustomerId());
        dto.setLevel(appeal.getLevel());
        dto.setReason(appeal.getReason());
        dto.setDescription(appeal.getDescription());
        dto.setStatus(appeal.getStatus());
        dto.setSubmittedAt(appeal.getSubmittedAt());
        dto.setDecisionAt(appeal.getDecisionAt());
        dto.setDecisionNotes(appeal.getDecisionNotes());

        // Add media if loaded
        if (appeal.getAppealMedia() != null && !appeal.getAppealMedia().isEmpty()) {
            dto.setAppealMedia(appeal.getAppealMedia().stream()
                    .map(media -> convertAppealMediaToDTO(media))
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
     * Send appeal confirmation email to customer using EmailService
     */
    private void sendAppealConfirmationEmailToCustomer(ReturnAppeal appeal, ReturnRequest returnRequest) {
        try {
            // Get customer email from OrderCustomerInfo
            Order order = orderRepository.findById(returnRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + returnRequest.getOrderId()));
            
            String customerEmail = null;
            String customerName = null;
            
            // Try to get email from OrderCustomerInfo first (for both guest and registered users)
            if (order.getOrderCustomerInfo() != null) {
                customerEmail = order.getOrderCustomerInfo().getEmail();
                customerName = order.getOrderCustomerInfo().getFullName();
            }
            
            // If no email found, skip sending email
            if (customerEmail == null || customerEmail.trim().isEmpty()) {
                log.warn("No customer email found for appeal {}, skipping confirmation email", appeal.getId());
                return;
            }
            
            String trackingUrl = String.format("https://shopsphere-frontend.vercel.app/returns/info?returnId=%d", returnRequest.getId());
            
            String formattedSubmittedAt = appeal.getSubmittedAt()
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm"));
            
            emailService.sendAppealConfirmationEmail(
                customerEmail,
                customerName != null ? customerName : "Customer",
                appeal.getId(),
                returnRequest.getId(),
                order.getOrderCode(),
                appeal.getReason(),
                formattedSubmittedAt,
                trackingUrl
            );
            
            log.info("Appeal confirmation email sent successfully to {} for appeal {}", customerEmail, appeal.getId());
            
        } catch (Exception e) {
            log.error("Failed to send appeal confirmation email for appeal {}: {}", appeal.getId(), e.getMessage(), e);
            // Don't throw exception - email failure shouldn't break appeal submission
        }
    }

    /**
     * Get all appeals for admin with filtering and pagination
     */
    public Page<ReturnAppealDTO> getAllAppealsForAdmin(String status, Integer level, String customerName, 
                                                      String orderCode, LocalDate fromDate, LocalDate toDate, 
                                                      Pageable pageable) {
        log.info("Retrieving appeals for admin with filters - status: {}, level: {}, customerName: {}, orderCode: {}", 
                status, level, customerName, orderCode);

        // Build dynamic query using Specification
        Specification<ReturnAppeal> spec = Specification.where(null);

        if (status != null && !status.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("status"), ReturnAppeal.AppealStatus.valueOf(status.toUpperCase())));
        }

        if (level != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("level"), level));
        }

        if (customerName != null && !customerName.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                // Join with ReturnRequest and Order to search customer name
                var returnRequestJoin = root.join("returnRequest");
                var orderJoin = returnRequestJoin.join("order");
                var customerInfoJoin = orderJoin.join("orderCustomerInfo");
                
                // Search in both firstName and lastName
                String searchTerm = "%" + customerName.toLowerCase() + "%";
                return cb.or(
                    cb.like(cb.lower(customerInfoJoin.get("firstName")), searchTerm),
                    cb.like(cb.lower(customerInfoJoin.get("lastName")), searchTerm),
                    cb.like(cb.lower(cb.concat(cb.concat(customerInfoJoin.get("firstName"), " "), customerInfoJoin.get("lastName"))), searchTerm)
                );
            });
        }

        if (orderCode != null && !orderCode.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                var returnRequestJoin = root.join("returnRequest");
                var orderJoin = returnRequestJoin.join("order");
                return cb.like(cb.lower(orderJoin.get("orderCode")), 
                              "%" + orderCode.toLowerCase() + "%");
            });
        }

        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("submittedAt"), fromDate.atStartOfDay()));
        }

        if (toDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("submittedAt"), toDate.atTime(23, 59, 59)));
        }

        Page<ReturnAppeal> appeals = returnAppealRepository.findAll(spec, pageable);
        return appeals.map(this::convertToDTO);
    }

    /**
     * Get count of pending appeals for sidebar badge
     */
    public Long getPendingAppealsCount() {
        return returnAppealRepository.countByStatus(ReturnAppeal.AppealStatus.PENDING);
    }

    /**
     * Send appeal decision email to customer using EmailService
     */
    private void sendAppealDecisionEmailToCustomer(ReturnAppeal appeal, AppealDecisionDTO decisionDTO) {
        try {
            // Get customer email from OrderCustomerInfo through ReturnRequest
            ReturnRequest returnRequest = appeal.getReturnRequest();
            if (returnRequest == null) {
                log.warn("No return request found for appeal {}, skipping decision email", appeal.getId());
                return;
            }

            Order order = orderRepository.findById(returnRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + returnRequest.getOrderId()));
            
            String customerEmail = null;
            String customerName = null;
            
            // Get email from OrderCustomerInfo
            if (order.getOrderCustomerInfo() != null) {
                customerEmail = order.getOrderCustomerInfo().getEmail();
                customerName = order.getOrderCustomerInfo().getFullName();
            }
            
            // If no email found, skip sending email
            if (customerEmail == null || customerEmail.trim().isEmpty()) {
                log.warn("No customer email found for appeal {}, skipping decision email", appeal.getId());
                return;
            }
            
            // Format dates
            String formattedSubmittedAt = appeal.getSubmittedAt()
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm"));
            String formattedDecisionAt = appeal.getDecisionAt()
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm"));
            
            // Create tracking URL for return request
            String trackingUrl = String.format("https://shopsphere-frontend.vercel.app/returns/info?returnId=%d", returnRequest.getId());
            
            // Send appropriate email based on decision
            if ("APPROVED".equals(decisionDTO.getDecision())) {
                emailService.sendAppealApprovalEmail(
                    customerEmail,
                    customerName != null ? customerName : "Customer",
                    appeal.getId(),
                    returnRequest.getId(),
                    order.getOrderCode(),
                    appeal.getReason(),
                    decisionDTO.getDecisionNotes(),
                    formattedSubmittedAt,
                    formattedDecisionAt,
                    trackingUrl
                );
                log.info("Appeal approval email sent successfully to {} for appeal {}", customerEmail, appeal.getId());
            } else if ("DENIED".equals(decisionDTO.getDecision())) {
                emailService.sendAppealDenialEmail(
                    customerEmail,
                    customerName != null ? customerName : "Customer",
                    appeal.getId(),
                    returnRequest.getId(),
                    order.getOrderCode(),
                    appeal.getReason(),
                    decisionDTO.getDecisionNotes(),
                    formattedSubmittedAt,
                    formattedDecisionAt
                );
                log.info("Appeal denial email sent successfully to {} for appeal {}", customerEmail, appeal.getId());
            }
            
        } catch (Exception e) {
            log.error("Failed to send appeal decision email for appeal {}: {}", appeal.getId(), e.getMessage(), e);
            // Don't throw exception - email failure shouldn't break appeal processing
        }
    }
}
