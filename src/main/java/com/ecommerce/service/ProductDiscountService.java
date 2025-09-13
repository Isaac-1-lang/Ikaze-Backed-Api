package com.ecommerce.service;

import com.ecommerce.dto.AssignDiscountRequest;
import com.ecommerce.dto.RemoveDiscountRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ProductDiscountService {

    void assignDiscount(AssignDiscountRequest request);

    void removeDiscount(RemoveDiscountRequest request);

    Page<Map<String, Object>> getProductsByDiscount(String discountId, Pageable pageable);

    Map<String, Object> getProductDiscountStatus(String productId);
}
