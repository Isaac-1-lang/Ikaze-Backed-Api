package com.ecommerce.controller;

import com.ecommerce.dto.RewardSystemPublicDTO;
import com.ecommerce.entity.RewardRange;
import com.ecommerce.entity.RewardSystem;
import com.ecommerce.repository.RewardSystemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Public REST Controller for reward system information
 */
@RestController
@RequestMapping("/api/v1/public/reward-system")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Reward System", description = "Public API for reward system information")
public class PublicRewardSystemController {

    private final RewardSystemRepository rewardSystemRepository;

    @GetMapping("/active")
    @Operation(summary = "Get active reward system", 
               description = "Retrieve the currently active reward system configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active reward system found"),
        @ApiResponse(responseCode = "404", description = "No active reward system found")
    })
    public ResponseEntity<RewardSystemPublicDTO> getActiveRewardSystem() {
        log.info("Fetching active reward system");
        
        RewardSystem rewardSystem = rewardSystemRepository.findByIsActiveTrue()
                .orElse(null);
        
        if (rewardSystem == null || !rewardSystem.getIsSystemEnabled()) {
            log.info("No active reward system found");
            return ResponseEntity.notFound().build();
        }
        
        RewardSystemPublicDTO dto = convertToDTO(rewardSystem);
        log.info("Active reward system found with ID: {}", rewardSystem.getId());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/status")
    @Operation(summary = "Check if reward system is active", 
               description = "Check if there is an active reward system available")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    })
    public ResponseEntity<Map<String, Boolean>> getRewardSystemStatus() {
        log.info("Checking reward system status");
        
        boolean isActive = rewardSystemRepository.findByIsActiveTrue()
                .map(rs -> rs.getIsSystemEnabled() && rs.getIsActive())
                .orElse(false);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("isActive", isActive);
        
        log.info("Reward system status: {}", isActive);
        return ResponseEntity.ok(response);
    }

    private RewardSystemPublicDTO convertToDTO(RewardSystem rewardSystem) {
        RewardSystemPublicDTO dto = new RewardSystemPublicDTO();
        dto.setId(rewardSystem.getId());
        dto.setPointValue(rewardSystem.getPointValue());
        dto.setIsActive(rewardSystem.getIsActive());
        dto.setIsReviewPointsEnabled(rewardSystem.getIsReviewPointsEnabled());
        dto.setReviewPointsAmount(rewardSystem.getReviewPointsAmount());
        dto.setIsSignupPointsEnabled(rewardSystem.getIsSignupPointsEnabled());
        dto.setSignupPointsAmount(rewardSystem.getSignupPointsAmount());
        dto.setIsPurchasePointsEnabled(rewardSystem.getIsPurchasePointsEnabled());
        dto.setIsQuantityBasedEnabled(rewardSystem.getIsQuantityBasedEnabled());
        dto.setIsAmountBasedEnabled(rewardSystem.getIsAmountBasedEnabled());
        dto.setIsPercentageBasedEnabled(rewardSystem.getIsPercentageBasedEnabled());
        dto.setPercentageRate(rewardSystem.getPercentageRate());
        dto.setDescription(rewardSystem.getDescription());
        
        if (rewardSystem.getRewardRanges() != null) {
            List<RewardSystemPublicDTO.RewardRangeDTO> rangeDTOs = rewardSystem.getRewardRanges().stream()
                    .map(this::convertRangeToDTO)
                    .collect(Collectors.toList());
            dto.setRewardRanges(rangeDTOs);
        }
        
        return dto;
    }

    private RewardSystemPublicDTO.RewardRangeDTO convertRangeToDTO(RewardRange range) {
        RewardSystemPublicDTO.RewardRangeDTO dto = new RewardSystemPublicDTO.RewardRangeDTO();
        dto.setId(range.getId());
        dto.setRangeType(range.getRangeType().name());
        dto.setMinValue(range.getMinValue());
        dto.setMaxValue(range.getMaxValue());
        dto.setPoints(range.getPoints());
        dto.setDescription(range.getDescription());
        return dto;
    }
}
