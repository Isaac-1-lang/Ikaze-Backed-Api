package com.ecommerce.service.impl;

import com.ecommerce.dto.CreateDiscountDTO;
import com.ecommerce.dto.DiscountDTO;
import com.ecommerce.dto.UpdateDiscountDTO;
import com.ecommerce.entity.Discount;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.Shop;
import com.ecommerce.repository.DiscountRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.service.DiscountService;
import com.ecommerce.service.ShopAuthorizationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ShopRepository shopRepository;
    private final ShopAuthorizationService shopAuthorizationService;

    @Override
    public DiscountDTO createDiscount(UUID vendorId, CreateDiscountDTO createDiscountDTO) {
        log.info("Creating new discount: {} for shop: {}", createDiscountDTO.getName(), createDiscountDTO.getShopId());

        if (createDiscountDTO.getShopId() == null) {
            throw new IllegalArgumentException("shopId is required");
        }

        // Verify shop exists and user has access
        Shop shop = shopRepository.findById(createDiscountDTO.getShopId())
                .orElseThrow(() -> new EntityNotFoundException("Shop not found with ID: " + createDiscountDTO.getShopId()));

        shopAuthorizationService.assertCanManageShop(vendorId, createDiscountDTO.getShopId());

        // Validate discount code uniqueness within the shop if provided
        if (createDiscountDTO.getDiscountCode() != null && !createDiscountDTO.getDiscountCode().trim().isEmpty()) {
            if (discountRepository.findByDiscountCodeAndShopShopId(createDiscountDTO.getDiscountCode(), createDiscountDTO.getShopId()).isPresent()) {
                throw new IllegalArgumentException(
                        "Discount code already exists in this shop: " + createDiscountDTO.getDiscountCode());
            }
        }

        // Validate date range
        if (createDiscountDTO.getEndDate() != null &&
                createDiscountDTO.getStartDate().isAfter(createDiscountDTO.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
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
        discount.setShop(shop);

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
    public DiscountDTO updateDiscount(UUID discountId, UUID vendorId, UpdateDiscountDTO updateDiscountDTO) {
        log.info("Updating discount with ID: {}", discountId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with ID: " + discountId));

        // Verify user has access to the shop
        if (discount.getShop() == null) {
            throw new IllegalStateException("Discount is not associated with a shop");
        }
        shopAuthorizationService.assertCanManageShop(vendorId, discount.getShop().getShopId());

        // Validate discount code uniqueness within the shop if being updated
        if (updateDiscountDTO.getDiscountCode() != null && !updateDiscountDTO.getDiscountCode().trim().isEmpty()) {
            discountRepository.findByDiscountCodeAndShopShopId(updateDiscountDTO.getDiscountCode(), discount.getShop().getShopId())
                    .filter(existing -> !existing.getDiscountId().equals(discountId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "Discount code already exists in this shop: " + updateDiscountDTO.getDiscountCode());
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

        Discount updatedDiscount = discountRepository.save(discount);
        log.info("Discount updated successfully with ID: {}", updatedDiscount.getDiscountId());

        return mapToDTO(updatedDiscount);
    }

    @Override
    public boolean deleteDiscount(UUID discountId, UUID vendorId) {
        log.info("Deleting discount with ID: {}", discountId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with ID: " + discountId));

        // Verify user has access to the shop
        if (discount.getShop() == null) {
            throw new IllegalStateException("Discount is not associated with a shop");
        }
        shopAuthorizationService.assertCanManageShop(vendorId, discount.getShop().getShopId());

        List<Product> productsWithDiscount = productRepository.findByDiscount(discount, Pageable.unpaged()).getContent();
        if (!productsWithDiscount.isEmpty()) {
            log.info("Removing discount from {} products", productsWithDiscount.size());
            for (Product product : productsWithDiscount) {
                product.setDiscount(null);
            }
            productRepository.saveAll(productsWithDiscount);
            log.info("Successfully removed discount from {} products", productsWithDiscount.size());
        }

        List<ProductVariant> variantsWithDiscount = productVariantRepository.findByDiscount(discount, Pageable.unpaged()).getContent();
        if (!variantsWithDiscount.isEmpty()) {
            log.info("Removing discount from {} product variants", variantsWithDiscount.size());
            for (ProductVariant variant : variantsWithDiscount) {
                variant.setDiscount(null);
            }
            productVariantRepository.saveAll(variantsWithDiscount);
            log.info("Successfully removed discount from {} product variants", variantsWithDiscount.size());
        }

        discountRepository.deleteById(discountId);
        log.info("Discount deleted successfully with ID: {} (removed from {} products and {} variants)", 
                discountId, productsWithDiscount.size(), variantsWithDiscount.size());

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountDTO getDiscountById(UUID discountId, UUID shopId) {
        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with ID: " + discountId));

        // Verify discount belongs to the shop
        if (discount.getShop() == null || !discount.getShop().getShopId().equals(shopId)) {
            throw new EntityNotFoundException("Discount not found with ID: " + discountId + " for shop: " + shopId);
        }

        return mapToDTO(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscountDTO> getAllDiscounts(UUID shopId, Pageable pageable) {
        log.info("Fetching all discounts for shop: {} with pagination", shopId);

        Page<Discount> discounts = discountRepository.findByShopShopId(shopId, pageable);
        return discounts.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscountDTO> getActiveDiscounts(UUID shopId, Pageable pageable) {
        log.info("Fetching active discounts for shop: {} with pagination", shopId);

        Page<Discount> discounts = discountRepository.findByShopShopIdAndIsActiveTrue(shopId, pageable);
        return discounts.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountDTO getDiscountByCode(String discountCode, UUID shopId) {
        log.info("Fetching discount by code: {} for shop: {}", discountCode, shopId);

        Discount discount = discountRepository.findByDiscountCodeAndShopShopId(discountCode, shopId)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with code: " + discountCode + " for shop: " + shopId));

        return mapToDTO(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDiscountValid(UUID discountId, UUID shopId) {
        log.info("Checking if discount is valid with ID: {} for shop: {}", discountId, shopId);

        return discountRepository.findById(discountId)
                .filter(d -> d.getShop() != null && d.getShop().getShopId().equals(shopId))
                .map(Discount::isValid)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDiscountCodeValid(String discountCode, UUID shopId) {
        log.info("Checking if discount code is valid: {} for shop: {}", discountCode, shopId);

        return discountRepository.findValidDiscountByCodeAndShop(discountCode, shopId, LocalDateTime.now())
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
                .active(discount.isActive())
                .usageLimit(discount.getUsageLimit())
                .usedCount(discount.getUsedCount())
                .discountType(discount.getDiscountType().name())
                .createdAt(discount.getCreatedAt())
                .updatedAt(discount.getUpdatedAt())
                .valid(discount.isValid())
                .canBeUsed(discount.canBeUsed())
                .shopId(discount.getShop() != null ? discount.getShop().getShopId() : null)
                .shopName(discount.getShop() != null ? discount.getShop().getName() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Discount> getAllDiscountEntities() {
        log.info("Fetching all discount entities");
        return discountRepository.findAll();
    }

    @Override
    @Transactional
    public void saveAllDiscounts(List<Discount> discounts) {
        log.info("Saving {} discount entities", discounts.size());
        discountRepository.saveAll(discounts);
    }
}
