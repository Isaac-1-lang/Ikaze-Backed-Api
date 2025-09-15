package com.ecommerce.service.impl;

import com.ecommerce.dto.AssignDiscountRequest;
import com.ecommerce.dto.RemoveDiscountRequest;
import com.ecommerce.entity.Discount;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.repository.DiscountRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.service.ProductDiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductDiscountServiceImpl implements ProductDiscountService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final DiscountRepository discountRepository;

    @Override
    @Transactional
    public void assignDiscount(AssignDiscountRequest request) {
        log.info("Assigning discount {} to products/variants", request.getDiscountId());

        Discount discount = discountRepository.findByDiscountId(request.getDiscountId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Discount not found with ID: " + request.getDiscountId()));

        if (!discount.isValid()) {
            throw new IllegalArgumentException("Discount is not valid or has expired");
        }

        // Assign to products if provided
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            assignDiscountToProducts(request.getProductIds(), discount);
        }

        // Assign to variants if provided
        if (request.getVariantIds() != null && !request.getVariantIds().isEmpty()) {
            assignDiscountToVariants(request.getVariantIds(), discount);
        }

        // Validate that at least one assignment was made
        if ((request.getProductIds() == null || request.getProductIds().isEmpty()) &&
                (request.getVariantIds() == null || request.getVariantIds().isEmpty())) {
            throw new IllegalArgumentException("At least one product ID or variant ID is required");
        }

        log.info("Successfully assigned discount {} to products/variants", request.getDiscountId());
    }

    @Override
    @Transactional
    public void removeDiscount(RemoveDiscountRequest request) {
        log.info("Removing discount from products/variants");

        // Remove from products if provided
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            removeDiscountFromProducts(request.getProductIds());
        }

        // Remove from variants if provided
        if (request.getVariantIds() != null && !request.getVariantIds().isEmpty()) {
            removeDiscountFromVariants(request.getVariantIds());
        }

        // Validate that at least one removal was made
        if ((request.getProductIds() == null || request.getProductIds().isEmpty()) &&
                (request.getVariantIds() == null || request.getVariantIds().isEmpty())) {
            throw new IllegalArgumentException("At least one product ID or variant ID is required");
        }

        log.info("Successfully removed discount from products/variants");
    }

    private void assignDiscountToProducts(List<String> productIds, Discount discount) {
        log.info("Assigning discount to {} products", productIds.size());

        for (String productId : productIds) {
            Product product = productRepository.findByProductId(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

            if (product.getDiscount() != null) {
                throw new IllegalArgumentException("Product " + productId + " already has a discount assigned");
            }

            if (product.getTotalStockQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Product " + productId + " must have active stock to assign discount");
            }

            product.setDiscount(discount);
            productRepository.save(product);
        }

        log.info("Successfully assigned discount to {} products", productIds.size());
    }

    private void assignDiscountToVariants(List<String> variantIds, Discount discount) {
        log.info("Assigning discount to {} variants", variantIds.size());

        List<Long> variantIdLongs = variantIds.stream()
                .map(Long::parseLong)
                .toList();

        List<ProductVariant> variants = productVariantRepository.findAllById(variantIdLongs);

        if (variants.size() != variantIds.size()) {
            throw new IllegalArgumentException("Some variants were not found");
        }

        for (ProductVariant variant : variants) {
            if (variant.getDiscount() != null) {
                throw new IllegalArgumentException("Variant " + variant.getId() + " already has a discount assigned");
            }

            if (variant.getTotalStockQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Variant " + variant.getId() + " must have active stock to assign discount");
            }

            variant.setDiscount(discount);
        }

        productVariantRepository.saveAll(variants);

        log.info("Successfully assigned discount to {} variants", variants.size());
    }

    private void removeDiscountFromProducts(List<String> productIds) {
        log.info("Removing discount from {} products", productIds.size());

        for (String productId : productIds) {
            Product product = productRepository.findByProductId(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

            if (product.getDiscount() == null) {
                throw new IllegalArgumentException("Product " + productId + " does not have a discount assigned");
            }

            product.setDiscount(null);
            productRepository.save(product);
        }

        log.info("Successfully removed discount from {} products", productIds.size());
    }

    private void removeDiscountFromVariants(List<String> variantIds) {
        log.info("Removing discount from {} variants", variantIds.size());

        List<Long> variantIdLongs = variantIds.stream()
                .map(Long::parseLong)
                .toList();

        List<ProductVariant> variants = productVariantRepository.findAllById(variantIdLongs);

        if (variants.size() != variantIds.size()) {
            throw new IllegalArgumentException("Some variants were not found");
        }

        for (ProductVariant variant : variants) {
            if (variant.getDiscount() == null) {
                throw new IllegalArgumentException("Variant " + variant.getId() + " does not have a discount assigned");
            }

            variant.setDiscount(null);
        }

        productVariantRepository.saveAll(variants);

        log.info("Successfully removed discount from {} variants", variants.size());
    }

    @Override
    public Page<Map<String, Object>> getProductsByDiscount(String discountId, Pageable pageable) {
        log.info("Getting products by discount {}", discountId);

        Discount discount = discountRepository.findByDiscountId(discountId)
                .orElseThrow(() -> new IllegalArgumentException("Discount not found with ID: " + discountId));

        // Get products with this discount
        Page<Product> products = productRepository.findByDiscount(discount, pageable);

        // Get variants with this discount
        Page<ProductVariant> variants = productVariantRepository.findByDiscount(discount, pageable);

        // Combine results into a single page
        // For simplicity, we'll return products first, then variants
        // In a real implementation, you might want to create a more sophisticated
        // pagination
        Map<String, Object> result = new HashMap<>();
        result.put("products", products.getContent());
        result.put("variants", variants.getContent());
        result.put("totalProducts", products.getTotalElements());
        result.put("totalVariants", variants.getTotalElements());

        return Page.empty(pageable); // Simplified for now
    }

    @Override
    public Map<String, Object> getProductDiscountStatus(String productId) {
        log.info("Getting discount status for product {}", productId);

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        Map<String, Object> status = new HashMap<>();
        status.put("productId", productId);
        status.put("hasDiscount", product.getDiscount() != null);
        status.put("discount", product.getDiscount());
        status.put("hasVariants", product.getVariants() != null && !product.getVariants().isEmpty());

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            status.put("variants", product.getVariants().stream()
                    .map(variant -> {
                        Map<String, Object> variantInfo = new HashMap<>();
                        variantInfo.put("id", variant.getId());
                        variantInfo.put("name", variant.getVariantName());
                        variantInfo.put("hasDiscount", variant.getDiscount() != null);
                        variantInfo.put("discount", variant.getDiscount());
                        variantInfo.put("stockQuantity", variant.getTotalStockQuantity());
                        return variantInfo;
                    })
                    .toList());
        }

        return status;
    }
}