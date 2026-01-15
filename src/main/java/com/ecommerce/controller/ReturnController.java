package com.ecommerce.controller;

import com.ecommerce.dto.*;
import com.ecommerce.entity.ReturnRequest;
import com.ecommerce.entity.User;
import com.ecommerce.service.ShopAuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Return Management", description = "APIs for managing product return requests")
public class ReturnController {

        private final ReturnService returnService;
        private final ShopAuthorizationService shopAuthorizationService;
        private final ObjectMapper objectMapper;

        @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyRole('CUSTOMER','DELIVERY_AGENT','ADMIN')")
        @Operation(summary = "Submit return request (Authenticated)", description = "Submit a return request for authenticated customers with optional media files", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Return request submitted successfully", content = @Content(schema = @Schema(implementation = ReturnRequestDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request data"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Customer role required"),
                        @ApiResponse(responseCode = "404", description = "Order not found"),
                        @ApiResponse(responseCode = "422", description = "Order not eligible for return")
        })
        public ResponseEntity<?> submitReturnRequest(
                        @Valid @RequestPart("returnRequest") SubmitReturnRequestDTO submitDTO,
                        @RequestPart(value = "mediaFiles", required = false) MultipartFile[] mediaFiles,
                        Authentication authentication) {

                try {
                        log.info("Processing return request submission for authenticated customer {} and shop order {}",
                                        submitDTO.getCustomerId(), submitDTO.getShopOrderId());

                        ReturnRequestDTO result = returnService.submitReturnRequest(submitDTO, mediaFiles);

                        log.info("Return request {} submitted successfully for customer {} and shop order {}",
                                        result.getId(), submitDTO.getCustomerId(), submitDTO.getShopOrderId());

                        return ResponseEntity.status(HttpStatus.CREATED).body(result);

                } catch (IllegalArgumentException e) {
                        log.warn("Invalid return request data: {}", e.getMessage());
                        return ResponseEntity.badRequest().body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
                } catch (RuntimeException e) {
                        log.error("Error processing return request for customer {} and shop order {}: {}",
                                        submitDTO.getCustomerId(), submitDTO.getShopOrderId(), e.getMessage(), e);

                        if (e.getMessage().contains("not found")) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(createErrorResponse("ORDER_NOT_FOUND", e.getMessage()));
                        } else if (e.getMessage().contains("already exists")) {
                                return ResponseEntity.status(HttpStatus.CONFLICT)
                                                .body(createErrorResponse("RETURN_EXISTS", e.getMessage()));
                        } else if (e.getMessage().contains("not eligible") || e.getMessage().contains("expired")) {
                                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                                .body(createErrorResponse("NOT_ELIGIBLE", e.getMessage()));
                        }

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("RETURN_ERROR",
                                                        "Failed to process return request" + e.getMessage()));
                }
        }

        /**
         * Get return requests for authenticated customer
         */
        @GetMapping("/my-returns")
        @PreAuthorize("hasAnyRole('CUSTOMER','DELIVERY_AGENT','ADMIN')")
        @Operation(summary = "Get customer return requests", description = "Get paginated list of return requests for authenticated customer", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Return requests retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Customer role required")
        })
        public ResponseEntity<?> getCustomerReturnRequests(
                        @Parameter(description = "Customer ID") @RequestParam UUID customerId,
                        @PageableDefault(size = 10) Pageable pageable,
                        Authentication authentication) {

                try {
                        log.info("Retrieving return requests for customer {}", customerId);

                        Page<ReturnRequestDTO> returnRequests = returnService.getReturnRequestsByCustomer(customerId,
                                        pageable);

                        log.info("Retrieved {} return requests for customer {}",
                                        returnRequests.getTotalElements(), customerId);

                        return ResponseEntity.ok(returnRequests);

                } catch (Exception e) {
                        log.error("Error retrieving return requests for customer {}: {}", customerId, e.getMessage(),
                                        e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("RETRIEVAL_ERROR",
                                                        "Failed to retrieve return requests"));
                }
        }

        /**
         * Get return requests by order ID for authenticated users
         */
        @GetMapping("/order/{orderId}")
        @PreAuthorize("hasAnyRole('CUSTOMER','DELIVERY_AGENT','ADMIN')")
        @Operation(summary = "Get return requests by order ID", description = "Get all return requests for a specific order (authenticated users)", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Return requests retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Customer role required"),
                        @ApiResponse(responseCode = "404", description = "Order not found")
        })
        public ResponseEntity<?> getReturnRequestsByOrderId(
                        @Parameter(description = "Order ID") @PathVariable Long orderId,
                        @Parameter(description = "Customer ID") @RequestParam UUID customerId,
                        Authentication authentication) {

                try {
                        log.info("Retrieving return requests for order {} and customer {}", orderId, customerId);

                        var returnRequests = returnService.getReturnRequestsByOrderId(orderId, customerId);

                        log.info("Retrieved {} return requests for order {}", returnRequests.size(), orderId);

                        return ResponseEntity.ok(returnRequests);

                } catch (RuntimeException e) {
                        log.error("Error retrieving return requests for order {}: {}", orderId, e.getMessage(), e);

                        if (e.getMessage().contains("not found")) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(createErrorResponse("ORDER_NOT_FOUND", e.getMessage()));
                        }

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("RETRIEVAL_ERROR",
                                                        "Failed to retrieve return requests"));
                }
        }

        /**
         * Get return requests by order number and tracking token (for guest users)
         */
        @GetMapping("/order/guest")
        @Operation(summary = "Get return requests by order number and token", description = "Get all return requests for a specific order using tracking token (guest users)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Return requests retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                        @ApiResponse(responseCode = "404", description = "Order not found")
        })
        public ResponseEntity<?> getReturnRequestsByOrderNumberAndToken(
                        @Parameter(description = "Order number") @RequestParam String orderNumber,
                        @Parameter(description = "Tracking token") @RequestParam String token) {

                try {
                        log.info("Retrieving return requests for guest order {}", orderNumber);

                        var returnRequests = returnService.getReturnRequestsByOrderNumberAndToken(orderNumber, token);

                        log.info("Retrieved {} return requests for guest order {}", returnRequests.size(), orderNumber);

                        return ResponseEntity.ok(returnRequests);

                } catch (IllegalArgumentException e) {
                        log.warn("Invalid request parameters: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
                } catch (RuntimeException e) {
                        log.error("Error retrieving return requests for order {}: {}", orderNumber, e.getMessage(), e);

                        if (e.getMessage().contains("not found")) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(createErrorResponse("ORDER_NOT_FOUND", e.getMessage()));
                        }

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("RETRIEVAL_ERROR",
                                                        "Failed to retrieve return requests"));
                }
        }

        /**
         * Get specific return request details
         */
        @GetMapping("/{returnRequestId}")
        // @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'EMPLOYEE')")
        @Operation(summary = "Get return request details", description = "Get detailed information about a specific return request", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Return request details retrieved successfully", content = @Content(schema = @Schema(implementation = ReturnRequestDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
                        @ApiResponse(responseCode = "404", description = "Return request not found")
        })
        public ResponseEntity<?> getReturnRequestDetails(
                        @Parameter(description = "Return request ID") @PathVariable Long returnRequestId,
                        Authentication authentication) {

                try {
                        log.info("Retrieving return request details for ID {}", returnRequestId);

                        ReturnRequestDTO returnRequest = returnService.getReturnRequestById(returnRequestId);

                        log.info("Return request {} details retrieved successfully", returnRequestId);

                        return ResponseEntity.ok(returnRequest);

                } catch (RuntimeException e) {
                        log.error("Error retrieving return request {}: {}", returnRequestId, e.getMessage(), e);

                        if (e.getMessage().contains("not found")) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(createErrorResponse("RETURN_NOT_FOUND", e.getMessage()));
                        }

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("RETRIEVAL_ERROR",
                                                        "Failed to retrieve return request"));
                }
        }

        // ==================== ADMIN/EMPLOYEE ENDPOINTS ====================

        /**
         * Get all return requests with filtering (Admin/Employee only)
         */
        @GetMapping("/admin/all")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
        @Operation(summary = "Get all return requests with filtering (Admin)", description = "Get paginated list of return requests with optional filtering by status, customer type, and search", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Return requests retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Employee role required")
        })
        public ResponseEntity<?> getAllReturnRequests(
                        @Parameter(description = "Filter by return status") @RequestParam(required = false) String status,
                        @Parameter(description = "Filter by customer type (REGISTERED/GUEST)") @RequestParam(required = false) String customerType,
                        @Parameter(description = "Search by order number, customer name, or email") @RequestParam(required = false) String search,
                        @Parameter(description = "Filter by date from (ISO format)") @RequestParam(required = false) String dateFrom,
                        @Parameter(description = "Filter by date to (ISO format)") @RequestParam(required = false) String dateTo,
                        @PageableDefault(size = 20) Pageable pageable,
                        Authentication authentication) {

                try {
                        log.info("Admin {} retrieving return requests with filters - status: {}, customerType: {}, search: {}",
                                        authentication.getName(), status, customerType, search);

                        // Validate status parameter if provided
                        ReturnRequest.ReturnStatus returnStatus = null;
                        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                                try {
                                        returnStatus = ReturnRequest.ReturnStatus.valueOf(status.toUpperCase());
                                } catch (IllegalArgumentException e) {
                                        return ResponseEntity.badRequest()
                                                        .body(createErrorResponse("INVALID_STATUS",
                                                                        "Invalid return status: " + status));
                                }
                        }

                        Page<ReturnRequestDTO> returnRequests = returnService.getAllReturnRequestsWithFilters(
                                        returnStatus, customerType, search, dateFrom, dateTo, pageable);

                        log.info("Retrieved {} return requests for admin review with applied filters",
                                        returnRequests.getTotalElements());

                        return ResponseEntity.ok(returnRequests);

                } catch (Exception e) {
                        log.error("Error retrieving return requests for admin {}: {}",
                                        authentication.getName(), e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("RETRIEVAL_ERROR",
                                                        "Failed to retrieve return requests"));
                }
        }

        /**
         * Get return requests by status (Admin/Employee only)
         */
        @GetMapping("/admin/status/{status}")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
        @Operation(summary = "Get return requests by status (Admin)", description = "Get paginated list of return requests filtered by status", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Return requests retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid status parameter"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Employee role required")
        })
        public ResponseEntity<?> getReturnRequestsByStatus(
                        @Parameter(description = "Return request status") @PathVariable String status,
                        @PageableDefault(size = 20) Pageable pageable,
                        Authentication authentication) {

                try {
                        log.info("Admin {} retrieving return requests with status {}", authentication.getName(),
                                        status);

                        // Validate status parameter
                        ReturnRequest.ReturnStatus returnStatus;
                        try {
                                returnStatus = ReturnRequest.ReturnStatus.valueOf(status.toUpperCase());
                        } catch (IllegalArgumentException e) {
                                return ResponseEntity.badRequest()
                                                .body(createErrorResponse("INVALID_STATUS",
                                                                "Invalid return status: " + status));
                        }

                        Page<ReturnRequestDTO> returnRequests = returnService.getReturnRequestsByStatus(returnStatus,
                                        pageable);

                        log.info("Retrieved {} return requests with status {} for admin review",
                                        returnRequests.getTotalElements(), status);

                        return ResponseEntity.ok(returnRequests);

                } catch (Exception e) {
                        log.error("Error retrieving return requests by status {} for admin {}: {}",
                                        status, authentication.getName(), e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("RETRIEVAL_ERROR",
                                                        "Failed to retrieve return requests"));
                }
        }

        /**
         * Get guest return requests (Admin/Employee only)
         */
        @GetMapping("/admin/guest")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
        @Operation(summary = "Get guest return requests (Admin)", description = "Get paginated list of return requests from guest users", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Guest return requests retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Employee role required")
        })
        public ResponseEntity<?> getGuestReturnRequests(
                        @PageableDefault(size = 20) Pageable pageable,
                        Authentication authentication) {

                try {
                        log.info("Admin {} retrieving guest return requests", authentication.getName());

                        Page<ReturnRequestDTO> returnRequests = returnService.getGuestReturnRequests(pageable);

                        log.info("Retrieved {} guest return requests for admin review",
                                        returnRequests.getTotalElements());

                        return ResponseEntity.ok(returnRequests);

                } catch (Exception e) {
                        log.error("Error retrieving guest return requests for admin {}: {}",
                                        authentication.getName(), e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("RETRIEVAL_ERROR",
                                                        "Failed to retrieve guest return requests"));
                }
        }

        /**
         * Review and make decision on return request (Admin/Employee only)
         */
        @PostMapping("/admin/review")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
        @Operation(summary = "Review return request (Admin)", description = "Approve or deny a return request", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Return request reviewed successfully", content = @Content(schema = @Schema(implementation = ReturnRequestDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid review data"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Employee role required"),
                        @ApiResponse(responseCode = "404", description = "Return request not found"),
                        @ApiResponse(responseCode = "409", description = "Return request already reviewed")
        })
        public ResponseEntity<?> reviewReturnRequest(
                        @Valid @RequestBody ReturnDecisionDTO decisionDTO,
                        Authentication authentication) {

                try {
                        log.info("Admin/Vendor {} reviewing return request {} with decision {}",
                                        authentication.getName(), decisionDTO.getReturnRequestId(),
                                        decisionDTO.getDecision());

                        // Validate shop access for VENDOR
                        validateShopAccess(decisionDTO.getReturnRequestId(), authentication);

                        ReturnRequestDTO result = returnService.reviewReturnRequest(decisionDTO);

                        log.info("Return request {} reviewed successfully by admin {} with decision {}",
                                        decisionDTO.getReturnRequestId(), authentication.getName(),
                                        decisionDTO.getDecision());

                        return ResponseEntity.ok(result);

                } catch (IllegalArgumentException e) {
                        log.warn("Invalid review data for return request {}: {}", decisionDTO.getReturnRequestId(),
                                        e.getMessage());
                        return ResponseEntity.badRequest().body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
                } catch (RuntimeException e) {
                        log.error("Error reviewing return request {} by admin {}: {}",
                                        decisionDTO.getReturnRequestId(), authentication.getName(), e.getMessage(), e);

                        if (e.getMessage().contains("not found")) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(createErrorResponse("RETURN_NOT_FOUND", e.getMessage()));
                        } else if (e.getMessage().contains("not in pending status")
                                        || e.getMessage().contains("already")) {
                                return ResponseEntity.status(HttpStatus.CONFLICT)
                                                .body(createErrorResponse("ALREADY_REVIEWED", e.getMessage()));
                        }

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("REVIEW_ERROR", "Failed to review return request"));
                }
        }

        /**
         * Complete quality control for approved return (Admin/Employee only)
         */
        @PostMapping("/admin/{returnRequestId}/quality-control")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
        @Operation(summary = "Complete quality control (Admin)", description = "Complete quality control assessment for an approved return", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Quality control completed successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid quality control data"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Employee role required"),
                        @ApiResponse(responseCode = "404", description = "Return request not found"),
                        @ApiResponse(responseCode = "409", description = "Return request not in correct status for QC")
        })
        public ResponseEntity<?> completeQualityControl(
                        @Parameter(description = "Return request ID") @PathVariable Long returnRequestId,
                        @Valid @RequestBody QualityControlDTO qcDTO,
                        Authentication authentication) {

                try {
                        log.info("Admin {} completing quality control for return request {} with result {}",
                                        authentication.getName(), returnRequestId, qcDTO.getQcResult());

                        // Set the return request ID in the DTO
                        qcDTO.setReturnRequestId(returnRequestId);

                        // Validate shop access for VENDOR
                        validateShopAccess(returnRequestId, authentication);

                        returnService.completeQualityControl(qcDTO);

                        log.info("Quality control completed successfully for return request {} by admin {} with result {}",
                                        returnRequestId, authentication.getName(), qcDTO.getQcResult());

                        return ResponseEntity.ok(createSuccessResponse("Quality control completed successfully"));

                } catch (IllegalArgumentException e) {
                        log.warn("Invalid quality control data for return request {}: {}", returnRequestId,
                                        e.getMessage());
                        return ResponseEntity.badRequest().body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
                } catch (RuntimeException e) {
                        log.error("Error completing quality control for return request {} by admin {}: {}",
                                        returnRequestId, authentication.getName(), e.getMessage(), e);

                        if (e.getMessage().contains("not found")) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(createErrorResponse("RETURN_NOT_FOUND", e.getMessage()));
                        } else if (e.getMessage().contains("not approved") || e.getMessage().contains("status")) {
                                return ResponseEntity.status(HttpStatus.CONFLICT)
                                                .body(createErrorResponse("INVALID_STATUS", e.getMessage()));
                        }

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("QC_ERROR", "Failed to complete quality control"));
                }
        }

        // ==================== UTILITY METHODS ====================

        /**
         * Create standardized error response
         */
        private Map<String, Object> createErrorResponse(String errorCode, String message) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("errorCode", errorCode);
                error.put("message", message);
                error.put("timestamp", System.currentTimeMillis());
                return error;
        }

        /**
         * Submit return request using tracking token (secure endpoint)
         */
        @PostMapping(value = "/submit/tokenized", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Submit return request with tracking token", description = "Submit a return request using a valid tracking token for secure access")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Return request submitted successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid request data or expired token"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<Map<String, Object>> submitTokenizedReturnRequest(
                        @Parameter(description = "Return request data as JSON") @RequestPart("returnRequest") String returnRequestJson,
                        @Parameter(description = "Optional media files (images/videos)") @RequestPart(value = "mediaFiles", required = false) MultipartFile[] mediaFiles) {

                try {
                        log.info("Processing tokenized return request");

                        // Parse the JSON request
                        TokenizedReturnRequestDTO returnRequest = objectMapper.readValue(returnRequestJson,
                                        TokenizedReturnRequestDTO.class);

                        // Submit the return request
                        ReturnRequestDTO response = returnService.submitTokenizedReturnRequest(returnRequest,
                                        mediaFiles);

                        Map<String, Object> successResponse = createSuccessResponse(
                                        "Return request submitted successfully");
                        successResponse.put("data", response);

                        return ResponseEntity.ok(successResponse);

                } catch (IllegalArgumentException e) {
                        log.warn("Invalid tokenized return request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(createErrorResponse("INVALID_REQUEST",
                                                        "Invalid request: " + e.getMessage()));
                } catch (Exception e) {
                        log.error("Error processing tokenized return request", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(createErrorResponse("INTERNAL_ERROR",
                                                        "Failed to process return request" + e.getMessage()));
                }
        }

        /**
         * Create standardized success response
         */
        private Map<String, Object> createSuccessResponse(String message) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", message);
                response.put("timestamp", System.currentTimeMillis());
                return response;
        }

        /**
         * Get pending return requests count (Admin/Employee only)
         */
        @GetMapping("/admin/count/pending")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
        @Operation(summary = "Get pending return requests count", description = "Get count of return requests with PENDING status")
        public ResponseEntity<?> getPendingReturnRequestsCount() {
                try {
                        long count = returnService.countReturnRequestsByStatus(ReturnRequest.ReturnStatus.PENDING);

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("count", count);
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        log.error("Error getting pending return requests count: {}", e.getMessage(), e);
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Failed to get pending return requests count");
                        response.put("count", 0);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
        }

        /**
         * Validate whether the current user has access to the shop associated with the
         * return request
         */
        private void validateShopAccess(Long returnRequestId, Authentication authentication) {
                if (authentication == null || authentication.getPrincipal() == null) {
                        throw new RuntimeException("User not authenticated");
                }

                // If it's an ADMIN or EMPLOYEE, they might have global access (handle based on
                // business rules)
                // For VENDOR, we MUST check shop ownership
                boolean isVendor = authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_VENDOR"));

                if (isVendor && authentication.getPrincipal() instanceof User user) {
                        UUID shopId = returnService.getShopIdForReturnRequest(returnRequestId);
                        shopAuthorizationService.assertCanManageShop(user.getId(), shopId);
                }
        }
}
