package com.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ProductDiscountService {

    void assignDiscountToProduct(String productId, String discountId);

    void assignDiscountToVariants(List<String> variantIds, String discountId);

    void removeDiscountFromProduct(String productId);

    void removeDiscountFromVariants(List<String> variantIds);

    Page<Map<String, Object>> getProductsByDiscount(String discountId, Pageable pageable);

    Map<String, Object> getProductDiscountStatus(String productId);
}
