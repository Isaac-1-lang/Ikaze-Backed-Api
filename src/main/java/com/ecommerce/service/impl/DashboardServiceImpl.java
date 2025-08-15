package com.ecommerce.service.impl;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.ServiceImpl.JwtService;
import com.ecommerce.dto.AlertsDTO;
import com.ecommerce.dto.DashboardResponseDTO;
import com.ecommerce.dto.RecentOrderDTO;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderTransactionRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderTransactionRepository orderTransactionRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public DashboardResponseDTO getDashboardData(String bearerToken) {
        String token = extractToken(bearerToken);
        String username = token != null ? jwtService.extractUsername(token) : null;
        UserRole role = null;
        if (username != null) {
            role = userRepository.findByUserEmail(username)
                    .map(User::getRole)
                    .orElse(null);
        }

        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long totalCustomers = userRepository.count();
        long pendingOrders = orderRepository.findByOrderStatus(Order.OrderStatus.PENDING).size();
        long lowStock = productRepository.countLowStock();

        BigDecimal revenue = null;
        if (role == UserRole.ADMIN) {
            revenue = orderTransactionRepository.sumCompletedRevenue();
        }

        List<Order> recent = orderRepository
                .findAll(PageRequest.of(0, 3, org.springframework.data.domain.Sort.by("createdAt").descending()))
                .getContent();

        List<RecentOrderDTO> recentDtos = recent.stream().map(o -> RecentOrderDTO.builder()
                .orderId(o.getOrderId())
                .status(o.getOrderStatus())
                .amount(o.getOrderTransaction() != null ? o.getOrderTransaction().getOrderAmount()
                        : (o.getOrderInfo() != null ? o.getOrderInfo().getTotalAmount() : BigDecimal.ZERO))
                .owner(o.getUser() != null ? (o.getUser().getFullName()) : "guest")
                .build())
                .collect(Collectors.toList());

        AlertsDTO alerts = AlertsDTO.builder()
                .lowStockProducts(lowStock)
                .pendingOrders(pendingOrders)
                .build();

        return DashboardResponseDTO.builder()
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalRevenue(revenue)
                .totalCustomers(totalCustomers)
                .recentOrders(recentDtos)
                .alerts(alerts)
                .build();
    }

    private String extractToken(String bearerToken) {
        if (bearerToken == null)
            return null;
        String trimmed = bearerToken.trim();
        if (trimmed.toLowerCase().startsWith("bearer ")) {
            return trimmed.substring(7);
        }
        return trimmed;
    }
}
