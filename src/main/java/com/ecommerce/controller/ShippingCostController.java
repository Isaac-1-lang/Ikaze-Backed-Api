package com.ecommerce.controller;

import com.ecommerce.dto.CreateShippingCostDTO;
import com.ecommerce.dto.ShippingCostDTO;
import com.ecommerce.dto.UpdateShippingCostDTO;
import com.ecommerce.service.ShippingCostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping-costs")
@RequiredArgsConstructor
@Slf4j
public class ShippingCostController {

    private final ShippingCostService shippingCostService;

    @PostMapping
    public ResponseEntity<ShippingCostDTO> createShippingCost(
            @Valid @RequestBody CreateShippingCostDTO createShippingCostDTO) {
        try {
            log.info("Creating shipping cost: {}", createShippingCostDTO.getName());
            ShippingCostDTO shippingCost = shippingCostService.createShippingCost(createShippingCostDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(shippingCost);
        } catch (Exception e) {
            log.error("Error creating shipping cost: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<ShippingCostDTO>> getAllShippingCosts(Pageable pageable) {
        try {
            log.info("Fetching all shipping costs");
            Page<ShippingCostDTO> shippingCosts = shippingCostService.getAllShippingCosts(pageable);
            return ResponseEntity.ok(shippingCosts);
        } catch (Exception e) {
            log.error("Error fetching shipping costs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<ShippingCostDTO>> getActiveShippingCosts() {
        try {
            log.info("Fetching active shipping costs");
            List<ShippingCostDTO> shippingCosts = shippingCostService.getActiveShippingCosts();
            return ResponseEntity.ok(shippingCosts);
        } catch (Exception e) {
            log.error("Error fetching active shipping costs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShippingCostDTO> getShippingCostById(@PathVariable Long id) {
        try {
            log.info("Fetching shipping cost by ID: {}", id);
            ShippingCostDTO shippingCost = shippingCostService.getShippingCostById(id);
            return ResponseEntity.ok(shippingCost);
        } catch (Exception e) {
            log.error("Error fetching shipping cost by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShippingCostDTO> updateShippingCost(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShippingCostDTO updateShippingCostDTO) {
        try {
            log.info("Updating shipping cost with ID: {}", id);
            ShippingCostDTO shippingCost = shippingCostService.updateShippingCost(id, updateShippingCostDTO);
            return ResponseEntity.ok(shippingCost);
        } catch (Exception e) {
            log.error("Error updating shipping cost with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShippingCost(@PathVariable Long id) {
        try {
            log.info("Deleting shipping cost with ID: {}", id);
            shippingCostService.deleteShippingCost(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting shipping cost with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ShippingCostDTO>> searchShippingCosts(
            @RequestParam String name,
            Pageable pageable) {
        try {
            log.info("Searching shipping costs by name: {}", name);
            Page<ShippingCostDTO> shippingCosts = shippingCostService.searchShippingCosts(name, pageable);
            return ResponseEntity.ok(shippingCosts);
        } catch (Exception e) {
            log.error("Error searching shipping costs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/calculate")
    public ResponseEntity<BigDecimal> calculateShippingCost(
            @RequestParam(required = false) BigDecimal weight,
            @RequestParam(required = false) BigDecimal distance,
            @RequestParam(required = false) BigDecimal orderValue) {
        try {
            log.info("Calculating shipping cost for weight: {}, distance: {}, orderValue: {}",
                    weight, distance, orderValue);
            BigDecimal cost = shippingCostService.calculateShippingCost(weight, distance, orderValue);
            return ResponseEntity.ok(cost);
        } catch (Exception e) {
            log.error("Error calculating shipping cost: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ShippingCostDTO> toggleShippingCostStatus(@PathVariable Long id) {
        try {
            log.info("Toggling shipping cost status for ID: {}", id);
            ShippingCostDTO shippingCost = shippingCostService.toggleShippingCostStatus(id);
            return ResponseEntity.ok(shippingCost);
        } catch (Exception e) {
            log.error("Error toggling shipping cost status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/calculate-order")
    public ResponseEntity<BigDecimal> calculateOrderShippingCost(
            @RequestBody com.ecommerce.dto.CalculateOrderShippingRequest request) {
        try {
            log.info("Calculating order shipping cost for address: {}", request.getDeliveryAddress().getCountry());
            BigDecimal cost = shippingCostService.calculateOrderShippingCost(
                    request.getDeliveryAddress(),
                    request.getItems(),
                    request.getOrderValue());
            return ResponseEntity.ok(cost);
        } catch (Exception e) {
            log.error("Error calculating order shipping cost: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BigDecimal.ZERO);
        }
    }
}
