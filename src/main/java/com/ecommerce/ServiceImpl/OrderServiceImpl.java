package com.ecommerce.ServiceImpl;

import com.ecommerce.entity.Order;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

    @Override
    public List<Order> getOrdersForUser(UUID userId) {
        return orderRepository.findAllForUserWithDetails(userId);
    }

    @Override
    public Order getOrderByIdForUser(UUID userId, Long orderId) {
        return orderRepository.findByIdForUserWithDetails(userId, orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }
}