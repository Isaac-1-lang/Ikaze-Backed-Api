package com.ecommerce.controller;

import com.ecommerce.dto.CreateMoneyFlowDTO;
import com.ecommerce.dto.MoneyFlowDTO;
import com.ecommerce.dto.MoneyFlowResponseDTO;
import com.ecommerce.entity.MoneyFlow;
import com.ecommerce.service.MoneyFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/money-flow")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Money Flow Management", description = "APIs for tracking money inflows and outflows")
@SecurityRequirement(name = "bearerAuth")
public class MoneyFlowController {

    private final MoneyFlowService moneyFlowService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Create a new money flow transaction", 
               description = "Record a new money inflow or outflow. Only ADMIN and EMPLOYEE roles can create transactions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Money flow created successfully", 
                        content = @Content(schema = @Schema(implementation = MoneyFlowDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createMoneyFlow(@Valid @RequestBody CreateMoneyFlowDTO createMoneyFlowDTO) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Creating money flow transaction: type={}, amount={}", 
                    createMoneyFlowDTO.getType(), createMoneyFlowDTO.getAmount());

            MoneyFlow moneyFlow = moneyFlowService.save(createMoneyFlowDTO);
            MoneyFlowDTO dto = MoneyFlowDTO.builder()
                    .id(moneyFlow.getId())
                    .description(moneyFlow.getDescription())
                    .type(moneyFlow.getType())
                    .amount(moneyFlow.getAmount())
                    .remainingBalance(moneyFlow.getRemainingBalance())
                    .createdAt(moneyFlow.getCreatedAt())
                    .build();

            response.put("success", true);
            response.put("message", "Money flow transaction created successfully");
            response.put("data", dto);

            log.info("Money flow created successfully with ID: {}", moneyFlow.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for creating money flow: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error creating money flow: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to create money flow transaction");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get money flow data with automatic aggregation", 
               description = "Retrieve money flow data between two dates. The system automatically determines the best aggregation granularity based on the time range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Money flow data retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = MoneyFlowResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getMoneyFlow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Fetching money flow data from {} to {}", start, end);

            if (start.isAfter(end)) {
                response.put("success", false);
                response.put("message", "Start date must be before end date");
                return ResponseEntity.badRequest().body(response);
            }

            MoneyFlowResponseDTO data = moneyFlowService.getMoneyFlow(start, end);

            response.put("success", true);
            response.put("message", "Money flow data retrieved successfully");
            response.put("data", data);

            log.info("Money flow data retrieved successfully with granularity: {}", data.getGranularity());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for fetching money flow: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error fetching money flow data: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve money flow data");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all transactions within a date range", 
               description = "Retrieve all individual money flow transactions between two dates without aggregation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Fetching transactions from {} to {}", start, end);

            if (start.isAfter(end)) {
                response.put("success", false);
                response.put("message", "Start date must be before end date");
                return ResponseEntity.badRequest().body(response);
            }

            List<MoneyFlowDTO> transactions = moneyFlowService.getTransactions(start, end);

            response.put("success", true);
            response.put("message", "Transactions retrieved successfully");
            response.put("data", transactions);
            response.put("count", transactions.size());

            log.info("Retrieved {} transactions", transactions.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching transactions: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve transactions");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get current account balance", 
               description = "Retrieve the current account balance based on the latest transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getCurrentBalance() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Fetching current balance");

            BigDecimal balance = moneyFlowService.getCurrentBalance();

            response.put("success", true);
            response.put("message", "Current balance retrieved successfully");
            response.put("data", Map.of("balance", balance));

            log.info("Current balance: {}", balance);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching current balance: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve current balance");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get a specific money flow transaction by ID", 
               description = "Retrieve details of a specific money flow transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Money flow retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = MoneyFlowDTO.class))),
            @ApiResponse(responseCode = "404", description = "Money flow not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getMoneyFlowById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Fetching money flow with ID: {}", id);

            MoneyFlowDTO moneyFlow = moneyFlowService.getById(id);

            response.put("success", true);
            response.put("message", "Money flow retrieved successfully");
            response.put("data", moneyFlow);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.error("Money flow not found with ID: {}", id);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error fetching money flow: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve money flow");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a money flow transaction", 
               description = "Delete a specific money flow transaction. Only ADMIN role can delete transactions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Money flow deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Money flow not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteMoneyFlow(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Deleting money flow with ID: {}", id);

            moneyFlowService.delete(id);

            response.put("success", true);
            response.put("message", "Money flow deleted successfully");

            log.info("Money flow deleted successfully with ID: {}", id);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.error("Money flow not found with ID: {}", id);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error deleting money flow: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to delete money flow");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
