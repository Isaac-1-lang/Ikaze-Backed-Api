package com.ecommerce.service;

import com.ecommerce.entity.Order;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    List<Order> getOrdersForUser(UUID userId);
    Order getOrderByIdForUser(UUID userId, Long orderId);
}