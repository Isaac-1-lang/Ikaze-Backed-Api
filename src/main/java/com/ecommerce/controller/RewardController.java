package com.ecommerce.controller;

import com.ecommerce.dto.RewardSystemDTO;
import com.ecommerce.dto.UserPointsDTO;
import com.ecommerce.dto.UserRewardSummaryDTO;
import com.ecommerce.service.RewardService;
import com.ecommerce.service.ShopAuthorizationService;
import com.ecommerce.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reward System", description = "APIs for managing the reward system and user points")
public class RewardController {

    private final RewardService rewardService;
    private final ShopAuthorizationService shopAuthorizationService;
    private final UserRepository userRepository;

    @GetMapping("/system")
    public ResponseEntity<RewardSystemDTO> getActiveRewardSystem(@RequestParam UUID shopId) {
        try {
            log.info("Fetching active reward system for shop: {}", shopId);
            RewardSystemDTO system = rewardService.getActiveRewardSystem(shopId);
            if (system != null) {
                log.info("Active reward system found with ID: {} for shop: {}", system.getId(), shopId);
                return ResponseEntity.ok(system);
            }
            log.warn("No active reward system found for shop: {}", shopId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching active reward system for shop {}: {}", shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/systems")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<Map<String, Object>> getAllRewardSystems(
            @RequestParam UUID shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            log.info("Fetching all reward systems for shop {}: page={}, size={}, sortBy={}, sortDir={}", 
                    shopId, page, size, sortBy, sortDir);

            // Validate pagination parameters
            if (page < 0) {
                page = 0;
            }
            if (size < 1 || size > 100) {
                size = 10;
            }

            // Validate sort parameters
            if (!Arrays.asList("id", "createdAt", "updatedAt", "isActive", "isSystemEnabled").contains(sortBy)) {
                sortBy = "id";
            }
            if (!Arrays.asList("asc", "desc").contains(sortDir.toLowerCase())) {
                sortDir = "desc";
            }

            Map<String, Object> result = rewardService.getAllRewardSystems(shopId, page, size, sortBy, sortDir);
            log.info("Retrieved {} reward systems successfully for shop {}", result.get("totalElements"), shopId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching all reward systems for shop {}: {}", shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/system/{id}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<RewardSystemDTO> getRewardSystemById(@PathVariable Long id, @RequestParam UUID shopId) {
        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            log.info("Fetching reward system with ID: {} for shop: {}", id, shopId);
            RewardSystemDTO system = rewardService.getRewardSystemById(id, shopId);
            if (system != null) {
                return ResponseEntity.ok(system);
            }
            log.warn("Reward system not found with ID: {} for shop: {}", id, shopId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching reward system with ID {} for shop {}: {}", id, shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/system")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<RewardSystemDTO> saveRewardSystem(
            @Valid @RequestBody RewardSystemDTO rewardSystemDTO,
            @RequestParam UUID shopId) {
        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            log.info("Saving reward system: {} for shop: {}", 
                    rewardSystemDTO.getId() != null ? "update" : "create", shopId);
            RewardSystemDTO saved = rewardService.saveRewardSystem(rewardSystemDTO, shopId);
            log.info("Reward system saved successfully with ID: {} for shop: {}", saved.getId(), shopId);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for saving reward system: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error saving reward system for shop {}: {}", shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/system/{id}/activate")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<RewardSystemDTO> activateRewardSystem(
            @PathVariable Long id,
            @RequestParam UUID shopId) {
        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            log.info("Activating reward system with ID: {} for shop: {}", id, shopId);
            RewardSystemDTO activated = rewardService.activateRewardSystem(id, shopId);
            log.info("Reward system activated successfully with ID: {} for shop: {}", id, shopId);
            return ResponseEntity.ok(activated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for activating reward system: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error activating reward system with ID {} for shop {}: {}", id, shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/system/{id}/toggle-system")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<RewardSystemDTO> toggleSystemEnabled(
            @PathVariable Long id,
            @RequestParam UUID shopId,
            @RequestParam Boolean enabled) {
        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            log.info("Toggling system enabled for reward system ID: {} to: {} for shop: {}", id, enabled, shopId);
            RewardSystemDTO updated = rewardService.toggleSystemEnabled(id, shopId, enabled);
            log.info("System enabled toggled successfully for reward system ID: {} for shop: {}", id, shopId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for toggling system enabled: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error toggling system enabled for reward system ID {} for shop {}: {}", id, shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/system/{id}/toggle-review-points")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<RewardSystemDTO> toggleReviewPoints(
            @PathVariable Long id,
            @RequestParam UUID shopId,
            @RequestParam Boolean enabled,
            @RequestParam(required = false) Integer pointsAmount) {
        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            log.info("Toggling review points for reward system ID: {} to: {} with points: {} for shop: {}", 
                    id, enabled, pointsAmount, shopId);
            RewardSystemDTO updated = rewardService.toggleReviewPoints(id, shopId, enabled, pointsAmount);
            log.info("Review points toggled successfully for reward system ID: {} for shop: {}", id, shopId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for toggling review points: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error toggling review points for reward system ID {} for shop {}: {}", id, shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/system/{id}/toggle-purchase-points")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<RewardSystemDTO> togglePurchasePoints(
            @PathVariable Long id,
            @RequestParam UUID shopId,
            @RequestParam Boolean enabled) {
        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            log.info("Toggling purchase points for reward system ID: {} to: {} for shop: {}", id, enabled, shopId);
            RewardSystemDTO updated = rewardService.togglePurchasePoints(id, shopId, enabled);
            log.info("Purchase points toggled successfully for reward system ID: {} for shop: {}", id, shopId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for toggling purchase points: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error toggling purchase points for reward system ID {} for shop {}: {}", id, shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/system/{id}/toggle-quantity-based")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<RewardSystemDTO> toggleQuantityBased(
            @PathVariable Long id,
            @RequestParam UUID shopId,
            @RequestParam Boolean enabled) {
        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            log.info("Toggling quantity-based rewards for reward system ID: {} to: {} for shop: {}", id, enabled, shopId);
            RewardSystemDTO updated = rewardService.toggleQuantityBased(id, shopId, enabled);
            log.info("Quantity-based rewards toggled successfully for reward system ID: {} for shop: {}", id, shopId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for toggling quantity-based rewards: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error toggling quantity-based rewards for reward system ID {} for shop {}: {}", id, shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/system/{id}/toggle-amount-based")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<RewardSystemDTO> toggleAmountBased(
            @PathVariable Long id,
            @RequestParam UUID shopId,
            @RequestParam Boolean enabled) {
        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            log.info("Toggling amount-based rewards for reward system ID: {} to: {} for shop: {}", id, enabled, shopId);
            RewardSystemDTO updated = rewardService.toggleAmountBased(id, shopId, enabled);
            log.info("Amount-based rewards toggled successfully for reward system ID: {} for shop: {}", id, shopId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for toggling amount-based rewards: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error toggling amount-based rewards for reward system ID {} for shop {}: {}", id, shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/system/{id}/toggle-percentage-based")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<RewardSystemDTO> togglePercentageBased(
            @PathVariable Long id,
            @RequestParam UUID shopId,
            @RequestParam Boolean enabled,
            @RequestParam(required = false) BigDecimal percentageRate) {
        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            log.info("Toggling percentage-based rewards for reward system ID: {} to: {} with rate: {} for shop: {}", 
                    id, enabled, percentageRate, shopId);
            RewardSystemDTO updated = rewardService.togglePercentageBased(id, shopId, enabled, percentageRate);
            log.info("Percentage-based rewards toggled successfully for reward system ID: {} for shop: {}", id, shopId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for toggling percentage-based rewards: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error toggling percentage-based rewards for reward system ID {} for shop {}: {}", id, shopId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new RuntimeException("User not authenticated");
            }

            Object principal = auth.getPrincipal();

            if (principal instanceof com.ecommerce.ServiceImpl.CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            }

            if (principal instanceof com.ecommerce.entity.User user && user.getId() != null) {
                return user.getId();
            }

            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                return userRepository.findByUserEmail(email)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            }

            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return userRepository.findByUserEmail(name)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + name));
            }
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to get current user ID: " + e.getMessage());
        }
        throw new RuntimeException("Unable to get current user ID");
    }

    @PostMapping("/users/{userId}/order-points")
    public ResponseEntity<UserPointsDTO> awardPointsForOrder(@PathVariable UUID userId, @RequestParam Long orderId,
            @RequestParam Integer productCount, @RequestParam Double orderAmount) {
        try {
            log.info("Awarding points for order: userId={}, orderId={}, productCount={}, orderAmount={}",
                    userId, orderId, productCount, orderAmount);
            UserPointsDTO awarded = rewardService.awardPointsForOrder(userId, orderId, productCount,
                    BigDecimal.valueOf(orderAmount));
            if (awarded != null) {
                log.info("Points awarded successfully for order: {}", orderId);
                return ResponseEntity.ok(awarded);
            }
            log.warn("No points awarded for order: {}", orderId);
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for awarding order points: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error awarding points for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/users/{userId}/review-points")
    public ResponseEntity<UserPointsDTO> awardPointsForReview(@PathVariable UUID userId,
            @RequestParam(required = false) String description) {
        try {
            log.info("Awarding points for review: userId={}, description={}", userId, description);
            UserPointsDTO awarded = rewardService.awardPointsForReview(userId, description);
            if (awarded != null) {
                log.info("Points awarded successfully for review by user: {}", userId);
                return ResponseEntity.ok(awarded);
            }
            log.warn("No points awarded for review by user: {}", userId);
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for awarding review points: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error awarding points for review by user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/users/{userId}/deduct-points")
    public ResponseEntity<UserPointsDTO> deductPointsForPurchase(@PathVariable UUID userId,
            @RequestParam Integer points, @RequestParam(required = false) String description) {
        try {
            log.info("Deducting points for purchase: userId={}, points={}, description={}", userId, points,
                    description);
            UserPointsDTO deducted = rewardService.deductPointsForPurchase(userId, points, description);
            log.info("Points deducted successfully for user: {}", userId);
            return ResponseEntity.ok(deducted);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for deducting points: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.warn("Runtime error deducting points: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error deducting points for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users/{userId}/current-points")
    public ResponseEntity<Integer> getUserCurrentPoints(@PathVariable UUID userId) {
        try {
            log.info("Getting current points for user: {}", userId);
            Integer points = rewardService.getUserCurrentPoints(userId);
            log.info("Current points retrieved successfully for user: {}", userId);
            return ResponseEntity.ok(points);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for getting current points: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting current points for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<UserRewardSummaryDTO> getUserRewardSummary(@PathVariable UUID userId) {
        try {
            log.info("Getting reward summary for user: {}", userId);
            UserRewardSummaryDTO summary = rewardService.getUserRewardSummary(userId);
            log.info("Reward summary retrieved successfully for user: {}", userId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for getting reward summary: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting reward summary for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users/{userId}/history")
    public ResponseEntity<List<UserPointsDTO>> getUserPointsHistory(@PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("Getting points history for user: {} (page: {}, size: {})", userId, page, size);
            List<UserPointsDTO> history = rewardService.getUserPointsHistory(userId, page, size);
            log.info("Points history retrieved successfully for user: {} ({} records)", userId, history.size());
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for getting points history: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting points history for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/calculate-points")
    public ResponseEntity<Integer> calculateOrderPoints(@RequestParam Integer productCount,
            @RequestParam Double orderAmount) {
        try {
            log.info("Calculating order points: productCount={}, orderAmount={}", productCount, orderAmount);
            Integer points = rewardService.calculateOrderPoints(productCount, BigDecimal.valueOf(orderAmount));
            log.info("Order points calculated successfully: {}", points);
            return ResponseEntity.ok(points);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for calculating order points: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error calculating order points: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/points-value")
    public ResponseEntity<Double> calculatePointsValue(@RequestParam Integer points) {
        try {
            log.info("Calculating points value for: {} points", points);
            BigDecimal value = rewardService.calculatePointsValue(points);
            log.info("Points value calculated successfully: {}", value);
            return ResponseEntity.ok(value.doubleValue());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for calculating points value: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error calculating points value: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users/{userId}/has-enough-points")
    public ResponseEntity<Boolean> hasEnoughPoints(@PathVariable UUID userId, @RequestParam Integer requiredPoints) {
        try {
            log.info("Checking if user {} has enough points: required={}", userId, requiredPoints);
            boolean hasEnough = rewardService.hasEnoughPoints(userId, requiredPoints);
            log.info("Points check completed for user {}: hasEnough={}", userId, hasEnough);
            return ResponseEntity.ok(hasEnough);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for checking points: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error checking points for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products/points-required")
    public ResponseEntity<Integer> getPointsRequiredForProduct(@RequestParam Double productPrice) {
        try {
            log.info("Getting points required for product price: {}", productPrice);
            Integer pointsRequired = rewardService.getPointsRequiredForProduct(BigDecimal.valueOf(productPrice));
            log.info("Points required calculated successfully: {}", pointsRequired);
            return ResponseEntity.ok(pointsRequired);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for getting points required: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting points required for product: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
