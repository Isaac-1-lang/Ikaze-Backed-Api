package com.ecommerce.repository;

import com.ecommerce.entity.Discount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, UUID> {

        Optional<Discount> findByDiscountCode(String discountCode);

        List<Discount> findByIsActiveTrue();

        Page<Discount> findByIsActiveTrue(Pageable pageable);

        @Query("SELECT d FROM Discount d WHERE d.isActive = true AND " +
                        "d.startDate <= :now AND (d.endDate IS NULL OR d.endDate >= :now) AND " +
                        "(d.usageLimit IS NULL OR d.usedCount < d.usageLimit)")
        List<Discount> findValidDiscounts(@Param("now") LocalDateTime now);

        @Query("SELECT d FROM Discount d WHERE d.discountCode = :code AND d.isActive = true AND " +
                        "d.startDate <= :now AND (d.endDate IS NULL OR d.endDate >= :now) AND " +
                        "(d.usageLimit IS NULL OR d.usedCount < d.usageLimit)")
        Optional<Discount> findValidDiscountByCode(@Param("code") String code, @Param("now") LocalDateTime now);

        @Query("SELECT d FROM Discount d WHERE CAST(d.discountId AS string) = :discountId")
        Optional<Discount> findByDiscountId(@Param("discountId") String discountId);

        @Query("SELECT d FROM Discount d WHERE d.isActive = true AND d.startDate <= :now AND (d.endDate IS NULL OR d.endDate >= :now)")
        List<Discount> findActiveAndValidDiscounts(@Param("now") LocalDateTime now);
}
