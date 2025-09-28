package com.ecommerce.service;

import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.Exception.CheckoutValidationException;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedStockValidationService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductAvailabilityService productAvailabilityService;

    public void validateCartItems(List<CartItemDTO> items) {
        for (CartItemDTO item : items) {
            validateCartItem(item);
        }
    }

    public void validateCartItem(CartItemDTO item) {
        if (item.getVariantId() != null) {
            validateVariantItem(item);
        } else if (item.getProductId() != null) {
            validateProductItem(item);
        } else {
            throw new CheckoutValidationException("INVALID_ITEM", "Cart item must have either productId or variantId");
        }
    }

    private void validateVariantItem(CartItemDTO item) {
        ProductVariant variant = variantRepository.findById(item.getVariantId())
            .orElseThrow(() -> new CheckoutValidationException("VARIANT_NOT_FOUND", "Product variant not found with ID: " + item.getVariantId()));

        Product product = variant.getProduct();
        if (product == null) {
            throw new CheckoutValidationException("PRODUCT_NOT_FOUND", "Product not found for variant ID: " + item.getVariantId());
        }

        if (!product.isActive()) {
            throw new CheckoutValidationException("PRODUCT_INACTIVE", "Product is not active: " + product.getProductName());
        }

        if (!Boolean.TRUE.equals(product.getDisplayToCustomers())) {
            throw new CheckoutValidationException("PRODUCT_NOT_AVAILABLE", "Product is not available for customers: " + product.getProductName());
        }

        if (!variant.isActive()) {
            throw new CheckoutValidationException("VARIANT_INACTIVE", "Product variant is not active: " + variant.getVariantSku());
        }

        if (!productAvailabilityService.isVariantAvailableForCustomers(variant)) {
            int totalStock = productAvailabilityService.getVariantTotalStock(variant);
            String reason = totalStock <= 0 ? "out of stock" : "not available for customers";
            throw new CheckoutValidationException("VARIANT_NOT_AVAILABLE", 
                "Product variant is not available: " + variant.getVariantSku() + " (" + reason + ", stock: " + totalStock + ")");
        }

        int availableStock = productAvailabilityService.getVariantTotalStock(variant);
        if (availableStock < item.getQuantity()) {
            throw new CheckoutValidationException("INSUFFICIENT_STOCK", "Insufficient stock for variant " + variant.getVariantSku() + 
                ". Available: " + availableStock + ", Requested: " + item.getQuantity());
        }
    }

    private void validateProductItem(CartItemDTO item) {
        Product product = productRepository.findById(item.getProductId())
            .orElseThrow(() -> new CheckoutValidationException("PRODUCT_NOT_FOUND", "Product not found with ID: " + item.getProductId()));

        if (!product.isActive()) {
            throw new CheckoutValidationException("PRODUCT_INACTIVE", "Product is not active: " + product.getProductName());
        }

        if (!Boolean.TRUE.equals(product.getDisplayToCustomers())) {
            throw new CheckoutValidationException("PRODUCT_NOT_AVAILABLE", "Product is not available for customers: " + product.getProductName());
        }

        if (!productAvailabilityService.isProductAvailableForCustomers(product)) {
            throw new CheckoutValidationException("PRODUCT_NOT_AVAILABLE", "Product is not available: " + product.getProductName());
        }

        int availableStock = productAvailabilityService.getTotalAvailableStock(product);
        if (availableStock < item.getQuantity()) {
            throw new CheckoutValidationException("INSUFFICIENT_STOCK", "Insufficient stock for product " + product.getProductName() + 
                ". Available: " + availableStock + ", Requested: " + item.getQuantity());
        }
    }
}
