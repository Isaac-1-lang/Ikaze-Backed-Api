package com.ecommerce.service;

import com.ecommerce.dto.SystemResetRequest;
import com.ecommerce.dto.SystemResetResponse;

/**
 * Service for system reset operations
 * Provides admin-only functionality to delete various entities with cascading
 */
public interface SystemResetService {
    
    /**
     * Performs a system reset based on the provided request
     * Deletes selected entities using multithreading for efficiency
     * Continues execution even if errors occur, collecting all errors for reporting
     * 
     * @param request The system reset request containing deletion flags
     * @return SystemResetResponse containing statistics and any errors encountered
     */
    SystemResetResponse performSystemReset(SystemResetRequest request);
    
    /**
     * Deletes all products with cascading relationships
     * Removes associated variants, images, videos, stocks, batches, cart items, wishlist items
     * 
     * @return Number of products deleted
     */
    long deleteAllProducts();
    
    /**
     * Deletes all discounts with cascading relationships
     * Removes discount associations from products and variants
     * 
     * @return Number of discounts deleted
     */
    long deleteAllDiscounts();
    
    /**
     * Deletes all orders with cascading relationships
     * Removes order items, transactions, addresses, customer info, tracking tokens, delivery notes
     * 
     * @return Number of orders deleted
     */
    long deleteAllOrders();
    
    /**
     * Deletes all reward systems with cascading relationships
     * Removes reward ranges and associated user points
     * 
     * @return Number of reward systems deleted
     */
    long deleteAllRewardSystems();
    
    /**
     * Deletes all shipping costs
     * 
     * @return Number of shipping costs deleted
     */
    long deleteAllShippingCosts();
    
    /**
     * Deletes all money flow records
     * 
     * @return Number of money flow records deleted
     */
    long deleteAllMoneyFlows();
    
    /**
     * Deletes all categories with cascading relationships
     * Removes category associations from products and child categories
     * 
     * @return Number of categories deleted
     */
    long deleteAllCategories();
    
    /**
     * Deletes all brands with cascading relationships
     * Removes brand associations from products
     * 
     * @return Number of brands deleted
     */
    long deleteAllBrands();
    
    /**
     * Deletes all warehouses with cascading relationships
     * Removes associated stocks, batches, and warehouse images
     * 
     * @return Number of warehouses deleted
     */
    long deleteAllWarehouses();
}
