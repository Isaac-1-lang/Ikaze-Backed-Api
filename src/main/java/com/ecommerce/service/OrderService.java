package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.dto.CreateOrderDTO;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrderService {
    List<Order> getAllOrders();

    Order getOrderById(Long orderId);

    List<Order> getOrdersForUser(UUID userId);

    Order getOrderByIdForUser(UUID userId, Long orderId);

    Order getOrderByNumber(UUID userId, String orderNumber);

    Order createOrder(UUID userId, CreateOrderDTO createOrderDTO);

    Order cancelOrder(UUID userId, Long orderId);

    Order updateOrderStatus(Long orderId, String status);

    Map<String, Object> getOrderTracking(UUID userId, Long orderId);
}