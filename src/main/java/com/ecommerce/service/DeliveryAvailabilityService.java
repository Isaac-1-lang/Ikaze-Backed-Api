package com.ecommerce.service;

import com.ecommerce.entity.Warehouse;
import com.ecommerce.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for checking delivery availability based on warehouse locations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAvailabilityService {

    private final WarehouseRepository warehouseRepository;

    /**
     * Check if delivery is available to a specific country
     * 
     * @param country The country name to check
     * @return true if delivery is available, false otherwise
     */
    public boolean isDeliveryAvailable(String country) {
        if (country == null || country.trim().isEmpty()) {
            return false;
        }

        try {
            // Normalize country name for comparison
            String normalizedCountry = normalizeCountryName(country);
            
            // Check if there are any active warehouses in the specified country
            List<Warehouse> warehouses = warehouseRepository.findByCountryIgnoreCaseAndIsActiveTrue(normalizedCountry);
            
            boolean isAvailable = !warehouses.isEmpty();
            
            log.debug("Delivery availability check - Country: {}, Available: {}, Warehouse count: {}", 
                    normalizedCountry, isAvailable, warehouses.size());
            
            return isAvailable;
            
        } catch (Exception e) {
            log.error("Error checking delivery availability for country {}: {}", country, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the number of warehouses in a specific country
     * 
     * @param country The country name
     * @return Number of active warehouses in the country
     */
    public int getWarehouseCountInCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            return 0;
        }

        try {
            String normalizedCountry = normalizeCountryName(country);
            List<Warehouse> warehouses = warehouseRepository.findByCountryIgnoreCaseAndIsActiveTrue(normalizedCountry);
            return warehouses.size();
            
        } catch (Exception e) {
            log.error("Error getting warehouse count for country {}: {}", country, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get list of all countries where delivery is available
     * 
     * @return Set of country names where delivery is available
     */
    public Set<String> getAvailableCountries() {
        try {
            List<Warehouse> activeWarehouses = warehouseRepository.findByIsActiveTrue();
            
            Set<String> countries = activeWarehouses.stream()
                    .map(Warehouse::getCountry)
                    .filter(country -> country != null && !country.trim().isEmpty())
                    .map(this::normalizeCountryName)
                    .collect(Collectors.toSet());
            
            log.debug("Found {} countries with delivery availability: {}", countries.size(), countries);
            
            return countries;
            
        } catch (Exception e) {
            log.error("Error fetching available countries: {}", e.getMessage(), e);
            return Set.of();
        }
    }

    /**
     * Get detailed warehouse information for a specific country
     * 
     * @param country The country name
     * @return List of warehouses in the country
     */
    public List<Warehouse> getWarehousesInCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            return List.of();
        }

        try {
            String normalizedCountry = normalizeCountryName(country);
            return warehouseRepository.findByCountryIgnoreCaseAndIsActiveTrue(normalizedCountry);
            
        } catch (Exception e) {
            log.error("Error fetching warehouses for country {}: {}", country, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Normalize country name for consistent comparison
     * Handles common variations and formatting
     * 
     * @param country Raw country name
     * @return Normalized country name
     */
    private String normalizeCountryName(String country) {
        if (country == null) {
            return "";
        }

        // Basic normalization
        String normalized = country.trim();
        
        // Handle common country name variations
        switch (normalized.toLowerCase()) {
            case "usa":
            case "united states":
            case "united states of america":
            case "us":
                return "United States";
                
            case "uk":
            case "united kingdom":
            case "great britain":
            case "britain":
                return "United Kingdom";
                
            case "uae":
            case "united arab emirates":
                return "United Arab Emirates";
                
            case "south korea":
            case "republic of korea":
                return "South Korea";
                
            case "north korea":
            case "democratic people's republic of korea":
                return "North Korea";
                
            default:
                // Capitalize first letter of each word
                return capitalizeWords(normalized);
        }
    }

    /**
     * Capitalize the first letter of each word in a string
     * 
     * @param input Input string
     * @return Capitalized string
     */
    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        
        return result.toString();
    }
}
