package com.ecommerce.repository;

import com.ecommerce.entity.ShopOrder;
import com.ecommerce.dto.OrderSearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShopOrderRepositoryCustom {
    Page<ShopOrder> searchShopOrders(OrderSearchDTO searchRequest, Pageable pageable);
}
