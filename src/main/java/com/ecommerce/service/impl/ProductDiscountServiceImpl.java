package com.ecommerce.service.impl;

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

import java.math.BigDecimal;
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
    public void assignDiscountToProduct(String productId, String discountId) {
        log.info("Assigning discount {} to product {}", discountId, productId);

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        Discount discount = discountRepository.findByDiscountId(discountId)
                .orElseThrow(() -> new IllegalArgumentException("Discount not found with ID: " + discountId));

        if (!discount.isValid()) {
            throw new IllegalArgumentException("Discount is not valid or has expired");
        }

        if (product.getDiscount() != null) {
            throw new IllegalArgumentException("Product already has a discount assigned");
        }

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            throw new IllegalArgumentException(
                    "Product has variants. Please assign discount to specific variants instead.");
        }

        if (product.getTotalStockQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Product is out of stock. Cannot assign discount to out-of-stock products.");
        }

        product.setDiscount(discount);
        productRepository.save(product);

        log.info("Successfully assigned discount {} to product {}", discountId, productId);
    }

    @Override
    @Transactional
    public void assignDiscountToVariants(List<String> variantIds, String discountId) {
        log.info("Assigning discount {} to {} variants", discountId, variantIds.size());

        Discount discount = discountRepository.findByDiscountId(discountId)
                .orElseThrow(() -> new IllegalArgumentException("Discount not found with ID: " + discountId));

        if (!discount.isValid()) {
            throw new IllegalArgumentException("Discount is not valid or has expired");
        }

        List<Long> variantIdLongs = variantIds.stream()
                .map(Long::parseLong)
                .toList();
        List<ProductVariant> variants = productVariantRepository.findAllById(variantIdLongs);

        if (variants.size() != variantIds.size()) {
            throw new IllegalArgumentException("Some variants were not found");
        }

        for (ProductVariant variant : variants) {
            if (variant.getDiscount() != null) {
                throw new IllegalArgumentException(
                        String.format("Variant %s already has a discount assigned", variant.getId()));
            }

            if (variant.getTotalStockQuantity() <= 0) {
                throw new IllegalArgumentException(
                        String.format("Variant %s is out of stock. Cannot assign discount to out-of-stock variants.",
                                variant.getId()));
            }

            variant.setDiscount(discount);
        }

        productVariantRepository.saveAll(variants);

        log.info("Successfully assigned discount {} to {} variants", discountId, variantIds.size());
    }

    @Override
    @Transactional
    public void removeDiscountFromProduct(String productId) {
        log.info("Removing discount from product {}", productId);

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        if (product.getDiscount() == null) {
            throw new IllegalArgumentException("Product does not have a discount assigned");
        }

        product.setDiscount(null);
        productRepository.save(product);

        log.info("Successfully removed discount from product {}", productId);
    }

    @Override
    @Transactional
    public void removeDiscountFromVariants(List<String> variantIds) {
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
                throw new IllegalArgumentException(
                        String.format("Variant %s does not have a discount assigned", variant.getId()));
            }

            variant.setDiscount(null);
        }

        productVariantRepository.saveAll(variants);

        log.info("Successfully removed discount from {} variants", variantIds.size());
    }

    @Override
    public Page<Map<String, Object>> getProductsByDiscount(String discountId, Pageable pageable) {
        log.info("Getting products with discount {}", discountId);

        Discount discount = discountRepository.findByDiscountId(discountId)
                .orElseThrow(() -> new IllegalArgumentException("Discount not found with ID: " + discountId));

        Page<Product> products = productRepository.findByDiscount(discount, pageable);
        Page<ProductVariant> variants = productVariantRepository.findByDiscount(discount, pageable);

        return products.map(product -> {
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("productId", product.getProductId());
            productMap.put("name", product.getProductName());
            productMap.put("price", product.getPrice());
            productMap.put("discountedPrice", product.getDiscountedPrice());
            productMap.put("stockQuantity", product.getTotalStockQuantity());
            productMap.put("hasVariants", product.getVariants() != null && !product.getVariants().isEmpty());
            productMap.put("type", "product");
            return productMap;
        });
    }

    @Override
    public Map<String, Object> getProductDiscountStatus(String productId) {
        log.info("Getting discount status for product {}", productId);

        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        Map<String, Object> status = new HashMap<>();
        status.put("productId", productId);
        status.put("hasDiscount", product.getDiscount() != null);

        if (product.getDiscount() != null) {
            Discount discount = product.getDiscount();
            status.put("discountId", discount.getDiscountId());
            status.put("discountName", discount.getName());
            status.put("discountPercentage", discount.getPercentage());
            status.put("discountValid", discount.isValid());
        }

        status.put("hasVariants", product.getVariants() != null && !product.getVariants().isEmpty());

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            List<Map<String, Object>> variantStatuses = product.getVariants().stream()
                    .map(variant -> {
                        Map<String, Object> variantStatus = new HashMap<>();
                        variantStatus.put("variantId", variant.getId());
                        variantStatus.put("variantName", variant.getVariantName());
                        variantStatus.put("hasDiscount", variant.getDiscount() != null);
                        variantStatus.put("stockQuantity", variant.getTotalStockQuantity());

                        if (variant.getDiscount() != null) {
                            Discount variantDiscount = variant.getDiscount();
                            variantStatus.put("discountId", variantDiscount.getDiscountId());
                            variantStatus.put("discountName", variantDiscount.getName());
                            variantStatus.put("discountPercentage", variantDiscount.getPercentage());
                            variantStatus.put("discountValid", variantDiscount.isValid());
                        }

                        return variantStatus;
                    })
                    .toList();

            status.put("variants", variantStatuses);
        }

        return status;
    }
}
