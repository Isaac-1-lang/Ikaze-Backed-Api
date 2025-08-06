package com.ecommerce.service;

import com.ecommerce.dto.DeliveryAreaDTO;
import com.ecommerce.entity.DeliveryArea;

import java.util.List;

public interface DeliveryAreaService {
    
    /**
     * Create a new delivery area
     * 
     * @param deliveryAreaDTO the delivery area data
     * @return the created delivery area
     */
    DeliveryAreaDTO createDeliveryArea(DeliveryAreaDTO deliveryAreaDTO);
    
    /**
     * Update an existing delivery area
     * 
     * @param id the ID of the delivery area to update
     * @param deliveryAreaDTO the updated delivery area data
     * @return the updated delivery area
     */
    DeliveryAreaDTO updateDeliveryArea(Long id, DeliveryAreaDTO deliveryAreaDTO);
    
    /**
     * Delete a delivery area
     * 
     * @param id the ID of the delivery area to delete
     */
    void deleteDeliveryArea(Long id);
    
    /**
     * Get a delivery area by ID
     * 
     * @param id the ID of the delivery area
     * @return the delivery area
     */
    DeliveryAreaDTO getDeliveryAreaById(Long id);
    
    /**
     * Get all delivery areas
     * 
     * @return list of all delivery areas
     */
    List<DeliveryAreaDTO> getAllDeliveryAreas();
    
    /**
     * Get all top-level delivery areas
     * 
     * @return list of top-level delivery areas
     */
    List<DeliveryAreaDTO> getTopLevelDeliveryAreas();
    
    /**
     * Get all sub-areas of a given parent delivery area
     * 
     * @param parentId the ID of the parent delivery area
     * @return list of child delivery areas
     */
    List<DeliveryAreaDTO> getSubAreas(Long parentId);
    
    /**
     * Convert a DeliveryArea entity to a DeliveryAreaDTO
     * 
     * @param deliveryArea the delivery area entity
     * @return the delivery area DTO
     */
    DeliveryAreaDTO convertToDTO(DeliveryArea deliveryArea);
    
    /**
     * Convert a DeliveryAreaDTO to a DeliveryArea entity
     * 
     * @param deliveryAreaDTO the delivery area DTO
     * @return the delivery area entity
     */
    DeliveryArea convertToEntity(DeliveryAreaDTO deliveryAreaDTO);
}