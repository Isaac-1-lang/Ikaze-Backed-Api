package com.ecommerce.service;

import com.ecommerce.entity.Product;
import com.ecommerce.enums.ProductStatus;
import org.springframework.stereotype.Service;

@Service
public class ProductStatusCalculationService {

    public ProductStatus calculateProductStatus(Product product) {
        int completion = calculateCompletionPercentage(product);

        if (completion < 30)
            return ProductStatus.DRAFT;
        if (completion < 80)
            return ProductStatus.INCOMPLETE;
        if (completion < 100)
            return ProductStatus.READY;
        return ProductStatus.PUBLISHED;
    }

    public int calculateCompletionPercentage(Product product) {
        int score = 0;
        int total = 10;

        if (product.getProductName() != null && !product.getProductName().trim().isEmpty())
            score++;
        if (product.getCategory() != null)
            score++;
        if (product.getPrice() != null && product.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0)
            score++;
        if (product.getSku() != null && !product.getSku().trim().isEmpty())
            score++;
        if (product.getShortDescription() != null && !product.getShortDescription().trim().isEmpty())
            score++;
        if (product.getImages() != null && !product.getImages().isEmpty())
            score++;
        if (product.getBrand() != null)
            score++;
        if (product.getSlug() != null && !product.getSlug().trim().isEmpty())
            score++;
        if (product.getProductDetail() != null)
            score++;
        if (product.getDisplayToCustomers() != null && product.getDisplayToCustomers())
            score++;

        return (score * 100) / total;
    }

    public boolean isVisibleToCustomers(Product product) {
        Boolean displayToCustomers = product.getDisplayToCustomers();
        if (displayToCustomers == null) {
            displayToCustomers = false;
        }

        return displayToCustomers && product.isActive();
    }
}
