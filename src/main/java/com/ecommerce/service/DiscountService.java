package com.ecommerce.service;

import com.ecommerce.dto.CreateDiscountDTO;
import com.ecommerce.dto.DiscountDTO;
import com.ecommerce.dto.UpdateDiscountDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface DiscountService {

    DiscountDTO createDiscount(CreateDiscountDTO createDiscountDTO);

    DiscountDTO updateDiscount(UUID discountId, UpdateDiscountDTO updateDiscountDTO);

    boolean deleteDiscount(UUID discountId);

    DiscountDTO getDiscountById(UUID discountId);

    Page<DiscountDTO> getAllDiscounts(Pageable pageable);

    Page<DiscountDTO> getActiveDiscounts(Pageable pageable);

    DiscountDTO getDiscountByCode(String discountCode);

    boolean isDiscountValid(UUID discountId);

    boolean isDiscountCodeValid(String discountCode);
}
