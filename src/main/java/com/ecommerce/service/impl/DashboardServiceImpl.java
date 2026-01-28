package com.ecommerce.service.impl;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.ServiceImpl.JwtService;
import com.ecommerce.dto.AlertsDTO;
import com.ecommerce.dto.DashboardResponseDTO;
import com.ecommerce.dto.RecentOrderDTO;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.DashboardService;
import com.ecommerce.service.ShopAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final com.ecommerce.repository.ShopOrderRepository shopOrderRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final JwtService jwtService;
    private final com.ecommerce.service.MoneyFlowService moneyFlowService;
    private final ShopAuthorizationService shopAuthorizationService;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponseDTO getDashboardData(String bearerToken, String shopSlug) {
        String token = extractToken(bearerToken);
        String username = token != null ? jwtService.extractUsername(token) : null;

        if (username == null) {
            throw new IllegalArgumentException("Authentication required");
        }

        User user = userRepository.findByUserEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserRole role = user.getRole();
        UUID userId = user.getId();

        UUID shopId = null;
        Shop shop = null;

        // Validate shop access for VENDOR and EMPLOYEE
        if (shopSlug != null && !shopSlug.trim().isEmpty()) {
            shop = shopRepository.findBySlug(shopSlug)
                    .orElseThrow(() -> new IllegalArgumentException("Shop not found with slug: " + shopSlug));

            shopId = shop.getShopId();

            // For VENDOR and EMPLOYEE, validate they have access to this shop
            if (role == UserRole.VENDOR || role == UserRole.EMPLOYEE) {
                if (!shopAuthorizationService.canManageShop(userId, shopId)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "You are not authorized to access this shop");
                }
            }
            // ADMIN can access any shop
        } else {
            // If no shopSlug provided, only ADMIN can access global dashboard
            if (role != UserRole.ADMIN) {
                throw new IllegalArgumentException("shopSlug is required for VENDOR and EMPLOYEE roles");
            }
        }

        long totalProducts;
        long totalOrders;
        long totalCustomers;
        long pendingOrders;
        long lowStock;
        BigDecimal revenue = null;
        List<RecentOrderDTO> recentOrders = new java.util.ArrayList<>();

        if (shopId != null) {
            // Shop-scoped data using ShopOrderRepository
            totalProducts = productRepository.countByShopId(shopId);
            totalOrders = shopOrderRepository.countByShopId(shopId);
            totalCustomers = shopOrderRepository.countDistinctCustomersByShopId(shopId);
            pendingOrders = shopOrderRepository.countByShopIdAndStatus(shopId,
                    com.ecommerce.entity.ShopOrder.ShopOrderStatus.PENDING);
            lowStock = productRepository.countLowStockByShopId(shopId);

            List<com.ecommerce.entity.ShopOrder> shopOrders = shopOrderRepository
                    .findRecentOrdersByShopId(shopId, PageRequest.of(0, 3)).getContent();
            recentOrders = shopOrders.stream()
                    .map(so -> RecentOrderDTO.builder()
                            .orderId(so.getShopOrderCode())
                            .status(so.getStatus().name())
                            .amount(so.getTotalAmount())
                            .owner(so.getOrder().getUser() != null ? so.getOrder().getUser().getFullName() : "Guest")
                            .build())
                    .collect(Collectors.toList());

            // Revenue for shop (only for ADMIN or shop owner/authorized)
            if (role == UserRole.ADMIN || shopAuthorizationService.canManageShop(userId, shopId)) {
                revenue = shopOrderRepository.sumTotalRevenueByShopId(shopId);

                AlertsDTO alerts = AlertsDTO.builder()
                        .lowStockProducts(lowStock)
                        .pendingOrders(pendingOrders)
                        .build();

                DashboardResponseDTO.DashboardResponseDTOBuilder builder = DashboardResponseDTO.builder()
                        .totalProducts(totalProducts)
                        .totalOrders(totalOrders)
                        .totalRevenue(revenue)
                        .pointsRevenue(shopOrderRepository.sumTotalPointsRevenueByShopId(shopId))
                        .totalCustomers(totalCustomers)
                        .recentOrders(recentOrders)
                        .alerts(alerts);
                return builder.build();
            }
        } else {
            // Global data (ADMIN only)
            totalProducts = productRepository.count();
            totalOrders = orderRepository.count();
            totalCustomers = userRepository.count();
            // Count pending across all shop orders in the system
            pendingOrders = com.ecommerce.entity.ShopOrder.ShopOrderStatus.PENDING != null
                    ? shopOrderRepository.countByStatus(com.ecommerce.entity.ShopOrder.ShopOrderStatus.PENDING)
                    : 0L;
            lowStock = productRepository.countLowStock();

            List<Order> globalOrders = orderRepository.findAll(
                    PageRequest.of(0, 3, org.springframework.data.domain.Sort.by("createdAt").descending()))
                    .getContent();

            recentOrders = globalOrders.stream()
                    .map(o -> RecentOrderDTO.builder()
                            .orderId(o.getOrderCode())
                            .status(o.getStatus())
                            .amount(o.getOrderTransaction() != null ? o.getOrderTransaction().getOrderAmount()
                                    : (o.getOrderInfo() != null ? o.getOrderInfo().getTotalAmount() : BigDecimal.ZERO))
                            .owner(o.getUser() != null ? o.getUser().getFullName() : "Guest")
                            .build())
                    .collect(Collectors.toList());

            if (role == UserRole.ADMIN) {
                revenue = moneyFlowService.getNetRevenue();
            }
        }

        AlertsDTO alerts = AlertsDTO.builder()
                .lowStockProducts(lowStock)
                .pendingOrders(pendingOrders)
                .build();

        return DashboardResponseDTO.builder()
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalRevenue(revenue)
                .totalCustomers(totalCustomers)
                .recentOrders(recentOrders)
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
