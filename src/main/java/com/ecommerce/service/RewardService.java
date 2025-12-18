package com.ecommerce.service;

import com.ecommerce.dto.RewardSystemDTO;
import com.ecommerce.dto.UserPointsDTO;
import com.ecommerce.dto.UserRewardSummaryDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RewardService {

    RewardSystemDTO getActiveRewardSystem(UUID shopId);
    
    /**
     * @deprecated Use getActiveRewardSystem(UUID shopId) instead. This method is kept for backward compatibility.
     */
    @Deprecated
    RewardSystemDTO getActiveRewardSystem();

    Map<String, Object> getAllRewardSystems(UUID shopId, int page, int size, String sortBy, String sortDir);

    RewardSystemDTO getRewardSystemById(Long id, UUID shopId);

    RewardSystemDTO saveRewardSystem(RewardSystemDTO rewardSystemDTO, UUID shopId);

    RewardSystemDTO activateRewardSystem(Long rewardSystemId, UUID shopId);

    RewardSystemDTO toggleSystemEnabled(Long rewardSystemId, UUID shopId, Boolean enabled);

    RewardSystemDTO toggleReviewPoints(Long rewardSystemId, UUID shopId, Boolean enabled, Integer pointsAmount);

    RewardSystemDTO togglePurchasePoints(Long rewardSystemId, UUID shopId, Boolean enabled);

    RewardSystemDTO toggleQuantityBased(Long rewardSystemId, UUID shopId, Boolean enabled);

    RewardSystemDTO toggleAmountBased(Long rewardSystemId, UUID shopId, Boolean enabled);

    RewardSystemDTO togglePercentageBased(Long rewardSystemId, UUID shopId, Boolean enabled, BigDecimal percentageRate);

    Integer calculateOrderPoints(Integer productCount, BigDecimal orderAmount);

    void checkRewardableOnOrderAndReward(UUID userId, Long orderId, Integer productCount, BigDecimal orderAmount);

    UserPointsDTO awardPointsForOrder(UUID userId, Long orderId, Integer productCount, BigDecimal orderAmount);

    UserPointsDTO awardPointsForReview(UUID userId, String description);

    UserPointsDTO deductPointsForPurchase(UUID userId, Integer points, String description);

    UserPointsDTO refundPointsForCancelledOrder(UUID userId, Integer points, String description);

    Integer getUserCurrentPoints(UUID userId);

    UserRewardSummaryDTO getUserRewardSummary(UUID userId);

    List<UserPointsDTO> getUserPointsHistory(UUID userId, int page, int size);

    BigDecimal calculatePointsValue(Integer points);

    boolean hasEnoughPoints(UUID userId, Integer requiredPoints);

    Integer getPointsRequiredForProduct(BigDecimal productPrice);

    Integer getPreviewPointsForOrder(Integer productCount, BigDecimal orderAmount);
}
