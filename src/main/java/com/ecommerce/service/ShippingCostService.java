package com.ecommerce.service;

import com.ecommerce.dto.CreateShippingCostDTO;
import com.ecommerce.dto.ShippingCostDTO;
import com.ecommerce.dto.UpdateShippingCostDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ShippingCostService {

    /**
     * Create a new shipping cost configuration
     */
    ShippingCostDTO createShippingCost(CreateShippingCostDTO createShippingCostDTO);
    ShippingCostDTO createShippingCost(CreateShippingCostDTO createShippingCostDTO, UUID shopId);

    /**
     * Get all shipping costs with pagination
     */
    Page<ShippingCostDTO> getAllShippingCosts(Pageable pageable);
    Page<ShippingCostDTO> getAllShippingCosts(UUID shopId, Pageable pageable);

    /**
     * Get all active shipping costs
     */
    List<ShippingCostDTO> getActiveShippingCosts();
    List<ShippingCostDTO> getActiveShippingCosts(UUID shopId);

    /**
     * Get shipping cost by ID
     */
    ShippingCostDTO getShippingCostById(Long id);
    ShippingCostDTO getShippingCostById(Long id, UUID shopId);

    /**
     * Update shipping cost
     */
    ShippingCostDTO updateShippingCost(Long id, UpdateShippingCostDTO updateShippingCostDTO);
    ShippingCostDTO updateShippingCost(Long id, UpdateShippingCostDTO updateShippingCostDTO, UUID shopId);

    /**
     * Delete shipping cost
     */
    void deleteShippingCost(Long id);
    void deleteShippingCost(Long id, UUID shopId);

    /**
     * Search shipping costs by name
     */
    Page<ShippingCostDTO> searchShippingCosts(String name, Pageable pageable);
    Page<ShippingCostDTO> searchShippingCosts(String name, UUID shopId, Pageable pageable);

    /**
     * Calculate shipping cost for given parameters
     */
    BigDecimal calculateShippingCost(BigDecimal weight, BigDecimal distance, BigDecimal orderValue);
    BigDecimal calculateShippingCost(BigDecimal weight, BigDecimal distance, BigDecimal orderValue, UUID shopId);

    /**
     * Calculate shipping cost for order with address and items
     */
    BigDecimal calculateOrderShippingCost(com.ecommerce.dto.AddressDto deliveryAddress,
            java.util.List<com.ecommerce.dto.CartItemDTO> items, BigDecimal orderValue);
    BigDecimal calculateOrderShippingCost(com.ecommerce.dto.AddressDto deliveryAddress,
            java.util.List<com.ecommerce.dto.CartItemDTO> items, BigDecimal orderValue, UUID shopId);

    /**
     * Calculate shipping details including distance and cost breakdown
     */
    com.ecommerce.dto.ShippingDetailsDTO calculateShippingDetails(com.ecommerce.dto.AddressDto deliveryAddress,
            java.util.List<com.ecommerce.dto.CartItemDTO> items, BigDecimal orderValue);
    com.ecommerce.dto.ShippingDetailsDTO calculateShippingDetails(com.ecommerce.dto.AddressDto deliveryAddress,
            java.util.List<com.ecommerce.dto.CartItemDTO> items, BigDecimal orderValue, UUID shopId);

    /**
     * Calculate enhanced shipping details using multi-warehouse allocation logic
     * This method considers stock allocation across multiple warehouses and calculates
     * shipping cost from the furthest warehouse involved in the order
     */
    com.ecommerce.dto.ShippingDetailsDTO calculateEnhancedShippingDetails(com.ecommerce.dto.AddressDto deliveryAddress,
            java.util.List<com.ecommerce.dto.CartItemDTO> items, BigDecimal orderValue);
    com.ecommerce.dto.ShippingDetailsDTO calculateEnhancedShippingDetails(com.ecommerce.dto.AddressDto deliveryAddress,
            java.util.List<com.ecommerce.dto.CartItemDTO> items, BigDecimal orderValue, UUID shopId);

    /**
     * Toggle shipping cost active status (only one can be active at a time)
     */
    ShippingCostDTO toggleShippingCostStatus(Long id);
    ShippingCostDTO toggleShippingCostStatus(Long id, UUID shopId);
}
