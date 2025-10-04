package com.ecommerce.service;

import com.ecommerce.dto.ReturnPickupRequestDTO;
import com.ecommerce.dto.ReturnPickupResponseDTO;

import java.util.UUID;

/**
 * Service interface for handling return pickup operations
 */
public interface ReturnPickupService {

    /**
     * Process return pickup by delivery agent
     * 
     * @param pickupRequest the pickup request containing return items and their statuses
     * @param deliveryAgentId the ID of the delivery agent performing the pickup
     * @return pickup response with processing results
     * @throws RuntimeException if validation fails or processing errors occur
     */
    ReturnPickupResponseDTO processReturnPickup(ReturnPickupRequestDTO pickupRequest, UUID deliveryAgentId);

    /**
     * Validate return pickup request
     * 
     * @param pickupRequest the pickup request to validate
     * @param deliveryAgentId the ID of the delivery agent
     * @throws RuntimeException if validation fails
     */
    void validateReturnPickup(ReturnPickupRequestDTO pickupRequest, UUID deliveryAgentId);
}
