package com.ecommerce.controller;

import com.ecommerce.dto.ReturnAppealDTO;
import com.ecommerce.dto.SubmitAppealRequestDTO;
import com.ecommerce.service.AppealService;
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
}
