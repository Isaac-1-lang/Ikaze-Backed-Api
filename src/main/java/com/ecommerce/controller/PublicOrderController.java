package com.ecommerce.controller;

import com.ecommerce.dto.OrderDeliveryNoteDTO;
import com.ecommerce.entity.OrderDeliveryNote;
import com.ecommerce.repository.OrderDeliveryNoteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Public REST Controller for order-related information
 */
@RestController
@RequestMapping("/api/v1/public/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Orders", description = "Public API for order information")
public class PublicOrderController {

    private final OrderDeliveryNoteRepository orderDeliveryNoteRepository;

    @GetMapping("/{orderId}/delivery-notes")
    @Operation(summary = "Get delivery notes for an order", 
               description = "Retrieve all delivery notes associated with a specific order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Delivery notes retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<Map<String, Object>> getOrderDeliveryNotes(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Fetching delivery notes for order ID: {}", orderId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDeliveryNote> notesPage = orderDeliveryNoteRepository.findByOrderId(orderId, pageable);
        
        List<OrderDeliveryNoteDTO> noteDTOs = notesPage.getContent().stream()
                .map(OrderDeliveryNoteDTO::fromEntity)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
            "notes", noteDTOs,
            "totalNotes", notesPage.getTotalElements(),
            "currentPage", notesPage.getNumber(),
            "totalPages", notesPage.getTotalPages()
        ));
        
        log.info("Retrieved {} delivery notes for order ID: {}", noteDTOs.size(), orderId);
        return ResponseEntity.ok(response);
    }
}
