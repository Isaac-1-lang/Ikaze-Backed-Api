package com.ecommerce.controller;

import com.ecommerce.dto.CreateReadyForDeliveryGroupDTO;
import com.ecommerce.dto.UpdateReadyForDeliveryGroupDTO;
import com.ecommerce.dto.ReadyForDeliveryGroupDTO;
import com.ecommerce.dto.AddOrdersToGroupDTO;
import com.ecommerce.dto.BulkAddResult;
import com.ecommerce.dto.AgentDto;
import com.ecommerce.dto.DeliveryGroupDto;
import com.ecommerce.service.ReadyForDeliveryGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/delivery-groups")
@Slf4j
@Tag(name = "Ready for Delivery Group Management", description = "APIs for managing ready for delivery groups")
@SecurityRequirement(name = "bearerAuth")
public class ReadyForDeliveryGroupController {

    private final ReadyForDeliveryGroupService deliveryGroupService;

    public ReadyForDeliveryGroupController(ReadyForDeliveryGroupService deliveryGroupService) {
        this.deliveryGroupService = deliveryGroupService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Create ready for delivery group", description = "Create a new ready for delivery group", responses = {
            @ApiResponse(responseCode = "200", description = "Group created successfully", content = @Content(schema = @Schema(implementation = ReadyForDeliveryGroupDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createGroup(@Valid @RequestBody CreateReadyForDeliveryGroupDTO request) {
        try {
            log.info("Creating ready for delivery group: {}", request.getDeliveryGroupName());
            ReadyForDeliveryGroupDTO group = deliveryGroupService.createGroup(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ready for delivery group created successfully");
            response.put("data", group);

            log.info("Ready for delivery group created successfully with ID: {}", group.getDeliveryGroupId());
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found during group creation: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Invalid state during group creation: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid state: " + e.getMessage());
            response.put("errorCode", "INVALID_STATE");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error creating ready for delivery group: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while creating the group.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{groupId}/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Add orders to group", description = "Add orders to an existing ready for delivery group", responses = {
            @ApiResponse(responseCode = "200", description = "Orders added successfully", content = @Content(schema = @Schema(implementation = ReadyForDeliveryGroupDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Group not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> addOrdersToGroup(@PathVariable Long groupId,
            @Valid @RequestBody AddOrdersToGroupDTO request) {
        try {
            log.info("Adding orders to group: {}", groupId);
            ReadyForDeliveryGroupDTO group = deliveryGroupService.addOrdersToGroup(groupId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Orders added to group successfully");
            response.put("data", group);

            log.info("Orders added to group successfully: {}", groupId);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Invalid state: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid state: " + e.getMessage());
            response.put("errorCode", "INVALID_STATE");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error adding orders to group: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while adding orders to group.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{groupId}/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Remove orders from group", description = "Remove orders from a ready for delivery group", responses = {
            @ApiResponse(responseCode = "200", description = "Orders removed successfully"),
            @ApiResponse(responseCode = "404", description = "Group not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> removeOrdersFromGroup(@PathVariable Long groupId, @RequestBody List<Long> orderIds) {
        try {
            log.info("Removing orders from group: {}", groupId);
            deliveryGroupService.removeOrdersFromGroup(groupId, orderIds);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Orders removed from group successfully");

            log.info("Orders removed from group successfully: {}", groupId);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Invalid state: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid state: " + e.getMessage());
            response.put("errorCode", "INVALID_STATE");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error removing orders from group: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while removing orders from group.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Update group", description = "Update a ready for delivery group", responses = {
            @ApiResponse(responseCode = "200", description = "Group updated successfully", content = @Content(schema = @Schema(implementation = ReadyForDeliveryGroupDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Group not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateGroup(@PathVariable Long groupId,
            @Valid @RequestBody UpdateReadyForDeliveryGroupDTO request) {
        try {
            log.info("Updating group: {}", groupId);
            ReadyForDeliveryGroupDTO group = deliveryGroupService.updateGroup(groupId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group updated successfully");
            response.put("data", group);

            log.info("Group updated successfully: {}", groupId);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error updating group: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while updating the group.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Delete group", description = "Delete a ready for delivery group", responses = {
            @ApiResponse(responseCode = "200", description = "Group deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Group not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId) {
        try {
            log.info("Deleting group: {}", groupId);
            deliveryGroupService.deleteGroup(groupId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group deleted successfully");

            log.info("Group deleted successfully: {}", groupId);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Invalid state: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid state: " + e.getMessage());
            response.put("errorCode", "INVALID_STATE");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error deleting group: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while deleting the group.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get group by ID", description = "Get a ready for delivery group by ID", responses = {
            @ApiResponse(responseCode = "200", description = "Group retrieved successfully", content = @Content(schema = @Schema(implementation = ReadyForDeliveryGroupDTO.class))),
            @ApiResponse(responseCode = "404", description = "Group not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getGroupById(@PathVariable Long groupId) {
        try {
            log.info("Getting group by ID: {}", groupId);
            ReadyForDeliveryGroupDTO group = deliveryGroupService.getGroupById(groupId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group retrieved successfully");
            response.put("data", group);

            log.info("Group retrieved successfully: {}", groupId);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error getting group: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while getting the group.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all groups", description = "Get all ready for delivery groups with pagination", responses = {
            @ApiResponse(responseCode = "200", description = "Groups retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAllGroups(Pageable pageable) {
        try {
            log.info("Getting all groups with pagination: page={}, size={}", pageable.getPageNumber(),
                    pageable.getPageSize());
            Page<ReadyForDeliveryGroupDTO> groups = deliveryGroupService.getAllGroups(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Groups retrieved successfully");
            response.put("data", groups.getContent());
            response.put("pagination", Map.of(
                    "page", groups.getNumber(),
                    "size", groups.getSize(),
                    "totalElements", groups.getTotalElements(),
                    "totalPages", groups.getTotalPages(),
                    "hasNext", groups.hasNext(),
                    "hasPrevious", groups.hasPrevious()));

            log.info("Groups retrieved successfully: {} groups found", groups.getTotalElements());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting all groups: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while getting groups.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all groups without pagination", description = "Get all ready for delivery groups without pagination", responses = {
            @ApiResponse(responseCode = "200", description = "Groups retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAllGroupsWithoutPagination() {
        try {
            log.info("Getting all groups without pagination");
            List<ReadyForDeliveryGroupDTO> groups = deliveryGroupService.getAllGroups();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Groups retrieved successfully");
            response.put("data", groups);

            log.info("Groups retrieved successfully: {} groups found", groups.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting all groups: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while getting groups.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all groups for admin", description = "Get ALL delivery groups without any exclusions (includes started, finished, and pending groups) with pagination and optional search", responses = {
            @ApiResponse(responseCode = "200", description = "Groups retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAllGroupsForAdmin(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            log.info("Getting all groups for admin with pagination: page={}, size={}, search={}", 
                    pageable.getPageNumber(), pageable.getPageSize(), search);
            Page<ReadyForDeliveryGroupDTO> groups = deliveryGroupService.getAllGroupsWithoutExclusions(search, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Groups retrieved successfully");
            response.put("data", groups.getContent());
            response.put("pagination", Map.of(
                    "page", groups.getNumber(),
                    "size", groups.getSize(),
                    "totalElements", groups.getTotalElements(),
                    "totalPages", groups.getTotalPages(),
                    "hasNext", groups.hasNext(),
                    "hasPrevious", groups.hasPrevious()));

            log.info("Groups retrieved successfully: {} groups found", groups.getTotalElements());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting all groups for admin: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while getting groups.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "List available groups", description = "Get paginated list of available delivery groups with optional search", responses = {
            @ApiResponse(responseCode = "200", description = "Groups retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> listAvailableGroups(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            log.info("Getting available groups with pagination: page={}, size={}, search={}", 
                    pageable.getPageNumber(), pageable.getPageSize(), search);
            Page<DeliveryGroupDto> groups = deliveryGroupService.listAvailableGroups(search, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Available groups retrieved successfully");
            response.put("data", groups.getContent());
            response.put("pagination", Map.of(
                    "page", groups.getNumber(),
                    "size", groups.getSize(),
                    "totalElements", groups.getTotalElements(),
                    "totalPages", groups.getTotalPages(),
                    "hasNext", groups.hasNext(),
                    "hasPrevious", groups.hasPrevious()));

            log.info("Available groups retrieved successfully: {} groups found", groups.getTotalElements());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting available groups: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while getting available groups.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/bulk-add/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Bulk add orders to group", description = "Add multiple orders to a delivery group", responses = {
            @ApiResponse(responseCode = "200", description = "Orders added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Group not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> bulkAddOrdersToGroup(@PathVariable Long groupId, @RequestBody List<Long> orderIds) {
        try {
            log.info("Bulk adding {} orders to group: {}", orderIds.size(), groupId);
            BulkAddResult result = deliveryGroupService.addOrdersToGroupBulk(groupId, orderIds);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bulk add operation completed");
            response.put("data", result);

            log.info("Bulk add completed: {} added, {} skipped", result.getSuccessfullyAdded(), result.getSkipped());
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Invalid state: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid state: " + e.getMessage());
            response.put("errorCode", "INVALID_STATE");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error bulk adding orders to group: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while bulk adding orders to group.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{groupId}/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Remove order from group", description = "Remove a single order from a delivery group", responses = {
            @ApiResponse(responseCode = "200", description = "Order removed successfully"),
            @ApiResponse(responseCode = "404", description = "Group or order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> removeOrderFromGroup(@PathVariable Long groupId, @PathVariable Long orderId) {
        try {
            log.info("Removing order {} from group: {}", orderId, groupId);
            deliveryGroupService.removeOrderFromGroup(groupId, orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order removed from group successfully");

            log.info("Order {} removed from group {} successfully", orderId, groupId);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Invalid state: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid state: " + e.getMessage());
            response.put("errorCode", "INVALID_STATE");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error removing order from group: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while removing order from group.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "List available agents", description = "Get paginated list of available delivery agents with optional search", responses = {
            @ApiResponse(responseCode = "200", description = "Agents retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> listAvailableAgents(
            @RequestParam(required = false) String search,
            Pageable pageable, 
            Sort sort) {
        try {
            log.info("Getting available agents with pagination: page={}, size={}, search={}", 
                    pageable.getPageNumber(), pageable.getPageSize(), search);
            Page<AgentDto> agents = deliveryGroupService.listAvailableAgents(search, pageable, sort);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Available agents retrieved successfully");
            response.put("data", agents.getContent());
            response.put("pagination", Map.of(
                    "page", agents.getNumber(),
                    "size", agents.getSize(),
                    "totalElements", agents.getTotalElements(),
                    "totalPages", agents.getTotalPages(),
                    "hasNext", agents.hasNext(),
                    "hasPrevious", agents.hasPrevious()));

            log.info("Available agents retrieved successfully: {} agents found", agents.getTotalElements());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting available agents: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while getting available agents.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Find group by order", description = "Find the delivery group that contains a specific order", responses = {
            @ApiResponse(responseCode = "200", description = "Group found successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found or not in any group"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> findGroupByOrder(@PathVariable Long orderId) {
        try {
            log.info("Finding group for order: {}", orderId);
            Optional<DeliveryGroupDto> group = deliveryGroupService.findGroupByOrder(orderId);

            Map<String, Object> response = new HashMap<>();
            if (group.isPresent()) {
                response.put("success", true);
                response.put("message", "Group found for order");
                response.put("data", group.get());
                log.info("Group found for order {}: {}", orderId, group.get().getDeliveryGroupName());
            } else {
                response.put("success", false);
                response.put("message", "Order is not assigned to any delivery group");
                response.put("data", null);
                log.info("No group found for order: {}", orderId);
            }

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Order not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Order not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error finding group for order: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while finding group for order.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{groupId}/start-delivery")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Start delivery group", description = "Start delivery for a group and notify customers", responses = {
            @ApiResponse(responseCode = "200", description = "Delivery started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or group already started"),
            @ApiResponse(responseCode = "404", description = "Group not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> startDelivery(@PathVariable Long groupId) {
        try {
            log.info("Starting delivery for group: {}", groupId);
            Map<String, Object> result = deliveryGroupService.startDelivery(groupId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Delivery started successfully");
            response.put("data", result);

            log.info("Delivery started successfully for group: {}", groupId);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Group not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Group not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Cannot start delivery: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "INVALID_STATE");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("Error starting delivery for group {}: {}", groupId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while starting delivery.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{groupId}/finish-delivery")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Finish delivery group", description = "Finish delivery for a group after all orders are delivered", responses = {
            @ApiResponse(responseCode = "200", description = "Delivery finished successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or not all orders delivered"),
            @ApiResponse(responseCode = "404", description = "Group not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> finishDelivery(@PathVariable Long groupId) {
        try {
            log.info("Finishing delivery for group: {}", groupId);
            Map<String, Object> result = deliveryGroupService.finishDelivery(groupId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Delivery finished successfully");
            response.put("data", result);

            log.info("Delivery finished successfully for group: {}", groupId);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Group not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Group not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Cannot finish delivery: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "INVALID_STATE");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("Error finishing delivery for group {}: {}", groupId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while finishing delivery.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/order/{orderId}/change-group/{newGroupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Change order's delivery group", description = "Move an order from its current group to a new group")
    public ResponseEntity<Map<String, Object>> changeTheOrderGroup(
            @PathVariable Long orderId,
            @PathVariable Long newGroupId) {
        try {
            log.info("Changing order {} to group {}", orderId, newGroupId);
            
            DeliveryGroupDto updatedGroup = deliveryGroupService.changeOrderGroup(orderId, newGroupId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order successfully moved to new delivery group");
            response.put("data", updatedGroup);
            
            log.info("Order {} successfully changed to group {}", orderId, newGroupId);
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (IllegalStateException e) {
            log.warn("Cannot change order group: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "INVALID_STATE");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            log.error("Error changing order {} to group {}: {}", orderId, newGroupId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while changing order group.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
