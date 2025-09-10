package com.ecommerce.service;

import com.ecommerce.dto.CreateShippingCostDTO;
import com.ecommerce.dto.ShippingCostDTO;
import com.ecommerce.dto.UpdateShippingCostDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingCostService {

    /**
     * Create a new shipping cost configuration
     */
    ShippingCostDTO createShippingCost(CreateShippingCostDTO createShippingCostDTO);

    /**
     * Get all shipping costs with pagination
     */
    Page<ShippingCostDTO> getAllShippingCosts(Pageable pageable);

    /**
     * Get all active shipping costs
     */
    List<ShippingCostDTO> getActiveShippingCosts();

    /**
     * Get shipping cost by ID
     */
    ShippingCostDTO getShippingCostById(Long id);

    /**
     * Update shipping cost
     */
    ShippingCostDTO updateShippingCost(Long id, UpdateShippingCostDTO updateShippingCostDTO);

    /**
     * Delete shipping cost
     */
    void deleteShippingCost(Long id);

    /**
     * Search shipping costs by name
     */
    Page<ShippingCostDTO> searchShippingCosts(String name, Pageable pageable);

    /**
     * Calculate shipping cost for given parameters
     */
    BigDecimal calculateShippingCost(BigDecimal weight, BigDecimal distance, BigDecimal orderValue);

    /**
     * Calculate shipping cost for order with address and items
     */
    BigDecimal calculateOrderShippingCost(com.ecommerce.dto.AddressDto deliveryAddress,
            java.util.List<com.ecommerce.dto.CartItemDTO> items, BigDecimal orderValue);

    /**
     * Calculate shipping details including distance and cost breakdown
     */
    com.ecommerce.dto.ShippingDetailsDTO calculateShippingDetails(com.ecommerce.dto.AddressDto deliveryAddress,
            java.util.List<com.ecommerce.dto.CartItemDTO> items, BigDecimal orderValue);

    /**
     * Toggle shipping cost active status (only one can be active at a time)
     */
    ShippingCostDTO toggleShippingCostStatus(Long id);
}
