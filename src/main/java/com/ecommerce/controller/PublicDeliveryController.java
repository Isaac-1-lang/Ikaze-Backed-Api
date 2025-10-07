package com.ecommerce.controller;

import com.ecommerce.service.DeliveryAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Public controller for delivery availability checks
 * No authentication required
 */
@RestController
@RequestMapping("/api/v1/public/delivery")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class PublicDeliveryController {

    private final DeliveryAvailabilityService deliveryAvailabilityService;

    /**
     * Check if delivery is available to a specific country
     * 
     * @param country The country name to check
     * @return Delivery availability status
     */
    @GetMapping("/check-availability")
    public ResponseEntity<Map<String, Object>> checkDeliveryAvailability(
            @RequestParam String country) {
        
        log.info("Checking delivery availability for country: {}", country);
        
        try {
            boolean isAvailable = deliveryAvailabilityService.isDeliveryAvailable(country);
            int warehouseCount = deliveryAvailabilityService.getWarehouseCountInCountry(country);
            
            Map<String, Object> response = new HashMap<>();
            response.put("available", isAvailable);
            response.put("country", country);
            response.put("warehouseCount", warehouseCount);
            
            if (isAvailable) {
                response.put("message", String.format("We deliver to %s! We have %d warehouse%s in your area.", 
                        country, warehouseCount, warehouseCount == 1 ? "" : "s"));
            } else {
                response.put("message", String.format("Sorry, we don't currently deliver to %s. We're working to expand our delivery network!", country));
            }
            
            log.info("Delivery availability check completed - Country: {}, Available: {}, Warehouses: {}", 
                    country, isAvailable, warehouseCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking delivery availability for country {}: {}", country, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("available", false);
            errorResponse.put("country", country);
            errorResponse.put("warehouseCount", 0);
            errorResponse.put("message", "Unable to check delivery availability at this time. Please try again later.");
            errorResponse.put("error", "Service temporarily unavailable");
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Get list of all countries where delivery is available
     * 
     * @return List of countries with delivery availability
     */
    @GetMapping("/available-countries")
    public ResponseEntity<Map<String, Object>> getAvailableCountries() {
        log.info("Fetching list of available delivery countries");
        
        try {
            var availableCountries = deliveryAvailabilityService.getAvailableCountries();
            
            Map<String, Object> response = new HashMap<>();
            response.put("countries", availableCountries);
            response.put("totalCountries", availableCountries.size());
            response.put("message", "Countries where delivery is available");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching available countries: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("countries", new String[0]);
            errorResponse.put("totalCountries", 0);
            errorResponse.put("message", "Unable to fetch available countries");
            errorResponse.put("error", "Service temporarily unavailable");
            
            return ResponseEntity.ok(errorResponse);
        }
    }
}
