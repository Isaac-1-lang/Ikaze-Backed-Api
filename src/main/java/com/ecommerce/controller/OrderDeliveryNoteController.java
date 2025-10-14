package com.ecommerce.controller;

import com.ecommerce.dto.CreateDeliveryNoteRequest;
import com.ecommerce.dto.OrderDeliveryNoteDTO;
import com.ecommerce.dto.UpdateDeliveryNoteRequest;
import com.ecommerce.service.OrderDeliveryNoteService;
import com.ecommerce.Exception.ResourceNotFoundException;
import com.ecommerce.Exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/delivery-notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Delivery Notes", description = "APIs for managing delivery notes by delivery agents")
public class OrderDeliveryNoteController {

    private final OrderDeliveryNoteService noteService;

    @PostMapping
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Create a delivery note", description = "Create a new delivery note for an order or delivery group")
    public ResponseEntity<Map<String, Object>> createNote(
            @Valid @RequestBody CreateDeliveryNoteRequest request,
            Authentication authentication) {
        try {
            
            OrderDeliveryNoteDTO note = noteService.createNote(request, authentication.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Delivery note created successfully");
            response.put("data", note);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating delivery note: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Update a delivery note", description = "Update an existing delivery note (only by the agent who created it)")
    public ResponseEntity<Map<String, Object>> updateNote(
            @PathVariable Long noteId,
            @Valid @RequestBody UpdateDeliveryNoteRequest request,
            Authentication authentication) {
        try {
            log.info("Updating delivery note {} by agent: {}", noteId, authentication.getName());
            
            OrderDeliveryNoteDTO note = noteService.updateNote(noteId, request, authentication.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Delivery note updated successfully");
            response.put("data", note);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating delivery note: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Delete a delivery note", description = "Delete a delivery note (only by the agent who created it)")
    public ResponseEntity<Map<String, Object>> deleteNote(
            @PathVariable Long noteId,
            Authentication authentication) {
        try {
            log.info("Deleting delivery note {} by agent: {}", noteId, authentication.getName());
            
            noteService.deleteNote(noteId, authentication.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Delivery note deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting delivery note: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('DELIVERY_AGENT', 'ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get notes for an order", description = "Get all delivery notes for a specific order")
    public ResponseEntity<Map<String, Object>> getNotesForOrder(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        try {
            log.info("Fetching notes for order: {}", orderId);
            
            Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<OrderDeliveryNoteDTO> notes = noteService.getNotesForOrder(orderId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", notes.getContent());
            response.put("currentPage", notes.getNumber());
            response.put("totalItems", notes.getTotalElements());
            response.put("totalPages", notes.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching notes for order: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('DELIVERY_AGENT', 'ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get notes for a delivery group", description = "Get all group-general notes for a delivery group")
    public ResponseEntity<Map<String, Object>> getNotesForDeliveryGroup(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        try {
            log.info("Fetching notes for delivery group: {}", groupId);
            
            Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<OrderDeliveryNoteDTO> notes = noteService.getNotesForDeliveryGroup(groupId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", notes.getContent());
            response.put("currentPage", notes.getNumber());
            response.put("totalItems", notes.getTotalElements());
            response.put("totalPages", notes.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching notes for delivery group: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/group/{groupId}/all")
    @PreAuthorize("hasAnyRole('DELIVERY_AGENT', 'ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all notes for a delivery group", description = "Get all notes (order-specific and group-general) for a delivery group")
    public ResponseEntity<Map<String, Object>> getAllNotesForDeliveryGroup(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        try {
            log.info("Fetching all notes for delivery group: {}", groupId);
            
            Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<OrderDeliveryNoteDTO> notes = noteService.getAllNotesForDeliveryGroup(groupId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", notes.getContent());
            response.put("currentPage", notes.getNumber());
            response.put("totalItems", notes.getTotalElements());
            response.put("totalPages", notes.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all notes for delivery group: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/agent/my-notes")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Get my notes", description = "Get all notes created by the authenticated delivery agent")
    public ResponseEntity<Map<String, Object>> getMyNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            Authentication authentication) {
        try {
            log.info("Fetching notes for agent: {}", authentication.getName());
            
            Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<OrderDeliveryNoteDTO> notes = noteService.getNotesByAgent(authentication.getName(), pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", notes.getContent());
            response.put("currentPage", notes.getNumber());
            response.put("totalItems", notes.getTotalElements());
            response.put("totalPages", notes.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching notes for agent: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/{noteId}")
    @PreAuthorize("hasAnyRole('DELIVERY_AGENT', 'ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get note by ID", description = "Get a specific delivery note by its ID")
    public ResponseEntity<Map<String, Object>> getNoteById(@PathVariable Long noteId) {
        try {
            log.info("Fetching note by ID: {}", noteId);
            
            OrderDeliveryNoteDTO note = noteService.getNoteById(noteId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", note);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching note by ID: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
