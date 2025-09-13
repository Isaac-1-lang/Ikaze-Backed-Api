package com.ecommerce.controller;

import com.ecommerce.dto.CreateDiscountDTO;
import com.ecommerce.dto.DiscountDTO;
import com.ecommerce.dto.UpdateDiscountDTO;
import com.ecommerce.entity.Discount;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.repository.DiscountRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.service.DiscountService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
@Slf4j
public class DiscountController {

    private final DiscountService discountService;
    private final DiscountRepository discountRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<DiscountDTO> createDiscount(@Valid @RequestBody CreateDiscountDTO createDiscountDTO) {
        log.info("Creating new discount: {}", createDiscountDTO.getName());

        try {
            DiscountDTO createdDiscount = discountService.createDiscount(createDiscountDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDiscount);
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for discount creation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating discount: {}", e.getMessage());
            throw e;
        }
    }

    @PutMapping("/{discountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<DiscountDTO> updateDiscount(@PathVariable UUID discountId,
            @Valid @RequestBody UpdateDiscountDTO updateDiscountDTO) {
        log.info("Updating discount with ID: {}", discountId);

        try {
            DiscountDTO updatedDiscount = discountService.updateDiscount(discountId, updateDiscountDTO);
            return ResponseEntity.ok(updatedDiscount);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with ID: {}", discountId);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for discount update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating discount: {}", e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{discountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Map<String, String>> deleteDiscount(@PathVariable UUID discountId) {
        log.info("Deleting discount with ID: {}", discountId);

        try {
            boolean deleted = discountService.deleteDiscount(discountId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Discount deleted successfully");
            response.put("discountId", discountId.toString());
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with ID: {}", discountId);
            throw e;
        } catch (Exception e) {
            log.error("Error deleting discount: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{discountId}")
    public ResponseEntity<DiscountDTO> getDiscountById(@PathVariable UUID discountId) {
        log.info("Fetching discount with ID: {}", discountId);

        try {
            DiscountDTO discount = discountService.getDiscountById(discountId);
            return ResponseEntity.ok(discount);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with ID: {}", discountId);
            throw e;
        } catch (Exception e) {
            log.error("Error fetching discount: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<Page<DiscountDTO>> getAllDiscounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        log.info("Fetching discounts - page: {}, size: {}, sortBy: {}, sortDirection: {}, activeOnly: {}",
                page, size, sortBy, sortDirection, activeOnly);

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<DiscountDTO> discounts;
            if (activeOnly) {
                discounts = discountService.getActiveDiscounts(pageable);
            } else {
                discounts = discountService.getAllDiscounts(pageable);
            }

            return ResponseEntity.ok(discounts);
        } catch (Exception e) {
            log.error("Error fetching discounts: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/code/{discountCode}")
    public ResponseEntity<DiscountDTO> getDiscountByCode(@PathVariable String discountCode) {
        log.info("Fetching discount by code: {}", discountCode);

        try {
            DiscountDTO discount = discountService.getDiscountByCode(discountCode);
            return ResponseEntity.ok(discount);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with code: {}", discountCode);
            throw e;
        } catch (Exception e) {
            log.error("Error fetching discount by code: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{discountId}/valid")
    public ResponseEntity<Map<String, Object>> isDiscountValid(@PathVariable UUID discountId) {
        log.info("Checking if discount is valid with ID: {}", discountId);

        try {
            boolean isValid = discountService.isDiscountValid(discountId);
            Map<String, Object> response = new HashMap<>();
            response.put("discountId", discountId.toString());
            response.put("isValid", isValid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking discount validity: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/code/{discountCode}/valid")
    public ResponseEntity<Map<String, Object>> isDiscountCodeValid(@PathVariable String discountCode) {
        log.info("Checking if discount code is valid: {}", discountCode);

        try {
            boolean isValid = discountService.isDiscountCodeValid(discountCode);
            Map<String, Object> response = new HashMap<>();
            response.put("discountCode", discountCode);
            response.put("isValid", isValid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking discount code validity: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/fix-dates")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Map<String, String>> fixDiscountDates() {
        log.info("Fixing discount dates to 2024");

        try {
            // Get all discounts and update their dates to 2024
            List<Discount> discounts = discountService.getAllDiscountEntities();
            int updatedCount = 0;

            for (Discount discount : discounts) {
                LocalDateTime startDate = discount.getStartDate();
                LocalDateTime endDate = discount.getEndDate();

                if (startDate != null && startDate.getYear() == 2025) {
                    discount.setStartDate(startDate.withYear(2024));
                    updatedCount++;
                }

                if (endDate != null && endDate.getYear() == 2025) {
                    discount.setEndDate(endDate.withYear(2024));
                }
            }

            // Save all updated discounts
            discountService.saveAllDiscounts(discounts);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Updated " + updatedCount + " discount dates to 2024");
            response.put("updatedCount", String.valueOf(updatedCount));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fixing discount dates: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{discountId}/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getProductsByDiscount(@PathVariable String discountId) {
        try {
            log.info("Fetching products for discount: {}", discountId);

            Discount discount = discountRepository.findByDiscountId(discountId)
                    .orElseThrow(() -> new EntityNotFoundException("Discount not found with ID: " + discountId));

            List<Product> products = productRepository.findByDiscount(discount, Pageable.unpaged()).getContent();
            List<ProductVariant> variants = productVariantRepository.findByDiscount(discount, Pageable.unpaged())
                    .getContent();

            log.info("Found {} products and {} variants for discount {}",
                    products.size(), variants.size(), discountId);

            Map<String, Object> response = new HashMap<>();
            response.put("products", products.stream().map(this::mapProductToDTO).collect(Collectors.toList()));
            response.put("variants", variants.stream().map(this::mapVariantToDTO).collect(Collectors.toList()));
            response.put("totalProducts", products.size());
            response.put("totalVariants", variants.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching products for discount {}: {}", discountId, e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> mapProductToDTO(Product product) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("productId", product.getProductId().toString());
        dto.put("name", product.getProductName());
        dto.put("price", product.getPrice());
        dto.put("discountedPrice", product.getDiscountedPrice());
        dto.put("hasVariants", product.hasVariants());
        dto.put("sku", product.getSku());
        dto.put("isActive", product.isActive());
        dto.put("imageUrl", product.getMainImageUrl());
        return dto;
    }

    private Map<String, Object> mapVariantToDTO(ProductVariant variant) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("variantId", variant.getId().toString());
        dto.put("variantName", variant.getVariantName());
        dto.put("variantSku", variant.getVariantSku());
        dto.put("price", variant.getPrice());
        dto.put("discountedPrice", variant.getDiscountedPrice());
        dto.put("productId", variant.getProduct().getProductId().toString());
        dto.put("productName", variant.getProduct().getProductName());
        dto.put("isActive", variant.isActive());
        dto.put("imageUrl", getVariantMainImageUrl(variant));

        // Debug logging
        log.info("Variant {} - Original price: {}, Discounted price: {}, Has discount: {}",
                variant.getId(), variant.getPrice(), variant.getDiscountedPrice(),
                variant.getDiscount() != null);

        return dto;
    }

    private String getVariantMainImageUrl(ProductVariant variant) {
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            return variant.getImages().stream()
                    .filter(img -> img.isPrimary())
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(variant.getImages().get(0).getImageUrl());
        }
        // Fallback to product's main image if variant has no images
        return variant.getProduct().getMainImageUrl();
    }
}
