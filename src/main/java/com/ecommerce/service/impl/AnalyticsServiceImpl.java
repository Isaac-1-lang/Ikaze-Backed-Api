package com.ecommerce.service.impl;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.ServiceImpl.JwtService;
import com.ecommerce.dto.*;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderTransactionRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderTransactionRepository orderTransactionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final JwtService jwtService;

    @Override
    public AnalyticsResponseDTO getAnalytics(AnalyticsRequestDTO request, String bearerToken) {
        LocalDate start = Optional.ofNullable(request.getStartDate()).orElse(LocalDate.now().minusDays(30));
        LocalDate end = Optional.ofNullable(request.getEndDate()).orElse(LocalDate.now());
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();

        // previous period same length
        long days = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(start, end.plusDays(1)));
        LocalDateTime prevFrom = from.minusDays(days);
        LocalDateTime prevTo = to.minusDays(days);

        boolean isAdmin = isAdmin(bearerToken);

        // Orders in period
        List<Order> orders = orderRepository.findAllBetween(from, to);
        List<Order> prevOrders = orderRepository.findAllBetween(prevFrom, prevTo);

        long totalOrders = orders.size();
        long prevTotalOrders = prevOrders.size();
        Double totalOrdersVs = percentChange(prevTotalOrders, totalOrders);

        // New customers: users created in range
        long newCustomers = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(from)
                        && u.getCreatedAt().isBefore(to))
                .count();
        long prevNewCustomers = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(prevFrom)
                        && u.getCreatedAt().isBefore(prevTo))
                .count();
        Double newCustomersVs = percentChange(prevNewCustomers, newCustomers);

        // Active products (all-time) and compare with previous period by creation date
        // (approx)
        long activeProducts = productRepository.countActive();

        Double activeProductsVs = null;

        // Revenue (completed transactions) in range
        BigDecimal revenue = null;
        Double revenueVs = null;
        if (isAdmin) {
            BigDecimal rev = orders.stream()
                    .map(o -> o.getOrderTransaction())
                    .filter(Objects::nonNull)
                    .filter(ot -> ot.getStatus() == OrderTransaction.TransactionStatus.COMPLETED)
                    .map(OrderTransaction::getOrderAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal prevRev = prevOrders.stream()
                    .map(o -> o.getOrderTransaction())
                    .filter(Objects::nonNull)
                    .filter(ot -> ot.getStatus() == OrderTransaction.TransactionStatus.COMPLETED)
                    .map(OrderTransaction::getOrderAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            revenue = rev;
            revenueVs = percentChangeBig(prevRev, rev);
        }

        // Top products (by quantity and revenue) in range
        Map<UUID, ProductAgg> productAgg = new HashMap<>();
        for (Order o : orders) {
            for (OrderItem oi : o.getOrderItems()) {
                UUID productId = oi.getProductVariant().getProduct().getProductId();
                ProductAgg agg = productAgg.computeIfAbsent(productId, k -> new ProductAgg());
                agg.count += oi.getQuantity();
                agg.amount = agg.amount.add(oi.getSubtotal());
                agg.name = oi.getProductVariant().getProduct().getProductName();
            }
        }
        long totalUnits = productAgg.values().stream().mapToLong(a -> a.count).sum();
        List<TopProductDTO> top = productAgg.entrySet().stream()
                .sorted((a, b) -> b.getValue().amount.compareTo(a.getValue().amount))
                .limit(5)
                .map(e -> TopProductDTO.builder()
                        .productId(e.getKey())
                        .productName(e.getValue().name)
                        .totalSalesCount(e.getValue().count)
                        .totalSalesAmount(e.getValue().amount)
                        .performancePercent(totalUnits == 0 ? 0.0 : round2((e.getValue().count * 100.0) / totalUnits))
                        .build())
                .collect(Collectors.toList());

        // Category performance by revenue
        Map<Long, CatAgg> catAgg = new HashMap<>();
        for (Order o : orders) {
            for (OrderItem oi : o.getOrderItems()) {
                if (oi.getProductVariant().getProduct().getCategory() == null)
                    continue;
                Long cid = oi.getProductVariant().getProduct().getCategory().getId();
                String cname = oi.getProductVariant().getProduct().getCategory().getName();
                CatAgg agg = catAgg.computeIfAbsent(cid, k -> new CatAgg());
                agg.name = cname;
                agg.amount = agg.amount.add(oi.getSubtotal());
            }
        }
        BigDecimal totalCatAmount = catAgg.values().stream().map(a -> a.amount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        List<CategoryPerformanceDTO> categories = catAgg.entrySet().stream()
                .sorted((a, b) -> b.getValue().amount.compareTo(a.getValue().amount))
                .map(e -> CategoryPerformanceDTO.builder()
                        .categoryId(e.getKey())
                        .categoryName(e.getValue().name)
                        .revenue(e.getValue().amount)
                        .revenuePercent(totalCatAmount.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                                : round2(e.getValue().amount.multiply(BigDecimal.valueOf(100))
                                        .divide(totalCatAmount, 4, RoundingMode.HALF_UP).doubleValue()))
                        .build())
                .collect(Collectors.toList());

        return AnalyticsResponseDTO.builder()
                .totalRevenue(revenue)
                .totalRevenueVsPercent(revenueVs)
                .totalOrders(totalOrders)
                .totalOrdersVsPercent(totalOrdersVs)
                .newCustomers(newCustomers)
                .newCustomersVsPercent(newCustomersVs)
                .activeProducts(activeProducts)
                .activeProductsVsPercent(activeProductsVs)
                .topProducts(top)
                .categoryPerformance(categories)
                .build();
    }

    private boolean isAdmin(String bearerToken) {
        String token = extractToken(bearerToken);
        if (token == null)
            return false;
        String username = jwtService.extractUsername(token);
        return userRepository.findByUserEmail(username)
                .map(User::getRole)
                .map(r -> r == UserRole.ADMIN)
                .orElse(false);
    }

    private String extractToken(String bearerToken) {
        if (bearerToken == null)
            return null;
        String t = bearerToken.trim();
        if (t.toLowerCase().startsWith("bearer "))
            return t.substring(7);
        return t;
    }

    private Double percentChange(long prev, long curr) {
        if (prev == 0)
            return curr == 0 ? 0.0 : 100.0;
        return round2(((curr - prev) * 100.0) / prev);
    }

    private Double percentChangeBig(BigDecimal prev, BigDecimal curr) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) {
            return (curr == null || curr.compareTo(BigDecimal.ZERO) == 0) ? 0.0 : 100.0;
        }
        BigDecimal diff = curr.subtract(prev);
        return round2(diff.multiply(BigDecimal.valueOf(100)).divide(prev, 4, RoundingMode.HALF_UP).doubleValue());
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static class ProductAgg {
        long count = 0;
        BigDecimal amount = BigDecimal.ZERO;
        String name;
    }

    private static class CatAgg {
        BigDecimal amount = BigDecimal.ZERO;
        String name;
    }
}
