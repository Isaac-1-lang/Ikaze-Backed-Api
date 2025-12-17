package com.ecommerce.controller;

import com.ecommerce.dto.CreateShippingCostDTO;
import com.ecommerce.dto.ShippingCostDTO;
import com.ecommerce.dto.UpdateShippingCostDTO;
import com.ecommerce.dto.CalculateOrderShippingRequest;
import com.ecommerce.service.ShopAuthorizationService;
import com.ecommerce.service.ShippingCostService;
import com.ecommerce.ServiceImpl.CustomUserDetails;
import com.ecommerce.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipping-costs")
@RequiredArgsConstructor
@Slf4j
public class ShippingCostController {

    private final ShippingCostService shippingCostService;
    private final ShopAuthorizationService shopAuthorizationService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR','EMPLOYEE')")
    public ResponseEntity<ShippingCostDTO> createShippingCost(
            @Valid @RequestBody CreateShippingCostDTO createShippingCostDTO) {
        try {
            log.info("Creating shipping cost: {}", createShippingCostDTO.getName());
            UUID shopId = createShippingCostDTO.getShopId();
            assertStaffCanManageShop(shopId);
            ShippingCostDTO shippingCost = shippingCostService.createShippingCost(createShippingCostDTO, shopId);
            return ResponseEntity.status(HttpStatus.CREATED).body(shippingCost);
        } catch (Exception e) {
            log.error("Error creating shipping cost: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDOR','EMPLOYEE')")
    public ResponseEntity<Page<ShippingCostDTO>> getAllShippingCosts(
            @RequestParam UUID shopId,
            Pageable pageable) {
        try {
            log.info("Fetching all shipping costs for shop {}", shopId);
            assertStaffCanManageShop(shopId);
            Page<ShippingCostDTO> shippingCosts = shippingCostService.getAllShippingCosts(shopId, pageable);
            return ResponseEntity.ok(shippingCosts);
        } catch (Exception e) {
            log.error("Error fetching shipping costs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<ShippingCostDTO>> getActiveShippingCosts(@RequestParam UUID shopId) {
        try {
            log.info("Fetching active shipping costs for shop {}", shopId);
            List<ShippingCostDTO> shippingCosts = shippingCostService.getActiveShippingCosts(shopId);
            return ResponseEntity.ok(shippingCosts);
        } catch (Exception e) {
            log.error("Error fetching active shipping costs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR','EMPLOYEE')")
    public ResponseEntity<ShippingCostDTO> getShippingCostById(@PathVariable Long id, @RequestParam UUID shopId) {
        try {
            log.info("Fetching shipping cost by ID: {}", id);
            assertStaffCanManageShop(shopId);
            ShippingCostDTO shippingCost = shippingCostService.getShippingCostById(id, shopId);
            return ResponseEntity.ok(shippingCost);
        } catch (Exception e) {
            log.error("Error fetching shipping cost by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR','EMPLOYEE')")
    public ResponseEntity<ShippingCostDTO> updateShippingCost(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShippingCostDTO updateShippingCostDTO,
            @RequestParam UUID shopId) {
        try {
            log.info("Updating shipping cost with ID: {}", id);
            assertStaffCanManageShop(shopId);
            ShippingCostDTO shippingCost = shippingCostService.updateShippingCost(id, updateShippingCostDTO, shopId);
            return ResponseEntity.ok(shippingCost);
        } catch (Exception e) {
            log.error("Error updating shipping cost with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR','EMPLOYEE')")
    public ResponseEntity<Void> deleteShippingCost(@PathVariable Long id, @RequestParam UUID shopId) {
        try {
            log.info("Deleting shipping cost with ID: {}", id);
            assertStaffCanManageShop(shopId);
            shippingCostService.deleteShippingCost(id, shopId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting shipping cost with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('VENDOR','EMPLOYEE')")
    public ResponseEntity<Page<ShippingCostDTO>> searchShippingCosts(
            @RequestParam String name,
            @RequestParam UUID shopId,
            Pageable pageable) {
        try {
            log.info("Searching shipping costs by name: {} for shop {}", name, shopId);
            assertStaffCanManageShop(shopId);
            Page<ShippingCostDTO> shippingCosts = shippingCostService.searchShippingCosts(name, shopId, pageable);
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
            @RequestParam(required = false) BigDecimal orderValue,
            @RequestParam UUID shopId) {
        try {
            log.info("Calculating shipping cost for weight: {}, distance: {}, orderValue: {}",
                    weight, distance, orderValue);
            BigDecimal cost = shippingCostService.calculateShippingCost(weight, distance, orderValue, shopId);
            return ResponseEntity.ok(cost);
        } catch (Exception e) {
            log.error("Error calculating shipping cost: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('VENDOR','EMPLOYEE')")
    public ResponseEntity<ShippingCostDTO> toggleShippingCostStatus(@PathVariable Long id, @RequestParam UUID shopId) {
        try {
            log.info("Toggling shipping cost status for ID: {}", id);
            assertStaffCanManageShop(shopId);
            ShippingCostDTO shippingCost = shippingCostService.toggleShippingCostStatus(id, shopId);
            return ResponseEntity.ok(shippingCost);
        } catch (Exception e) {
            log.error("Error toggling shipping cost status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/calculate-order")
    public ResponseEntity<BigDecimal> calculateOrderShippingCost(
            @RequestBody CalculateOrderShippingRequest request) {
        try {
            log.info("Calculating order shipping cost for address: {}", request.getDeliveryAddress().getCountry());
            BigDecimal cost = shippingCostService.calculateOrderShippingCost(
                    request.getDeliveryAddress(),
                    request.getItems(),
                    request.getOrderValue(),
                    request.getShopId());
            return ResponseEntity.ok(cost);
        } catch (Exception e) {
            log.error("Error calculating order shipping cost: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BigDecimal.ZERO);
        }
    }

    private void assertStaffCanManageShop(UUID shopId) {
        if (shopId == null) {
            throw new IllegalArgumentException("shopId is required");
        }
        UUID currentUserId = getCurrentUserId();
        shopAuthorizationService.assertCanManageShop(currentUserId, shopId);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            String email = customUserDetails.getUsername();
            return userRepository.findByUserEmail(email)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        }

        throw new RuntimeException("Unable to extract user ID from authentication");
    }
}
