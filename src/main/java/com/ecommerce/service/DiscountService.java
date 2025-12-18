package com.ecommerce.service;

import com.ecommerce.dto.CreateDiscountDTO;
import com.ecommerce.dto.DiscountDTO;
import com.ecommerce.dto.UpdateDiscountDTO;
import com.ecommerce.entity.Discount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface DiscountService {

    DiscountDTO createDiscount(UUID vendorId, CreateDiscountDTO createDiscountDTO);

    DiscountDTO updateDiscount(UUID discountId, UUID vendorId, UpdateDiscountDTO updateDiscountDTO);

    boolean deleteDiscount(UUID discountId, UUID vendorId);

    DiscountDTO getDiscountById(UUID discountId, UUID shopId);

    Page<DiscountDTO> getAllDiscounts(UUID shopId, Pageable pageable);

    Page<DiscountDTO> getActiveDiscounts(UUID shopId, Pageable pageable);

    DiscountDTO getDiscountByCode(String discountCode, UUID shopId);

    boolean isDiscountValid(UUID discountId, UUID shopId);

    boolean isDiscountCodeValid(String discountCode, UUID shopId);

    List<Discount> getAllDiscountEntities();

    void saveAllDiscounts(List<Discount> discounts);
}
