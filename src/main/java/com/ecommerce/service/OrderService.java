package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.dto.CreateOrderDTO;
import com.ecommerce.dto.AdminOrderDTO;
import com.ecommerce.dto.DeliveryOrderDTO;
import com.ecommerce.dto.CustomerOrderDTO;
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

    // Customer methods
    List<CustomerOrderDTO> getCustomerOrders(UUID userId);

    CustomerOrderDTO getCustomerOrderById(UUID userId, Long orderId);

    CustomerOrderDTO getCustomerOrderByNumber(UUID userId, String orderNumber);

    // Admin methods
    List<AdminOrderDTO> getAllAdminOrders();

    List<AdminOrderDTO> getOrdersByStatus(String status);

    AdminOrderDTO getAdminOrderById(Long orderId);

    AdminOrderDTO getAdminOrderByNumber(String orderNumber);

    // Delivery agency methods
    List<DeliveryOrderDTO> getDeliveryOrders();

    List<DeliveryOrderDTO> getDeliveryOrdersByStatus(String status);

    DeliveryOrderDTO getDeliveryOrderByNumber(String orderNumber);

    Order updateOrderTracking(Long orderId, String trackingNumber, String estimatedDelivery);
}