package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.dto.OrderSearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {
    Page<Order> searchOrders(OrderSearchDTO searchRequest, Pageable pageable);
}
