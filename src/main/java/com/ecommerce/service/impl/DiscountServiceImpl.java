package com.ecommerce.service.impl;

import com.ecommerce.dto.CreateDiscountDTO;
import com.ecommerce.dto.DiscountDTO;
import com.ecommerce.dto.UpdateDiscountDTO;
import com.ecommerce.entity.Discount;
import com.ecommerce.repository.DiscountRepository;
import com.ecommerce.service.DiscountService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;

    @Override
    public DiscountDTO createDiscount(CreateDiscountDTO createDiscountDTO) {
        log.info("Creating new discount: {}", createDiscountDTO.getName());

        // Validate discount code uniqueness if provided
        if (createDiscountDTO.getDiscountCode() != null && !createDiscountDTO.getDiscountCode().trim().isEmpty()) {
            if (discountRepository.findByDiscountCode(createDiscountDTO.getDiscountCode()).isPresent()) {
                throw new IllegalArgumentException(
                        "Discount code already exists: " + createDiscountDTO.getDiscountCode());
            }
        }

        // Validate date range
        if (createDiscountDTO.getEndDate() != null &&
                createDiscountDTO.getStartDate().isAfter(createDiscountDTO.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        // Validate amount range
        if (createDiscountDTO.getMinimumAmount() != null && createDiscountDTO.getMaximumAmount() != null) {
            if (createDiscountDTO.getMinimumAmount().compareTo(createDiscountDTO.getMaximumAmount()) > 0) {
                throw new IllegalArgumentException("Minimum amount cannot be greater than maximum amount");
            }
        }

        Discount discount = new Discount();
        discount.setName(createDiscountDTO.getName());
        discount.setDescription(createDiscountDTO.getDescription());
        discount.setPercentage(createDiscountDTO.getPercentage());
        discount.setDiscountCode(createDiscountDTO.getDiscountCode());
        discount.setStartDate(createDiscountDTO.getStartDate());
        discount.setEndDate(createDiscountDTO.getEndDate());
        discount.setActive(createDiscountDTO.isActive());
        discount.setUsageLimit(createDiscountDTO.getUsageLimit());
        discount.setMinimumAmount(createDiscountDTO.getMinimumAmount());
        discount.setMaximumAmount(createDiscountDTO.getMaximumAmount());

        // Set discount type
        if (createDiscountDTO.getDiscountType() != null) {
            try {
                discount.setDiscountType(Discount.DiscountType.valueOf(createDiscountDTO.getDiscountType()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid discount type: " + createDiscountDTO.getDiscountType());
            }
        }

        Discount savedDiscount = discountRepository.save(discount);
        log.info("Discount created successfully with ID: {}", savedDiscount.getDiscountId());

        return mapToDTO(savedDiscount);
    }

    @Override
    public DiscountDTO updateDiscount(UUID discountId, UpdateDiscountDTO updateDiscountDTO) {
        log.info("Updating discount with ID: {}", discountId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with ID: " + discountId));

        // Validate discount code uniqueness if being updated
        if (updateDiscountDTO.getDiscountCode() != null && !updateDiscountDTO.getDiscountCode().trim().isEmpty()) {
            discountRepository.findByDiscountCode(updateDiscountDTO.getDiscountCode())
                    .filter(existing -> !existing.getDiscountId().equals(discountId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "Discount code already exists: " + updateDiscountDTO.getDiscountCode());
                    });
        }

        // Update fields if provided
        if (updateDiscountDTO.getName() != null) {
            discount.setName(updateDiscountDTO.getName());
        }
        if (updateDiscountDTO.getDescription() != null) {
            discount.setDescription(updateDiscountDTO.getDescription());
        }
        if (updateDiscountDTO.getPercentage() != null) {
            discount.setPercentage(updateDiscountDTO.getPercentage());
        }
        if (updateDiscountDTO.getDiscountCode() != null) {
            discount.setDiscountCode(updateDiscountDTO.getDiscountCode());
        }
        if (updateDiscountDTO.getStartDate() != null) {
            discount.setStartDate(updateDiscountDTO.getStartDate());
        }
        if (updateDiscountDTO.getEndDate() != null) {
            discount.setEndDate(updateDiscountDTO.getEndDate());
        }
        if (updateDiscountDTO.getIsActive() != null) {
            discount.setActive(updateDiscountDTO.getIsActive());
        }
        if (updateDiscountDTO.getUsageLimit() != null) {
            discount.setUsageLimit(updateDiscountDTO.getUsageLimit());
        }
        if (updateDiscountDTO.getMinimumAmount() != null) {
            discount.setMinimumAmount(updateDiscountDTO.getMinimumAmount());
        }
        if (updateDiscountDTO.getMaximumAmount() != null) {
            discount.setMaximumAmount(updateDiscountDTO.getMaximumAmount());
        }
        if (updateDiscountDTO.getDiscountType() != null) {
            try {
                discount.setDiscountType(Discount.DiscountType.valueOf(updateDiscountDTO.getDiscountType()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid discount type: " + updateDiscountDTO.getDiscountType());
            }
        }

        // Validate date range
        if (discount.getEndDate() != null && discount.getStartDate().isAfter(discount.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        // Validate amount range
        if (discount.getMinimumAmount() != null && discount.getMaximumAmount() != null) {
            if (discount.getMinimumAmount().compareTo(discount.getMaximumAmount()) > 0) {
                throw new IllegalArgumentException("Minimum amount cannot be greater than maximum amount");
            }
        }

        Discount updatedDiscount = discountRepository.save(discount);
        log.info("Discount updated successfully with ID: {}", updatedDiscount.getDiscountId());

        return mapToDTO(updatedDiscount);
    }

    @Override
    public boolean deleteDiscount(UUID discountId) {
        log.info("Deleting discount with ID: {}", discountId);

        if (!discountRepository.existsById(discountId)) {
            throw new EntityNotFoundException("Discount not found with ID: " + discountId);
        }

        discountRepository.deleteById(discountId);
        log.info("Discount deleted successfully with ID: {}", discountId);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountDTO getDiscountById(UUID discountId) {
        log.info("Fetching discount with ID: {}", discountId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with ID: " + discountId));

        return mapToDTO(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscountDTO> getAllDiscounts(Pageable pageable) {
        log.info("Fetching all discounts with pagination");

        Page<Discount> discounts = discountRepository.findAll(pageable);
        return discounts.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscountDTO> getActiveDiscounts(Pageable pageable) {
        log.info("Fetching active discounts with pagination");

        Page<Discount> discounts = discountRepository.findByIsActiveTrue(pageable);
        return discounts.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountDTO getDiscountByCode(String discountCode) {
        log.info("Fetching discount by code: {}", discountCode);

        Discount discount = discountRepository.findByDiscountCode(discountCode)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with code: " + discountCode));

        return mapToDTO(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDiscountValid(UUID discountId) {
        log.info("Checking if discount is valid with ID: {}", discountId);

        return discountRepository.findById(discountId)
                .map(Discount::isValid)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDiscountCodeValid(String discountCode) {
        log.info("Checking if discount code is valid: {}", discountCode);

        return discountRepository.findValidDiscountByCode(discountCode, LocalDateTime.now())
                .isPresent();
    }

    private DiscountDTO mapToDTO(Discount discount) {
        return DiscountDTO.builder()
                .discountId(discount.getDiscountId())
                .name(discount.getName())
                .description(discount.getDescription())
                .percentage(discount.getPercentage())
                .discountCode(discount.getDiscountCode())
                .startDate(discount.getStartDate())
                .endDate(discount.getEndDate())
                .isActive(discount.isActive())
                .usageLimit(discount.getUsageLimit())
                .usedCount(discount.getUsedCount())
                .minimumAmount(discount.getMinimumAmount())
                .maximumAmount(discount.getMaximumAmount())
                .discountType(discount.getDiscountType().name())
                .createdAt(discount.getCreatedAt())
                .updatedAt(discount.getUpdatedAt())
                .isValid(discount.isValid())
                .canBeUsed(discount.canBeUsed())
                .build();
    }
}
