package com.ecommerce.service.impl;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.ServiceImpl.JwtService;
import com.ecommerce.dto.*;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.ShopOrder;
import com.ecommerce.entity.User;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderTransactionRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.ShopOrderRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AnalyticsService;
import com.ecommerce.service.ShopAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final OrderTransactionRepository orderTransactionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final JwtService jwtService;
    private final com.ecommerce.repository.MoneyFlowRepository moneyFlowRepository;
    private final ShopAuthorizationService shopAuthorizationService;

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

        User user = getUser(bearerToken);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        UUID shopId = resolveShopId(user, request.getShopId());

        // If shopId is present, we calculate analytics for that shop
        if (shopId != null) {
            return getShopAnalytics(shopId, from, to, prevFrom, prevTo);
        }

        // Use Global Analytics (Admin only)
        if (user.getRole() == UserRole.ADMIN) {
            return getGlobalAnalytics(from, to, prevFrom, prevTo);
        }

        throw new IllegalArgumentException(
                "Could not determine analytics context. Please provide a shop ID or login as Admin.");
    }

    private UUID resolveShopId(User user, UUID requestedShopId) {
        if (requestedShopId != null) {
            if (shopAuthorizationService.hasAccessToShop(user.getId(), requestedShopId)
                    || user.getRole() == UserRole.ADMIN) {
                return requestedShopId;
            } else {
                throw new IllegalArgumentException("Access denied to requested shop analytics");
            }
        }

        // Infer link from user role
        if (user.getRole() == UserRole.EMPLOYEE || user.getRole() == UserRole.DELIVERY_AGENT) {
            Shop shop = user.getShop(); // Direct association
            return shop != null ? shop.getShopId() : null;
        }

        if (user.getRole() == UserRole.VENDOR) {
            List<Shop> ownedShops = shopRepository.findByOwnerId(user.getId());
            if (!ownedShops.isEmpty()) {
                // If multiple shops, logic might need adjustment, but defaulting to first is
                // reasonable fallback
                return ownedShops.get(0).getShopId();
            }
        }

        return null;
    }

    private AnalyticsResponseDTO getShopAnalytics(UUID shopId, LocalDateTime from, LocalDateTime to,
            LocalDateTime prevFrom, LocalDateTime prevTo) {
        // Orders
        long totalOrders = shopOrderRepository.countByShopIdAndCreatedAtBetween(shopId, from, to);
        long prevTotalOrders = shopOrderRepository.countByShopIdAndCreatedAtBetween(shopId, prevFrom, prevTo);
        Double totalOrdersVs = percentChange(prevTotalOrders, totalOrders);

        // Revenue
        BigDecimal revenue = shopOrderRepository.sumTotalAmountByShopIdAndCreatedAtBetween(shopId, from, to);
        BigDecimal prevRevenue = shopOrderRepository.sumTotalAmountByShopIdAndCreatedAtBetween(shopId, prevFrom,
                prevTo);
        Double revenueVs = percentChangeBig(prevRevenue, revenue);

        // New Customers (Unique users buying from this shop)
        long newCustomers = shopOrderRepository.countDistinctCustomersByShopIdAndCreatedAtBetween(shopId, from, to);
        long prevNewCustomers = shopOrderRepository.countDistinctCustomersByShopIdAndCreatedAtBetween(shopId, prevFrom,
                prevTo);
        Double newCustomersVs = percentChange(prevNewCustomers, newCustomers);

        // Active Products
        long activeProducts = productRepository.countActiveByShopId(shopId);
        Double activeProductsVs = 0.0; // Hard to track historic active counts without snapshot table

        // Top Products
        List<ShopOrder> shopOrders = shopOrderRepository.findByShopIdAndCreatedAtBetween(shopId, from, to);

        Map<UUID, ProductAgg> productAgg = new HashMap<>();
        Map<Long, CatAgg> catAgg = new HashMap<>();

        for (ShopOrder so : shopOrders) {
            for (OrderItem oi : so.getItems()) {
                Product effectiveProduct = oi.getEffectiveProduct();
                if (effectiveProduct == null)
                    continue;

                // Product Agg
                UUID pid = effectiveProduct.getProductId();
                ProductAgg pagg = productAgg.computeIfAbsent(pid, k -> new ProductAgg());
                pagg.name = effectiveProduct.getProductName();
                pagg.count += oi.getQuantity();
                pagg.amount = pagg.amount.add(oi.getSubtotal());

                // Category Agg
                if (effectiveProduct.getCategory() != null) {
                    Long cid = effectiveProduct.getCategory().getId();
                    CatAgg cagg = catAgg.computeIfAbsent(cid, k -> new CatAgg());
                    cagg.name = effectiveProduct.getCategory().getName();
                    cagg.amount = cagg.amount.add(oi.getSubtotal());
                }
            }
        }

        // Processing Top Products
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

        // Processing Category Performance
        BigDecimal totalCatAmount = catAgg.values().stream().map(a -> a.amount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        List<CategoryPerformanceDTO> categories = catAgg.entrySet().stream()
                .sorted((a, b) -> b.getValue().amount.compareTo(a.getValue().amount))
                .limit(5)
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

    private AnalyticsResponseDTO getGlobalAnalytics(LocalDateTime from, LocalDateTime to, LocalDateTime prevFrom,
            LocalDateTime prevTo) {
        // Orders
        List<Order> orders = orderRepository.findAllBetween(from, to);
        List<Order> prevOrders = orderRepository.findAllBetween(prevFrom, prevTo);

        long totalOrders = orders.size();
        long prevTotalOrders = prevOrders.size();
        Double totalOrdersVs = percentChange(prevTotalOrders, totalOrders);

        // New customers (Platform wide)
        long newCustomers = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(from)
                        && u.getCreatedAt().isBefore(to))
                .count();
        long prevNewCustomers = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(prevFrom)
                        && u.getCreatedAt().isBefore(prevTo))
                .count();
        Double newCustomersVs = percentChange(prevNewCustomers, newCustomers);

        // Revenue (Using MoneyFlow for global)
        BigDecimal balanceAtEnd = moneyFlowRepository.findBalanceAtTime(to).orElse(BigDecimal.ZERO);
        BigDecimal balanceBeforeStart = moneyFlowRepository.findBalanceBeforeTime(from).orElse(BigDecimal.ZERO);
        BigDecimal revenue = balanceAtEnd.subtract(balanceBeforeStart);

        BigDecimal prevBalanceAtEnd = moneyFlowRepository.findBalanceAtTime(prevTo).orElse(BigDecimal.ZERO);
        BigDecimal prevBalanceBeforeStart = moneyFlowRepository.findBalanceBeforeTime(prevFrom).orElse(BigDecimal.ZERO);
        BigDecimal prevRevenue = prevBalanceAtEnd.subtract(prevBalanceBeforeStart);

        Double revenueVs = percentChangeBig(prevRevenue, revenue);

        long activeProducts = productRepository.countActive();

        // Top Products Aggregation Global
        // Note: Global orders iterates Order -> ShopOrders -> Items? Or just Order ->
        // Items (deprecated?)
        // The Order entity has `shopOrders` but `getAllItems()` helper. existing code
        // used `o.getOrderItems()`.
        // Order.getOrderItems() is likely mapped to `shopOrders.items`.
        // Let's verify Order.java... `getAllItems()` helper exists. `getOrderItems()`
        // isn't explicit in viewed code,
        // but Order entity might have it or it's implicitly mapped?
        // Wait, Order.java viewed in Step 2119 shows `shopOrders` replacing direct
        // `orderItems`.
        // There is no `@OneToMany private List<OrderItem> orderItems;` anymore.
        // So `o.getOrderItems()` might fail if I use it. I should use `o.getAllItems()`
        // helper.
        // I will fix that for global analytics too.

        Map<UUID, ProductAgg> productAgg = new HashMap<>();
        Map<Long, CatAgg> catAgg = new HashMap<>();

        for (Order o : orders) {
            // Fix: Use getAllItems
            for (OrderItem oi : o.getAllItems()) {
                Product effectiveProduct = oi.getEffectiveProduct();
                if (effectiveProduct == null)
                    continue;

                // Product Agg
                UUID pid = effectiveProduct.getProductId();
                ProductAgg pagg = productAgg.computeIfAbsent(pid, k -> new ProductAgg());
                pagg.name = effectiveProduct.getProductName();
                pagg.count += oi.getQuantity();
                pagg.amount = pagg.amount.add(oi.getSubtotal());

                // Cat Agg
                if (effectiveProduct.getCategory() != null) {
                    Long cid = effectiveProduct.getCategory().getId();
                    CatAgg cagg = catAgg.computeIfAbsent(cid, k -> new CatAgg());
                    cagg.name = effectiveProduct.getCategory().getName();
                    cagg.amount = cagg.amount.add(oi.getSubtotal());
                }
            }
        }

        // Processing lists
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

        BigDecimal totalCatAmount = catAgg.values().stream().map(a -> a.amount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        List<CategoryPerformanceDTO> categories = catAgg.entrySet().stream()
                .sorted((a, b) -> b.getValue().amount.compareTo(a.getValue().amount))
                .limit(5)
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
                .activeProductsVsPercent(null)
                .topProducts(top)
                .categoryPerformance(categories)
                .build();
    }

    private User getUser(String bearerToken) {
        String token = extractToken(bearerToken);
        if (token == null)
            return null;
        String username = jwtService.extractUsername(token);
        return userRepository.findByUserEmail(username)
                .orElse(null);
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
