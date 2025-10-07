package com.ecommerce.controller;

import com.ecommerce.dto.ReturnAppealDTO;
import com.ecommerce.dto.SubmitAppealRequestDTO;
import com.ecommerce.dto.TokenizedAppealRequestDTO;
import com.ecommerce.dto.AppealDecisionDTO;
import com.ecommerce.dto.AppealStatisticsDTO;
import com.ecommerce.service.AppealService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appeals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Appeal Management", description = "APIs for managing return request appeals")
public class AppealController {

    private final AppealService appealService;

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit an appeal for a denied return request", description = "Submit an appeal with reason, description, and optional media files (images/videos)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Appeal submitted successfully", content = @Content(schema = @Schema(implementation = ReturnAppealDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or appeal not allowed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Return request not found"),
            @ApiResponse(responseCode = "409", description = "Appeal already exists for this return request")
    })
    public ResponseEntity<ReturnAppealDTO> submitAppeal(
            @Parameter(description = "Return request ID", required = true) @RequestParam Long returnRequestId,

            @Parameter(description = "Customer ID (optional for guest customers)") @RequestParam(required = false) UUID customerId,

            @Parameter(description = "Appeal reason", required = true) @RequestParam String reason,

            @Parameter(description = "Detailed appeal description") @RequestParam(required = false) String description,

            @Parameter(description = "Media files (images and videos) to support the appeal") @RequestParam(value = "mediaFiles", required = false) MultipartFile[] mediaFiles) {

        log.info("Received appeal submission request for return request {} by customer {}",
                returnRequestId, customerId);

        SubmitAppealRequestDTO submitDTO = new SubmitAppealRequestDTO();
        submitDTO.setReturnRequestId(returnRequestId);
        submitDTO.setCustomerId(customerId);
        submitDTO.setReason(reason);
        submitDTO.setDescription(description);

        ReturnAppealDTO appealDTO = appealService.submitAppeal(submitDTO, mediaFiles);

        log.info("Appeal {} submitted successfully for return request {}",
                appealDTO.getId(), returnRequestId);

        return ResponseEntity.status(HttpStatus.CREATED).body(appealDTO);
    }

    @PostMapping(value = "/submit/tokenized", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit an appeal using tracking token (for guest users)", description = "Submit an appeal with tracking token for guest users who don't have accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Appeal submitted successfully", content = @Content(schema = @Schema(implementation = ReturnAppealDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data, expired token, or appeal not allowed"),
            @ApiResponse(responseCode = "404", description = "Return request not found"),
            @ApiResponse(responseCode = "409", description = "Appeal already exists for this return request")
    })
    public ResponseEntity<ReturnAppealDTO> submitTokenizedAppeal(
            @Parameter(description = "Return request ID", required = true) @RequestParam Long returnRequestId,

            @Parameter(description = "Tracking token for guest access", required = true) @RequestParam String trackingToken,

            @Parameter(description = "Appeal reason", required = true) @RequestParam String reason,

            @Parameter(description = "Detailed appeal description") @RequestParam(required = false) String description,

            @Parameter(description = "Media files (images and videos) to support the appeal") @RequestParam(value = "mediaFiles", required = false) MultipartFile[] mediaFiles) {

        log.info("Received tokenized appeal submission request for return request {} with token",
                returnRequestId);

        TokenizedAppealRequestDTO submitDTO = new TokenizedAppealRequestDTO();
        submitDTO.setReturnRequestId(returnRequestId);
        submitDTO.setTrackingToken(trackingToken);
        submitDTO.setReason(reason);
        submitDTO.setDescription(description);

        ReturnAppealDTO appealDTO = appealService.submitTokenizedAppeal(submitDTO, mediaFiles);

        log.info("Tokenized appeal {} submitted successfully for return request {}",
                appealDTO.getId(), returnRequestId);

        return ResponseEntity.status(HttpStatus.CREATED).body(appealDTO);
    }

    @GetMapping("/{appealId}")
    @Operation(summary = "Get appeal by ID", description = "Retrieve appeal details including media attachments")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appeal found", content = @Content(schema = @Schema(implementation = ReturnAppealDTO.class))),
            @ApiResponse(responseCode = "404", description = "Appeal not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ReturnAppealDTO> getAppealById(
            @Parameter(description = "Appeal ID", required = true) @PathVariable Long appealId) {

        log.info("Retrieving appeal details for appeal ID: {}", appealId);

        ReturnAppealDTO appealDTO = appealService.getAppealById(appealId);

        return ResponseEntity.ok(appealDTO);
    }

    @GetMapping("/return-request/{returnRequestId}")
    @Operation(summary = "Get appeal by return request ID", description = "Retrieve appeal for a specific return request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appeal found", content = @Content(schema = @Schema(implementation = ReturnAppealDTO.class))),
            @ApiResponse(responseCode = "404", description = "Appeal not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ReturnAppealDTO> getAppealByReturnRequestId(
            @Parameter(description = "Return request ID", required = true) @PathVariable Long returnRequestId) {

        log.info("Retrieving appeal for return request ID: {}", returnRequestId);

        ReturnAppealDTO appealDTO = appealService.getAppealByReturnRequestId(returnRequestId);

        return ResponseEntity.ok(appealDTO);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all appeals with filtering and pagination", description = "Admin endpoint to retrieve appeals with various filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appeals retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Employee access required")
    })
    public ResponseEntity<Page<ReturnAppealDTO>> getAllAppeals(
            @Parameter(description = "Appeal status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Customer name filter") @RequestParam(required = false) String customerName,
            @Parameter(description = "Order code filter") @RequestParam(required = false) String orderCode,
            @Parameter(description = "From date filter") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "To date filter") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "submittedAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Admin retrieving appeals with filters - status: {}, customerName: {}, orderCode: {}", 
                status, customerName, orderCode);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReturnAppealDTO> appeals = appealService.getAllAppealsForAdmin(
                status, null, customerName, orderCode, fromDate, toDate, pageable);

        return ResponseEntity.ok(appeals);
    }

    @PostMapping("/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Review and make decision on appeal", description = "Admin endpoint to approve or deny appeals")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appeal decision submitted successfully", content = @Content(schema = @Schema(implementation = ReturnAppealDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid decision data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Employee access required"),
            @ApiResponse(responseCode = "404", description = "Appeal not found")
    })
    public ResponseEntity<ReturnAppealDTO> reviewAppeal(
            @Parameter(description = "Appeal decision data", required = true) @Valid @RequestBody AppealDecisionDTO decisionDTO) {

        log.info("Admin reviewing appeal {} with decision: {}", decisionDTO.getAppealId(), decisionDTO.getDecision());

        ReturnAppealDTO appealDTO = appealService.reviewAppeal(decisionDTO);

        log.info("Appeal {} decision completed: {}", appealDTO.getId(), appealDTO.getStatus());

        return ResponseEntity.ok(appealDTO);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get appeal statistics", description = "Admin endpoint to retrieve appeal statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Employee access required")
    })
    public ResponseEntity<AppealStatisticsDTO> getAppealStats() {
        log.info("Admin retrieving appeal statistics");

        AppealStatisticsDTO stats = appealService.getAppealStatistics();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/pending/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get pending appeals count", description = "Admin endpoint to get count of pending appeals for sidebar badge")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Employee access required")
    })
    public ResponseEntity<Long> getPendingAppealsCount() {
        log.info("Admin retrieving pending appeals count");

        Long count = appealService.getPendingAppealsCount();

        return ResponseEntity.ok(count);
    }
}
