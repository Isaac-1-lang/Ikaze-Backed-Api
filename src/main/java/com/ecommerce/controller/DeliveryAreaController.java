package com.ecommerce.controller;

import com.ecommerce.dto.DeliveryAreaDTO;
import com.ecommerce.service.DeliveryAreaService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delivery-areas")
@Slf4j
public class DeliveryAreaController {

    private final DeliveryAreaService deliveryAreaService;

    @Autowired
    public DeliveryAreaController(DeliveryAreaService deliveryAreaService) {
        this.deliveryAreaService = deliveryAreaService;
    }

    @PostMapping
    public ResponseEntity<?> createDeliveryArea(@Valid @RequestBody DeliveryAreaDTO deliveryAreaDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            DeliveryAreaDTO createdDeliveryArea = deliveryAreaService.createDeliveryArea(deliveryAreaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDeliveryArea);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request while creating delivery area: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error creating delivery area", e);
            response.put("success", false);
            response.put("message", "Failed to create delivery area");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDeliveryArea(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryAreaDTO deliveryAreaDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            DeliveryAreaDTO updatedDeliveryArea = deliveryAreaService.updateDeliveryArea(id, deliveryAreaDTO);
            return ResponseEntity.ok(updatedDeliveryArea);
        } catch (EntityNotFoundException e) {
            log.error("Delivery area not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request while updating delivery area: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error updating delivery area", e);
            response.put("success", false);
            response.put("message", "Failed to update delivery area");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDeliveryArea(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            deliveryAreaService.deleteDeliveryArea(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.error("Delivery area not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error deleting delivery area", e);
            response.put("success", false);
            response.put("message", "Failed to delete delivery area");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDeliveryAreaById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            DeliveryAreaDTO deliveryAreaDTO = deliveryAreaService.getDeliveryAreaById(id);
            return ResponseEntity.ok(deliveryAreaDTO);
        } catch (EntityNotFoundException e) {
            log.error("Delivery area not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving delivery area", e);
            response.put("success", false);
            response.put("message", "Failed to retrieve delivery area");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllDeliveryAreas() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<DeliveryAreaDTO> deliveryAreas = deliveryAreaService.getAllDeliveryAreas();
            return ResponseEntity.ok(deliveryAreas);
        } catch (Exception e) {
            log.error("Error retrieving all delivery areas", e);
            response.put("success", false);
            response.put("message", "Failed to retrieve delivery areas");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/top-level")
    public ResponseEntity<?> getTopLevelDeliveryAreas() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<DeliveryAreaDTO> topLevelAreas = deliveryAreaService.getTopLevelDeliveryAreas();
            return ResponseEntity.ok(topLevelAreas);
        } catch (Exception e) {
            log.error("Error retrieving top-level delivery areas", e);
            response.put("success", false);
            response.put("message", "Failed to retrieve top-level delivery areas");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/sub-areas/{parentId}")
    public ResponseEntity<?> getSubAreas(@PathVariable Long parentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<DeliveryAreaDTO> subAreas = deliveryAreaService.getSubAreas(parentId);
            return ResponseEntity.ok(subAreas);
        } catch (EntityNotFoundException e) {
            log.error("Parent delivery area not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving sub-areas", e);
            response.put("success", false);
            response.put("message", "Failed to retrieve sub-areas");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDeliveryAreas(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (query == null || query.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Search query cannot be empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            List<DeliveryAreaDTO> searchResults = deliveryAreaService.searchDeliveryAreas(query.trim());
            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            log.error("Error searching delivery areas", e);
            response.put("success", false);
            response.put("message", "Failed to search delivery areas");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}