package com.ecommerce.service.impl;

import com.ecommerce.dto.RewardSystemDTO;
import com.ecommerce.dto.UserPointsDTO;
import com.ecommerce.dto.UserRewardSummaryDTO;
import com.ecommerce.entity.RewardSystem;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserPoints;
import com.ecommerce.entity.UserPoints.PointsType;
import com.ecommerce.repository.RewardSystemRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserPointsRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.RewardService;
import com.ecommerce.service.ShopAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.ecommerce.dto.RewardRangeDTO;
import com.ecommerce.entity.RewardRange;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RewardServiceImpl implements RewardService {

    private final RewardSystemRepository rewardSystemRepository;
    private final UserPointsRepository userPointsRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ShopAuthorizationService shopAuthorizationService;

    @Override
    public RewardSystemDTO getActiveRewardSystem(UUID shopId) {
        Optional<RewardSystem> activeSystem = rewardSystemRepository.findByShopShopIdAndIsActiveTrue(shopId);
        if (activeSystem.isPresent()) {
            return convertToDTO(activeSystem.get());
        }
        return null;
    }

    @Override
    @Deprecated
    public RewardSystemDTO getActiveRewardSystem() {
        // For backward compatibility - returns first active system found
        // This should be replaced with shop-scoped calls
        Optional<RewardSystem> activeSystem = rewardSystemRepository.findByIsActiveTrue();
        if (activeSystem.isPresent()) {
            return convertToDTO(activeSystem.get());
        }
        return null;
    }

    @Override
    public Map<String, Object> getAllRewardSystems(UUID shopId, int page, int size, String sortBy, String sortDir) {
        try {
            // Create sort object
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);

            // Create pageable object
            PageRequest pageRequest = PageRequest.of(page, size, sort);

            // Fetch paginated data for the shop
            Page<RewardSystem> rewardSystemPage = rewardSystemRepository.findByShopShopId(shopId, pageRequest);

            // Convert to DTOs
            List<RewardSystemDTO> content = rewardSystemPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // Build response map
            Map<String, Object> response = new HashMap<>();
            response.put("content", content);
            response.put("currentPage", rewardSystemPage.getNumber());
            response.put("totalPages", rewardSystemPage.getTotalPages());
            response.put("totalElements", rewardSystemPage.getTotalElements());
            response.put("size", rewardSystemPage.getSize());
            response.put("first", rewardSystemPage.isFirst());
            response.put("last", rewardSystemPage.isLast());
            response.put("empty", rewardSystemPage.isEmpty());

            return response;
        } catch (Exception e) {
            log.error("Error fetching all reward systems: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch reward systems", e);
        }
    }

    @Override
    public RewardSystemDTO getRewardSystemById(Long id, UUID shopId) {
        RewardSystem system = rewardSystemRepository.findByIdAndShopShopId(id, shopId)
                .orElseThrow(() -> new RuntimeException("Reward system not found"));
        return convertToDTO(system);
    }

    @Override
    public RewardSystemDTO saveRewardSystem(RewardSystemDTO rewardSystemDTO, UUID shopId) {
        log.info("Saving reward system: {} for shop: {}", rewardSystemDTO.getDescription(), shopId);
        log.info("Received reward ranges count: {}",
                rewardSystemDTO.getRewardRanges() != null ? rewardSystemDTO.getRewardRanges().size() : 0);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        if (rewardSystemDTO.getRewardRanges() != null) {
            for (int i = 0; i < rewardSystemDTO.getRewardRanges().size(); i++) {
                RewardRangeDTO range = rewardSystemDTO.getRewardRanges().get(i);
                log.info("RewardRange[{}]: rangeType={}, minValue={}, maxValue={}, points={}",
                        i, range.getRangeType(), range.getMinValue(), range.getMaxValue(), range.getPoints());
            }
        }

        if (rewardSystemDTO.getIsPurchasePointsEnabled()) {
            if (rewardSystemDTO.getIsQuantityBasedEnabled() &&
                    (rewardSystemDTO.getRewardRanges() == null ||
                            rewardSystemDTO.getRewardRanges().stream()
                                    .noneMatch(range -> "QUANTITY".equals(range.getRangeType())))) {
                throw new IllegalArgumentException("Quantity-based rewards require at least one quantity range");
            }

            if (rewardSystemDTO.getIsAmountBasedEnabled() &&
                    (rewardSystemDTO.getRewardRanges() == null ||
                            rewardSystemDTO.getRewardRanges().stream()
                                    .noneMatch(range -> "AMOUNT".equals(range.getRangeType())))) {
                throw new IllegalArgumentException("Amount-based rewards require at least one amount range");
            }
        }

        if (rewardSystemDTO.getId() != null) {
            RewardSystem existing = rewardSystemRepository.findByIdAndShopShopId(rewardSystemDTO.getId(), shopId)
                    .orElseThrow(() -> new RuntimeException("Reward system not found"));
            updateRewardSystem(existing, rewardSystemDTO);
            return convertToDTO(rewardSystemRepository.save(existing));
        } else {
            if (rewardSystemDTO.getIsActive()) {
                deactivateCurrentSystem(shopId);
            }
            RewardSystem newSystem = convertToEntity(rewardSystemDTO, shop);

            // Ensure reward ranges are properly set for new systems
            if (rewardSystemDTO.getRewardRanges() != null && !rewardSystemDTO.getRewardRanges().isEmpty()) {
                List<RewardRange> ranges = rewardSystemDTO.getRewardRanges().stream()
                        .map(rangeDTO -> convertRangeToEntity(rangeDTO, newSystem))
                        .collect(Collectors.toList());
                newSystem.setRewardRanges(ranges);
            }

            return convertToDTO(rewardSystemRepository.save(newSystem));
        }
    }

    @Override
    public RewardSystemDTO activateRewardSystem(Long rewardSystemId, UUID shopId) {
        RewardSystem system = rewardSystemRepository.findByIdAndShopShopId(rewardSystemId, shopId)
                .orElseThrow(() -> new RuntimeException("Reward system not found"));

        // Deactivate only the active system for this shop
        deactivateCurrentSystem(shopId);
        system.setIsActive(true);
        system.setUpdatedAt(LocalDateTime.now());

        return convertToDTO(rewardSystemRepository.save(system));
    }

    @Override
    public RewardSystemDTO toggleSystemEnabled(Long rewardSystemId, UUID shopId, Boolean enabled) {
        RewardSystem system = rewardSystemRepository.findByIdAndShopShopId(rewardSystemId, shopId)
                .orElseThrow(() -> new RuntimeException("Reward system not found"));

        system.setIsSystemEnabled(enabled);
        system.setUpdatedAt(LocalDateTime.now());

        return convertToDTO(rewardSystemRepository.save(system));
    }

    @Override
    public RewardSystemDTO toggleReviewPoints(Long rewardSystemId, UUID shopId, Boolean enabled, Integer pointsAmount) {
        RewardSystem system = rewardSystemRepository.findByIdAndShopShopId(rewardSystemId, shopId)
                .orElseThrow(() -> new RuntimeException("Reward system not found"));

        system.setIsReviewPointsEnabled(enabled);
        if (enabled && pointsAmount != null) {
            system.setReviewPointsAmount(pointsAmount);
        }
        system.setUpdatedAt(LocalDateTime.now());

        return convertToDTO(rewardSystemRepository.save(system));
    }

    @Override
    public RewardSystemDTO togglePurchasePoints(Long rewardSystemId, UUID shopId, Boolean enabled) {
        RewardSystem system = rewardSystemRepository.findByIdAndShopShopId(rewardSystemId, shopId)
                .orElseThrow(() -> new RuntimeException("Reward system not found"));

        system.setIsPurchasePointsEnabled(enabled);
        system.setUpdatedAt(LocalDateTime.now());

        return convertToDTO(rewardSystemRepository.save(system));
    }

    @Override
    public RewardSystemDTO toggleQuantityBased(Long rewardSystemId, UUID shopId, Boolean enabled) {
        RewardSystem system = rewardSystemRepository.findByIdAndShopShopId(rewardSystemId, shopId)
                .orElseThrow(() -> new RuntimeException("Reward system not found"));

        system.setIsQuantityBasedEnabled(enabled);
        system.setUpdatedAt(LocalDateTime.now());

        return convertToDTO(rewardSystemRepository.save(system));
    }

    @Override
    public RewardSystemDTO toggleAmountBased(Long rewardSystemId, UUID shopId, Boolean enabled) {
        RewardSystem system = rewardSystemRepository.findByIdAndShopShopId(rewardSystemId, shopId)
                .orElseThrow(() -> new RuntimeException("Reward system not found"));

        system.setIsAmountBasedEnabled(enabled);
        system.setUpdatedAt(LocalDateTime.now());

        return convertToDTO(rewardSystemRepository.save(system));
    }

    @Override
    public RewardSystemDTO togglePercentageBased(Long rewardSystemId, UUID shopId, Boolean enabled,
            BigDecimal percentageRate) {
        RewardSystem system = rewardSystemRepository.findByIdAndShopShopId(rewardSystemId, shopId)
                .orElseThrow(() -> new RuntimeException("Reward system not found"));

        system.setIsPercentageBasedEnabled(enabled);
        if (enabled && percentageRate != null) {
            system.setPercentageRate(percentageRate);
        }
        system.setUpdatedAt(LocalDateTime.now());

        return convertToDTO(rewardSystemRepository.save(system));
    }

    @Override
    public Integer calculateOrderPoints(Integer productCount, BigDecimal orderAmount) {
        RewardSystem activeSystem = getActiveRewardSystemEntity();
        if (activeSystem == null) {
            return 0;
        }
        return activeSystem.calculatePurchasePoints(productCount, orderAmount);
    }

    private void logDebugToFile(String message) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("checkout_debug_logs.txt");
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String logLine = timestamp + " - " + message + "\n";
            java.nio.file.Files.write(path, logLine.getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.error("Failed to write to debug file: " + e.getMessage());
        }
    }

    @Override
    public void checkRewardableOnOrderAndReward(com.ecommerce.entity.Order order) {
        log.info("Starting the rewarding services and checking correctly");
        try {
            if (order == null || order.getUser() == null)
                return;
            UUID userId = order.getUser().getId();
            Long orderId = order.getOrderId();

            log.info("Checking if order {} is rewardable for user {}", orderId, userId);

            if (order.getShopOrders() != null) {
                for (com.ecommerce.entity.ShopOrder shopOrder : order.getShopOrders()) {
                    Integer productCount = shopOrder.getItems() != null ? shopOrder.getItems().size() : 0;
                    BigDecimal orderAmount = shopOrder.getTotalAmount();
                    Shop shop = shopOrder.getShop();
                    logDebugToFile("Checking rewardable for shop order");
                    if (shop != null) {
                        logDebugToFile("Shop not null");
                        checkRewardableForShopOrder(userId, orderId, productCount, orderAmount, shop);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking and rewarding points for order {}: {}",
                    order == null ? "null" : order.getOrderId(), e.getMessage(), e);
        }
    }

    private void checkRewardableForShopOrder(UUID userId, Long orderId, Integer productCount, BigDecimal orderAmount,
            Shop shop) {
        logDebugToFile("Checkin rewardable for shop order");
        logDebugToFile("Shop ID: " + shop.getShopId() + ", Shop Name: " + shop.getName());
        logDebugToFile("Product Count: " + productCount + ", Order Amount: " + orderAmount);

        try {
            RewardSystem activeSystem = getActiveRewardSystemEntity(shop.getShopId());
            if (activeSystem == null) {
                logDebugToFile("No active reward system found for shop " + shop.getName());
                return;
            }

            logDebugToFile("Found reward system - isSystemEnabled: " + activeSystem.getIsSystemEnabled());
            logDebugToFile("isPurchasePointsEnabled: " + activeSystem.getIsPurchasePointsEnabled());
            logDebugToFile("isQuantityBasedEnabled: " + activeSystem.getIsQuantityBasedEnabled());
            logDebugToFile("isAmountBasedEnabled: " + activeSystem.getIsAmountBasedEnabled());

            if (!activeSystem.getIsSystemEnabled()) {
                logDebugToFile("Reward system disabled for shop " + shop.getName());
                return;
            }

            Integer pointsEarned = activeSystem.calculatePurchasePoints(productCount, orderAmount);
            logDebugToFile("Points calculated: " + pointsEarned + " for shop " + shop.getName());

            if (pointsEarned <= 0) {
                logDebugToFile(
                        "ShopOrder from " + shop.getName() + " in Order " + orderId
                                + " does not meet reward criteria. Products: " + productCount + ", Amount: "
                                + orderAmount + ". No points awarded.");
                return;
            }

            logDebugToFile("ShopOrder from " + shop.getName() + " in Order " + orderId
                    + " meets reward criteria. Awarding " + pointsEarned + " points to user " + userId);

            awardPointsForOrder(userId, orderId, productCount, orderAmount, shop);
        } catch (Exception e) {
            log.error("Error processing shop order rewards for shop {} in order {}: {}", shop.getShopId(), orderId,
                    e.getMessage(), e);
        }
    }

    private RewardSystem getActiveRewardSystemEntity(UUID shopId) {
        logDebugToFile("Getting active reward system for shop");
        return rewardSystemRepository.findByShopShopIdAndIsActiveTrue(shopId).orElse(null);
    }

    @Override
    public UserPointsDTO awardPointsForOrder(UUID userId, Long orderId, Integer productCount, BigDecimal orderAmount) {
        // Legacy method
        return null;
    }

    @Override
    public UserPointsDTO awardPointsForOrder(UUID userId, Long orderId, Integer productCount, BigDecimal orderAmount,
            Shop shop) {
        log.info("award points for the order");
        RewardSystem activeSystem = getActiveRewardSystemEntity(shop.getShopId());
        if (activeSystem == null) {
            // Should verify via system if needed, but if we are here via
            // checkRewardableForShopOrder, it's likely fine.
            // But calculation below depends on it? Actually we can re-calculate or just use
            // the system to get point value.
            return null;
        }

        Integer pointsEarned = activeSystem.calculatePurchasePoints(productCount, orderAmount);
        if (pointsEarned <= 0) {
            return null;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Calculate current balance for THIS shop
        Integer currentBalance = userPointsRepository.calculateCurrentBalanceByShop(userId, shop.getShopId());
        if (currentBalance == null) {
            currentBalance = 0;
        }
        Integer newBalance = currentBalance + pointsEarned;

        // Create audit trail record
        UserPoints userPoints = new UserPoints();
        userPoints.setUser(user);
        userPoints.setShop(shop);
        userPoints.setPoints(pointsEarned); // Positive for earning
        userPoints.setPointsType(UserPoints.PointsType.EARNED_PURCHASE);
        userPoints.setDescription("Points earned from order #" + orderId + " at " + shop.getName());
        userPoints.setOrderId(orderId);
        userPoints.setBalanceAfter(newBalance);
        userPoints.setCreatedAt(LocalDateTime.now());
        userPoints.setPointsValue(activeSystem.calculatePointsValue(pointsEarned));

        UserPoints saved = userPointsRepository.save(userPoints);

        log.info("Awarded {} points to user {} for order {} at shop {}. New shop balance: {}",
                pointsEarned, userId, orderId, shop.getName(), newBalance);

        return convertToDTO(saved);
    }

    @Override
    public UserPointsDTO awardPointsForReview(UUID userId, String description) {
        RewardSystem activeSystem = getActiveRewardSystemEntity();
        if (activeSystem == null || !activeSystem.getIsSystemEnabled() || !activeSystem.getIsReviewPointsEnabled()) {
            return null;
        }

        Integer reviewPoints = activeSystem.calculateReviewPoints();
        if (reviewPoints <= 0) {
            return null;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Calculate current balance from UserPoints records
        Integer currentBalance = userPointsRepository.calculateCurrentBalance(userId);
        if (currentBalance == null) {
            currentBalance = 0;
        }
        Integer newBalance = currentBalance + reviewPoints;

        // Create audit trail record
        UserPoints userPoints = new UserPoints();
        userPoints.setUser(user);
        userPoints.setPoints(reviewPoints); // Positive for earning
        userPoints.setPointsType(UserPoints.PointsType.EARNED_REVIEW);
        userPoints.setDescription(description != null ? description : "Points earned from product review");
        userPoints.setBalanceAfter(newBalance);
        userPoints.setCreatedAt(LocalDateTime.now());
        userPoints.setPointsValue(activeSystem.calculatePointsValue(reviewPoints));

        UserPoints saved = userPointsRepository.save(userPoints);

        return convertToDTO(saved);
    }

    @Override
    public UserPointsDTO deductPointsForPurchase(UUID userId, Integer points, String description) {
        if (!hasEnoughPoints(userId, points)) {
            Integer currentPoints = getUserCurrentPoints(userId);
            log.info("Required points are " + points + " but user has " + currentPoints);
            throw new RuntimeException("Insufficient points");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Calculate current balance from UserPoints records
        Integer currentBalance = userPointsRepository.calculateCurrentBalance(userId);
        if (currentBalance == null) {
            currentBalance = 0;
        }
        Integer newBalance = currentBalance - points;

        // Create audit trail record
        UserPoints userPoints = new UserPoints();
        userPoints.setUser(user);
        userPoints.setPoints(-points); // Negative for spending
        userPoints.setPointsType(UserPoints.PointsType.SPENT_PURCHASE);
        userPoints.setDescription(description != null ? description : "Points spent on product purchase");
        userPoints.setBalanceAfter(newBalance);
        userPoints.setCreatedAt(LocalDateTime.now());

        RewardSystem activeSystem = getActiveRewardSystemEntity();
        if (activeSystem != null) {
            userPoints.setPointsValue(activeSystem.calculatePointsValue(points));
        }

        UserPoints saved = userPointsRepository.save(userPoints);

        log.info("Deducted {} points from user {}. New balance: {}", points, userId, newBalance);

        return convertToDTO(saved);
    }

    @Override
    public UserPointsDTO refundPointsForCancelledOrder(UUID userId, Integer points, String description) {
        if (points == null || points <= 0) {
            log.warn("Invalid points amount for refund: {}", points);
            return null;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer currentBalance = userPointsRepository.calculateCurrentBalance(userId);
        if (currentBalance == null) {
            currentBalance = 0;
        }
        Integer newBalance = currentBalance + points;

        UserPoints userPoints = new UserPoints();
        userPoints.setUser(user);
        userPoints.setPoints(points);
        userPoints.setPointsType(UserPoints.PointsType.ADJUSTMENT);
        userPoints.setDescription(description != null ? description : "Points refunded for cancelled order");
        userPoints.setBalanceAfter(newBalance);
        userPoints.setCreatedAt(LocalDateTime.now());

        RewardSystem activeSystem = getActiveRewardSystemEntity();
        if (activeSystem != null) {
            userPoints.setPointsValue(activeSystem.calculatePointsValue(points));
        }

        UserPoints saved = userPointsRepository.save(userPoints);

        log.info("Refunded {} points to user {} for cancelled order. New balance: {}",
                points, userId, newBalance);

        return convertToDTO(saved);
    }

    @Override
    public Integer getUserCurrentPoints(UUID userId) {
        Integer balance = userPointsRepository.calculateCurrentBalance(userId);
        return balance != null ? balance : 0;
    }

    @Override
    public UserRewardSummaryDTO getUserRewardSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer currentPoints = getUserCurrentPoints(userId);
        Integer totalEarned = userPointsRepository.calculateTotalPointsEarned(userId);
        Integer totalSpent = userPointsRepository.calculateTotalPointsSpent(userId);

        RewardSystem activeSystem = getActiveRewardSystemEntity();
        BigDecimal pointValue = activeSystem != null ? activeSystem.getPointValue() : BigDecimal.ZERO;

        BigDecimal currentPointsValue = calculatePointsValue(currentPoints);
        BigDecimal totalValueEarned = calculatePointsValue(totalEarned);
        BigDecimal totalValueSpent = calculatePointsValue(totalSpent);

        UserRewardSummaryDTO summary = new UserRewardSummaryDTO();
        summary.setUserId(userId);
        summary.setUserFullName(user.getFullName());
        summary.setUserEmail(user.getUserEmail());
        summary.setCurrentPoints(currentPoints);
        summary.setCurrentPointsValue(currentPointsValue);
        summary.setTotalPointsEarned(totalEarned);
        summary.setTotalPointsSpent(totalSpent);
        summary.setTotalPointsExpired(0);
        summary.setTotalValueEarned(totalValueEarned);
        summary.setTotalValueSpent(totalValueSpent);
        summary.setPointValue(pointValue);

        return summary;
    }

    @Override
    public List<UserPointsDTO> getUserPointsHistory(UUID userId, int page, int size) {
        Page<UserPoints> pointsPage = userPointsRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));

        return pointsPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculatePointsValue(Integer points) {
        RewardSystem activeSystem = getActiveRewardSystemEntity();
        if (activeSystem == null || points == null || points <= 0) {
            return BigDecimal.ZERO;
        }
        return activeSystem.calculatePointsValue(points);
    }

    @Override
    public boolean hasEnoughPoints(UUID userId, Integer requiredPoints) {
        Integer currentBalance = userPointsRepository.calculateCurrentBalance(userId);
        return currentBalance != null && currentBalance >= requiredPoints;
    }

    @Override
    public Integer getPointsRequiredForProduct(BigDecimal productPrice) {
        RewardSystem activeSystem = getActiveRewardSystemEntity();
        if (activeSystem == null || productPrice == null) {
            return 0;
        }

        BigDecimal pointsValue = activeSystem.getPointValue();
        if (pointsValue.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return productPrice.divide(pointsValue, 0, java.math.RoundingMode.HALF_UP).intValue();
    }

    @Override
    public Integer getPreviewPointsForOrder(Integer productCount, BigDecimal orderAmount) {
        RewardSystem activeSystem = getActiveRewardSystemEntity();
        if (activeSystem == null || !activeSystem.getIsSystemEnabled() || !activeSystem.getIsPurchasePointsEnabled()) {
            return 0;
        }
        return activeSystem.calculatePurchasePoints(productCount, orderAmount);
    }

    private RewardSystem getActiveRewardSystemEntity() {
        return rewardSystemRepository.findByIsActiveTrue().orElse(null);
    }

    private void deactivateCurrentSystem(UUID shopId) {
        Optional<RewardSystem> currentActive = rewardSystemRepository.findByShopShopIdAndIsActiveTrue(shopId);
        if (currentActive.isPresent()) {
            RewardSystem active = currentActive.get();
            active.setIsActive(false);
            active.setUpdatedAt(LocalDateTime.now());
            rewardSystemRepository.save(active);
        }
    }

    private void updateRewardSystem(RewardSystem existing, RewardSystemDTO dto) {
        existing.setPointValue(dto.getPointValue());
        existing.setIsSystemEnabled(dto.getIsSystemEnabled());
        existing.setIsReviewPointsEnabled(dto.getIsReviewPointsEnabled());
        existing.setReviewPointsAmount(dto.getReviewPointsAmount());
        existing.setIsPurchasePointsEnabled(dto.getIsPurchasePointsEnabled());
        existing.setIsQuantityBasedEnabled(dto.getIsQuantityBasedEnabled());
        existing.setIsAmountBasedEnabled(dto.getIsAmountBasedEnabled());
        existing.setIsPercentageBasedEnabled(dto.getIsPercentageBasedEnabled());
        existing.setPercentageRate(dto.getPercentageRate());
        existing.setDescription(dto.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());

        if (existing.getRewardRanges() != null) {
            existing.getRewardRanges().clear();
        } else {
            existing.setRewardRanges(new java.util.ArrayList<>());
        }

        if (dto.getRewardRanges() != null && !dto.getRewardRanges().isEmpty()) {
            List<RewardRange> newRanges = dto.getRewardRanges().stream()
                    .map(rangeDTO -> convertRangeToEntity(rangeDTO, existing))
                    .collect(Collectors.toList());

            existing.getRewardRanges().addAll(newRanges);
        }
    }

    private RewardSystem convertToEntity(RewardSystemDTO dto, Shop shop) {
        RewardSystem entity = new RewardSystem();
        entity.setShop(shop);
        entity.setPointValue(dto.getPointValue());
        entity.setIsSystemEnabled(dto.getIsSystemEnabled());
        entity.setIsReviewPointsEnabled(dto.getIsReviewPointsEnabled());
        entity.setReviewPointsAmount(dto.getReviewPointsAmount());
        entity.setIsPurchasePointsEnabled(dto.getIsPurchasePointsEnabled());
        entity.setIsQuantityBasedEnabled(dto.getIsQuantityBasedEnabled());
        entity.setIsAmountBasedEnabled(dto.getIsAmountBasedEnabled());
        entity.setIsPercentageBasedEnabled(dto.getIsPercentageBasedEnabled());
        entity.setPercentageRate(dto.getPercentageRate());
        entity.setDescription(dto.getDescription());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private RewardSystemDTO convertToDTO(RewardSystem entity) {
        RewardSystemDTO dto = new RewardSystemDTO();
        dto.setId(entity.getId());
        if (entity.getShop() != null) {
            dto.setShopId(entity.getShop().getShopId());
            dto.setShopName(entity.getShop().getName());
        }
        dto.setPointValue(entity.getPointValue());
        dto.setIsActive(entity.getIsActive());
        dto.setIsSystemEnabled(entity.getIsSystemEnabled());
        dto.setIsReviewPointsEnabled(entity.getIsReviewPointsEnabled());
        dto.setReviewPointsAmount(entity.getReviewPointsAmount());
        dto.setIsPurchasePointsEnabled(entity.getIsPurchasePointsEnabled());
        dto.setIsQuantityBasedEnabled(entity.getIsQuantityBasedEnabled());
        dto.setIsAmountBasedEnabled(entity.getIsAmountBasedEnabled());
        dto.setIsPercentageBasedEnabled(entity.getIsPercentageBasedEnabled());
        dto.setPercentageRate(entity.getPercentageRate());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Convert reward ranges
        if (entity.getRewardRanges() != null && !entity.getRewardRanges().isEmpty()) {
            List<RewardRangeDTO> rangeDTOs = entity.getRewardRanges().stream()
                    .map(this::convertRangeToDTO)
                    .collect(Collectors.toList());
            dto.setRewardRanges(rangeDTOs);
        }

        return dto;
    }

    private UserPointsDTO convertToDTO(UserPoints entity) {
        UserPointsDTO dto = new UserPointsDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser().getId());
        dto.setUserFullName(entity.getUser().getFullName());
        dto.setUserEmail(entity.getUser().getUserEmail());
        dto.setPoints(entity.getPoints());
        dto.setPointsType(entity.getPointsType().name());
        dto.setDescription(entity.getDescription());
        dto.setOrderId(entity.getOrderId());
        dto.setPointsValue(entity.getPointsValue());
        dto.setBalanceAfter(entity.getBalanceAfter());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private RewardRangeDTO convertRangeToDTO(RewardRange range) {
        RewardRangeDTO dto = new RewardRangeDTO();
        dto.setId(range.getId());
        dto.setRewardSystemId(range.getRewardSystem().getId());
        dto.setRangeType(range.getRangeType().name());
        dto.setMinValue(range.getMinValue());
        dto.setMaxValue(range.getMaxValue());
        dto.setPoints(range.getPoints());
        dto.setDescription(range.getDescription());
        return dto;
    }

    private RewardRange convertRangeToEntity(RewardRangeDTO rangeDTO, RewardSystem rewardSystem) {
        log.info("Converting RewardRangeDTO to entity: rangeType={}, minValue={}, maxValue={}, points={}",
                rangeDTO.getRangeType(), rangeDTO.getMinValue(), rangeDTO.getMaxValue(), rangeDTO.getPoints());

        RewardRange range = new RewardRange();
        range.setRewardSystem(rewardSystem);
        range.setRangeType(RewardRange.RangeType.valueOf(rangeDTO.getRangeType()));
        range.setMinValue(rangeDTO.getMinValue());
        range.setMaxValue(rangeDTO.getMaxValue());
        range.setPoints(rangeDTO.getPoints());
        range.setDescription(rangeDTO.getDescription());

        log.info("Created RewardRange entity: rangeType={}, minValue={}, maxValue={}, points={}",
                range.getRangeType(), range.getMinValue(), range.getMaxValue(), range.getPoints());

        return range;
    }
}
