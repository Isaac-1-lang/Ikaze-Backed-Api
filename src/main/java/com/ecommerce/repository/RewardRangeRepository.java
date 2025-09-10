package com.ecommerce.repository;

import com.ecommerce.entity.RewardRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRangeRepository extends JpaRepository<RewardRange, Long> {

    List<RewardRange> findByRewardSystemId(Long rewardSystemId);

    List<RewardRange> findByRewardSystemIdAndRangeType(Long rewardSystemId, RewardRange.RangeType rangeType);

    void deleteByRewardSystemId(Long rewardSystemId);
}
