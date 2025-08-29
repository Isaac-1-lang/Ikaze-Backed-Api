package com.ecommerce.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrderAnalyticsService {
    
    /**
     * Get total number of orders
     */
    Long getTotalOrdersCount();
    
    /**
     * Get total revenue from all orders
     */
    Double getTotalRevenue();
    
    /**
     * Get average order value
     */
    Double getAverageOrderValue();
    
    /**
     * Get orders count grouped by status
     */
    Map<String, Long> getOrdersCountByStatus();
    
    /**
     * Get customer order statistics
     */
    Map<String, Object> getCustomerOrderStats();
    
    /**
     * Get delivery performance metrics
     */
    Map<String, Object> getDeliveryPerformanceMetrics();
    
    /**
     * Get top selling products
     */
    List<Map<String, Object>> getTopSellingProducts(int limit);
    
    /**
     * Get revenue trend for specified number of days
     */
    List<Map<String, Object>> getRevenueTrend(int days);
    
    /**
     * Get revenue for a specific period
     */
    Double getRevenueForPeriod(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get orders by date range
     */
    List<Map<String, Object>> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}
