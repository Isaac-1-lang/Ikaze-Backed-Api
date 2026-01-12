package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.dto.CreateOrderDTO;
import com.ecommerce.dto.AdminOrderDTO;
import com.ecommerce.dto.DeliveryOrderDTO;
import com.ecommerce.dto.CustomerOrderDTO;
import com.ecommerce.dto.OrderSearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrderService {
    List<Order> getAllOrders();

    Page<Order> getAllOrders(Pageable pageable);

    List<Order> getOrdersForUser(UUID userId);

    Order getOrderByIdForUser(UUID userId, Long orderId);

    Order getOrderById(Long orderId);

    Order getOrderByNumber(UUID userId, String orderNumber);

    List<CustomerOrderDTO> getCustomerOrders(UUID userId);

    CustomerOrderDTO getCustomerOrderById(UUID userId, Long orderId);

    CustomerOrderDTO getCustomerOrderByNumber(UUID userId, String orderNumber);

    // Admin methods
    List<AdminOrderDTO> getAllAdminOrders();

    Page<AdminOrderDTO> getAllAdminOrdersPaginated(Pageable pageable);

    Page<AdminOrderDTO> getAllAdminOrdersPaginated(Pageable pageable, UUID shopId);

    AdminOrderDTO getAdminOrderById(Long orderId);

    AdminOrderDTO getAdminOrderByNumber(String orderNumber);

    Page<AdminOrderDTO> searchOrders(OrderSearchDTO searchRequest, Pageable pageable);

    List<AdminOrderDTO> getOrdersByStatus(String status);

    com.ecommerce.entity.ShopOrder updateShopOrderStatus(Long shopOrderId, String status);

    // Delivery agency methods
    List<DeliveryOrderDTO> getDeliveryOrders();

    List<DeliveryOrderDTO> getDeliveryOrdersByStatus(String status);

    DeliveryOrderDTO getDeliveryOrderByNumber(String orderNumber);

    com.ecommerce.entity.ShopOrder updateShopOrderTracking(Long shopOrderId, String trackingNumber,
            String estimatedDelivery);

    // Public tracking methods
    Order getOrderByOrderCode(String orderCode);

    com.ecommerce.entity.ShopOrder getShopOrderByPickupToken(String pickupToken);

    Order saveOrder(Order order);

    boolean checkAllOrdersDeliveredInGroup(Long groupId);

    void autoFinishDeliveryGroup(Long groupId);

    long countOrdersByStatus(String status);

    long countProcessingOrdersWithoutDeliveryGroup();
}