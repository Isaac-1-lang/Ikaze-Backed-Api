package com.ecommerce.service.impl;

import com.ecommerce.dto.AddressDto;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.CreateShippingCostDTO;
import com.ecommerce.dto.ShippingCostDTO;
import com.ecommerce.dto.UpdateShippingCostDTO;
import com.ecommerce.entity.ShippingCost;
import com.ecommerce.entity.Warehouse;
import com.ecommerce.repository.ShippingCostRepository;
import com.ecommerce.repository.WarehouseRepository;
import com.ecommerce.service.GeocodingService;
import com.ecommerce.service.ShippingCostService;
import com.ecommerce.util.DistanceCalculator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingCostServiceImpl implements ShippingCostService {

    private final ShippingCostRepository shippingCostRepository;
    private final WarehouseRepository warehouseRepository;
    private final GeocodingService geocodingService;

    @Override
    @Transactional
    public ShippingCostDTO createShippingCost(CreateShippingCostDTO createShippingCostDTO) {
        log.info("Creating shipping cost: {}", createShippingCostDTO.getName());

        // Check if name already exists
        if (shippingCostRepository.existsByName(createShippingCostDTO.getName())) {
            throw new IllegalArgumentException(
                    "Shipping cost with name '" + createShippingCostDTO.getName() + "' already exists");
        }

        ShippingCost shippingCost = ShippingCost.builder()
                .name(createShippingCostDTO.getName())
                .description(createShippingCostDTO.getDescription())
                .distanceKmCost(createShippingCostDTO.getDistanceKmCost())
                .weightKgCost(createShippingCostDTO.getWeightKgCost())
                .baseFee(createShippingCostDTO.getBaseFee())
                .internationalFee(createShippingCostDTO.getInternationalFee())
                .freeShippingThreshold(createShippingCostDTO.getFreeShippingThreshold())
                .isActive(createShippingCostDTO.getIsActive())
                .build();

        ShippingCost savedShippingCost = shippingCostRepository.save(shippingCost);
        log.info("Successfully created shipping cost with ID: {}", savedShippingCost.getId());

        return mapToDTO(savedShippingCost);
    }

    @Override
    public Page<ShippingCostDTO> getAllShippingCosts(Pageable pageable) {
        log.info("Fetching all shipping costs with pagination");
        Page<ShippingCost> shippingCosts = shippingCostRepository.findAll(pageable);
        return shippingCosts.map(this::mapToDTO);
    }

    @Override
    public List<ShippingCostDTO> getActiveShippingCosts() {
        log.info("Fetching all active shipping costs");
        List<ShippingCost> shippingCosts = shippingCostRepository.findByIsActiveTrue();
        return shippingCosts.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public ShippingCostDTO getShippingCostById(Long id) {
        log.info("Fetching shipping cost by ID: {}", id);
        ShippingCost shippingCost = shippingCostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipping cost not found with ID: " + id));
        return mapToDTO(shippingCost);
    }

    @Override
    @Transactional
    public ShippingCostDTO updateShippingCost(Long id, UpdateShippingCostDTO updateShippingCostDTO) {
        log.info("Updating shipping cost with ID: {}", id);

        ShippingCost shippingCost = shippingCostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipping cost not found with ID: " + id));

        // Check if name already exists (excluding current ID)
        if (updateShippingCostDTO.getName() != null &&
                shippingCostRepository.existsByNameAndIdNot(updateShippingCostDTO.getName(), id)) {
            throw new IllegalArgumentException(
                    "Shipping cost with name '" + updateShippingCostDTO.getName() + "' already exists");
        }

        // Update fields if provided
        if (updateShippingCostDTO.getName() != null) {
            shippingCost.setName(updateShippingCostDTO.getName());
        }
        if (updateShippingCostDTO.getDescription() != null) {
            shippingCost.setDescription(updateShippingCostDTO.getDescription());
        }
        if (updateShippingCostDTO.getDistanceKmCost() != null) {
            shippingCost.setDistanceKmCost(updateShippingCostDTO.getDistanceKmCost());
        }
        if (updateShippingCostDTO.getWeightKgCost() != null) {
            shippingCost.setWeightKgCost(updateShippingCostDTO.getWeightKgCost());
        }
        if (updateShippingCostDTO.getBaseFee() != null) {
            shippingCost.setBaseFee(updateShippingCostDTO.getBaseFee());
        }
        if (updateShippingCostDTO.getInternationalFee() != null) {
            shippingCost.setInternationalFee(updateShippingCostDTO.getInternationalFee());
        }

        if (updateShippingCostDTO.getFreeShippingThreshold() != null) {
            shippingCost.setFreeShippingThreshold(updateShippingCostDTO.getFreeShippingThreshold());
        }

        if (updateShippingCostDTO.getIsActive() != null) {
            shippingCost.setIsActive(updateShippingCostDTO.getIsActive());
        }
        ShippingCost updatedShippingCost = shippingCostRepository.save(shippingCost);
        log.info("Successfully updated shipping cost with ID: {}", updatedShippingCost.getId());

        return mapToDTO(updatedShippingCost);
    }

    @Override
    @Transactional
    public void deleteShippingCost(Long id) {
        log.info("Deleting shipping cost with ID: {}", id);

        if (!shippingCostRepository.existsById(id)) {
            throw new EntityNotFoundException("Shipping cost not found with ID: " + id);
        }

        shippingCostRepository.deleteById(id);
        log.info("Successfully deleted shipping cost with ID: {}", id);
    }

    @Override
    public Page<ShippingCostDTO> searchShippingCosts(String name, Pageable pageable) {
        log.info("Searching shipping costs by name: {}", name);
        Page<ShippingCost> shippingCosts = shippingCostRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(name,
                pageable);
        return shippingCosts.map(this::mapToDTO);
    }

    @Override
    public BigDecimal calculateShippingCost(BigDecimal weight, BigDecimal distance, BigDecimal orderValue) {
        log.info("Calculating shipping cost for weight: {}, distance: {}, orderValue: {}",
                weight, distance, orderValue);

        // Find the active shipping cost configuration
        List<ShippingCost> shippingCosts = shippingCostRepository.findByIsActiveTrue();

        if (shippingCosts.isEmpty()) {
            log.warn("No active shipping cost configuration found");
            return BigDecimal.ZERO;
        }

        // Use the first active shipping cost configuration
        ShippingCost shippingCost = shippingCosts.get(0);

        // Check if order qualifies for free shipping
        if (shippingCost.getFreeShippingThreshold() != null &&
                orderValue != null &&
                orderValue.compareTo(shippingCost.getFreeShippingThreshold()) >= 0) {
            log.info("Order qualifies for free shipping");
            return BigDecimal.ZERO;
        }

        // Calculate shipping cost
        BigDecimal totalCost = BigDecimal.ZERO;

        // Add base fee
        if (shippingCost.getBaseFee() != null) {
            totalCost = totalCost.add(shippingCost.getBaseFee());
        }

        // Add distance-based cost
        if (shippingCost.getDistanceKmCost() != null && distance != null) {
            BigDecimal distanceCost = shippingCost.getDistanceKmCost().multiply(distance);
            totalCost = totalCost.add(distanceCost);
        }

        // Add weight-based cost
        if (shippingCost.getWeightKgCost() != null && weight != null) {
            BigDecimal weightCost = shippingCost.getWeightKgCost().multiply(weight);
            totalCost = totalCost.add(weightCost);
        }

        // Add international fee if applicable
        if (shippingCost.getInternationalFee() != null) {
            totalCost = totalCost.add(shippingCost.getInternationalFee());
        }

        log.info("Calculated shipping cost: {}", totalCost);
        return totalCost;
    }

    @Override
    public BigDecimal calculateOrderShippingCost(AddressDto deliveryAddress, List<CartItemDTO> items,
            BigDecimal orderValue) {
        log.info("=== SHIPPING COST CALCULATION START ===");
        log.info("Delivery Address: {} {}, {}, {}",
                deliveryAddress.getStreetAddress(),
                deliveryAddress.getCity(),
                deliveryAddress.getState(),
                deliveryAddress.getCountry());
        log.info("Order Value: {}, Items Count: {}", orderValue, items.size());

        // Step 0: Check for free shipping threshold
        List<ShippingCost> activeShippingCosts = shippingCostRepository.findByIsActiveTrue();
        if (activeShippingCosts.isEmpty()) {
            log.warn("No active shipping cost configuration found");
            return BigDecimal.ZERO;
        }

        ShippingCost shippingCost = activeShippingCosts.get(0);
        log.info("Using shipping cost configuration: {} (ID: {})", shippingCost.getName(), shippingCost.getId());
        log.info("Free shipping threshold: {}, Base fee: {}, Distance cost: {}, Weight cost: {}, International fee: {}",
                shippingCost.getFreeShippingThreshold(),
                shippingCost.getBaseFee(),
                shippingCost.getDistanceKmCost(),
                shippingCost.getWeightKgCost(),
                shippingCost.getInternationalFee());

        if (shippingCost.getFreeShippingThreshold() != null &&
                shippingCost.getFreeShippingThreshold().compareTo(BigDecimal.ZERO) > 0 &&
                orderValue != null &&
                orderValue.compareTo(shippingCost.getFreeShippingThreshold()) >= 0) {
            log.info("Order qualifies for free shipping (threshold: {}, order value: {})",
                    shippingCost.getFreeShippingThreshold(), orderValue);
            return BigDecimal.ZERO;
        }

        // Step 1: Get all warehouses
        List<Warehouse> warehouses = warehouseRepository.findAll();
        if (warehouses.isEmpty()) {
            log.error("No warehouses available in the system");
            throw new RuntimeException("No warehouses available");
        }

        log.info("Total warehouses available: {}", warehouses.size());
        warehouses.forEach(w -> log.info("Warehouse: {} - {} ({}, {})",
                w.getName(), w.getCountry(), w.getLatitude(), w.getLongitude()));

        // Step 2: Check if we have warehouses in the delivery country
        List<Warehouse> countryWarehouses = warehouses.stream()
                .filter(w -> w.getCountry().equalsIgnoreCase(deliveryAddress.getCountry()))
                .collect(Collectors.toList());

        log.info("Warehouses in delivery country '{}': {}", deliveryAddress.getCountry(), countryWarehouses.size());

        Warehouse selectedWarehouse;
        boolean isInternational = false;

        if (countryWarehouses.isEmpty()) {
            // No warehouse in delivery country - use nearest warehouse (international
            // shipping)
            log.info("No warehouse in delivery country. Finding nearest warehouse for international shipping.");
            selectedWarehouse = warehouses.stream()
                    .min(Comparator.comparingDouble(w -> DistanceCalculator.getDistanceFromLatLonInKm(
                            geocodingService.getCoordinates(deliveryAddress).get("latitude"),
                            geocodingService.getCoordinates(deliveryAddress).get("longitude"),
                            w.getLatitude().doubleValue(),
                            w.getLongitude().doubleValue())))
                    .orElse(warehouses.get(0));
            isInternational = true;
            log.info("Selected nearest warehouse for international shipping: {} ({})",
                    selectedWarehouse.getName(), selectedWarehouse.getCountry());
        } else {
            // Use nearest warehouse in the same country
            selectedWarehouse = countryWarehouses.stream()
                    .min(Comparator.comparingDouble(w -> DistanceCalculator.getDistanceFromLatLonInKm(
                            geocodingService.getCoordinates(deliveryAddress).get("latitude"),
                            geocodingService.getCoordinates(deliveryAddress).get("longitude"),
                            w.getLatitude().doubleValue(),
                            w.getLongitude().doubleValue())))
                    .orElse(countryWarehouses.get(0));
            log.info("Selected nearest warehouse in same country: {} ({})",
                    selectedWarehouse.getName(), selectedWarehouse.getCountry());
        }

        // Step 3: Get customer coordinates
        Map<String, Double> customerCoords = geocodingService.getCoordinates(deliveryAddress);
        if (customerCoords == null) {
            log.error("Could not determine customer location for address: {}", deliveryAddress);
            throw new RuntimeException("Could not determine customer location");
        }

        double customerLat = customerCoords.get("latitude");
        double customerLon = customerCoords.get("longitude");
        log.info("Customer coordinates: ({}, {})", customerLat, customerLon);
        log.info("Warehouse coordinates: ({}, {})",
                selectedWarehouse.getLatitude().doubleValue(),
                selectedWarehouse.getLongitude().doubleValue());

        // Step 4: Calculate distance
        double distanceKm = DistanceCalculator.getDistanceFromLatLonInKm(
                customerLat, customerLon,
                selectedWarehouse.getLatitude().doubleValue(),
                selectedWarehouse.getLongitude().doubleValue());
        log.info("Distance between customer and warehouse: {} km", distanceKm);

        // Step 5: Calculate total weight
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (CartItemDTO item : items) {
            BigDecimal itemWeight = item.getWeight() != null ? item.getWeight() : BigDecimal.ZERO;
            BigDecimal itemTotalWeight = itemWeight.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalWeight = totalWeight.add(itemTotalWeight);
            log.info("Item: {} - Weight: {} kg × {} = {} kg",
                    item.getProductName(), itemWeight, item.getQuantity(), itemTotalWeight);
        }
        log.info("Total order weight: {} kg", totalWeight);

        // Step 6: Calculate shipping cost components
        BigDecimal shippingCostTotal = BigDecimal.ZERO;

        // Base fee
        if (shippingCost.getBaseFee() != null) {
            shippingCostTotal = shippingCostTotal.add(shippingCost.getBaseFee());
            log.info("Base fee: {}", shippingCost.getBaseFee());
        }

        // Distance cost
        if (shippingCost.getDistanceKmCost() != null) {
            BigDecimal distanceCost = shippingCost.getDistanceKmCost().multiply(BigDecimal.valueOf(distanceKm));
            shippingCostTotal = shippingCostTotal.add(distanceCost);
            log.info("Distance cost: {} × {} km = {}",
                    shippingCost.getDistanceKmCost(), distanceKm, distanceCost);
        }

        // Weight cost
        if (shippingCost.getWeightKgCost() != null && totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal weightCost = shippingCost.getWeightKgCost().multiply(totalWeight);
            shippingCostTotal = shippingCostTotal.add(weightCost);
            log.info("Weight cost: {} × {} kg = {}",
                    shippingCost.getWeightKgCost(), totalWeight, weightCost);
        }

        // International fee
        if (isInternational && shippingCost.getInternationalFee() != null) {
            shippingCostTotal = shippingCostTotal.add(shippingCost.getInternationalFee());
            log.info("International fee: {} (shipping from {} to {})",
                    shippingCost.getInternationalFee(),
                    selectedWarehouse.getCountry(),
                    deliveryAddress.getCountry());
        }

        log.info("=== SHIPPING COST CALCULATION RESULT ===");
        log.info("Final shipping cost: {} (distance: {} km, weight: {} kg, international: {})",
                shippingCostTotal, distanceKm, totalWeight, isInternational);
        log.info("=== SHIPPING COST CALCULATION END ===");

        return shippingCostTotal;
    }

    @Override
    public com.ecommerce.dto.ShippingDetailsDTO calculateShippingDetails(AddressDto deliveryAddress, 
            List<CartItemDTO> items, BigDecimal orderValue) {
        log.info("=== SHIPPING DETAILS CALCULATION START ===");
        log.info("Delivery Address: {} {}, {}, {}", 
                deliveryAddress.getStreetAddress(), 
                deliveryAddress.getCity(), 
                deliveryAddress.getState(), 
                deliveryAddress.getCountry());
        log.info("Order Value: {}, Items Count: {}", orderValue, items.size());

        List<ShippingCost> activeShippingCosts = shippingCostRepository.findByIsActiveTrue();
        if (activeShippingCosts.isEmpty()) {
            log.warn("No active shipping cost configuration found");
            return com.ecommerce.dto.ShippingDetailsDTO.builder()
                    .shippingCost(BigDecimal.ZERO)
                    .distanceKm(0.0)
                    .costPerKm(BigDecimal.ZERO)
                    .selectedWarehouseName("No warehouse")
                    .selectedWarehouseCountry("Unknown")
                    .isInternationalShipping(false)
                    .baseFee(BigDecimal.ZERO)
                    .distanceCost(BigDecimal.ZERO)
                    .weightCost(BigDecimal.ZERO)
                    .internationalFee(BigDecimal.ZERO)
                    .totalWeight(BigDecimal.ZERO)
                    .build();
        }

        ShippingCost shippingCost = activeShippingCosts.get(0);
        log.info("Using shipping cost configuration: {} (ID: {})", shippingCost.getName(), shippingCost.getId());

        // Check for free shipping
        if (shippingCost.getFreeShippingThreshold() != null &&
                shippingCost.getFreeShippingThreshold().compareTo(BigDecimal.ZERO) > 0 &&
                orderValue != null &&
                orderValue.compareTo(shippingCost.getFreeShippingThreshold()) >= 0) {
            log.info("Order qualifies for free shipping (threshold: {}, order value: {})", 
                    shippingCost.getFreeShippingThreshold(), orderValue);
            return com.ecommerce.dto.ShippingDetailsDTO.builder()
                    .shippingCost(BigDecimal.ZERO)
                    .distanceKm(0.0)
                    .costPerKm(BigDecimal.ZERO)
                    .selectedWarehouseName("Free shipping")
                    .selectedWarehouseCountry("N/A")
                    .isInternationalShipping(false)
                    .baseFee(BigDecimal.ZERO)
                    .distanceCost(BigDecimal.ZERO)
                    .weightCost(BigDecimal.ZERO)
                    .internationalFee(BigDecimal.ZERO)
                    .totalWeight(BigDecimal.ZERO)
                    .build();
        }

        List<Warehouse> warehouses = warehouseRepository.findAll();
        if (warehouses.isEmpty()) {
            log.error("No warehouses available in the system");
            throw new RuntimeException("No warehouses available");
        }

        List<Warehouse> countryWarehouses = warehouses.stream()
                .filter(w -> w.getCountry().equalsIgnoreCase(deliveryAddress.getCountry()))
                .collect(Collectors.toList());

        Warehouse selectedWarehouse;
        boolean isInternational = false;

        if (countryWarehouses.isEmpty()) {
            log.info("No warehouse in delivery country. Finding nearest warehouse for international shipping.");
            selectedWarehouse = warehouses.stream()
                    .min(Comparator.comparingDouble(w -> DistanceCalculator.getDistanceFromLatLonInKm(
                            geocodingService.getCoordinates(deliveryAddress).get("latitude"),
                            geocodingService.getCoordinates(deliveryAddress).get("longitude"),
                            w.getLatitude().doubleValue(),
                            w.getLongitude().doubleValue())))
                    .orElse(warehouses.get(0));
            isInternational = true;
            log.info("Selected nearest warehouse for international shipping: {} ({})", 
                    selectedWarehouse.getName(), selectedWarehouse.getCountry());
        } else {
            selectedWarehouse = countryWarehouses.stream()
                    .min(Comparator.comparingDouble(w -> DistanceCalculator.getDistanceFromLatLonInKm(
                            geocodingService.getCoordinates(deliveryAddress).get("latitude"),
                            geocodingService.getCoordinates(deliveryAddress).get("longitude"),
                            w.getLatitude().doubleValue(),
                            w.getLongitude().doubleValue())))
                    .orElse(countryWarehouses.get(0));
            log.info("Selected nearest warehouse in same country: {} ({})", 
                    selectedWarehouse.getName(), selectedWarehouse.getCountry());
        }

        Map<String, Double> customerCoords = geocodingService.getCoordinates(deliveryAddress);
        if (customerCoords == null) {
            log.error("Could not determine customer location for address: {}", deliveryAddress);
            throw new RuntimeException("Could not determine customer location");
        }

        double customerLat = customerCoords.get("latitude");
        double customerLon = customerCoords.get("longitude");
        log.info("Customer coordinates: ({}, {})", customerLat, customerLon);
        log.info("Warehouse coordinates: ({}, {})", 
                selectedWarehouse.getLatitude().doubleValue(), 
                selectedWarehouse.getLongitude().doubleValue());

        double distanceKm = DistanceCalculator.getDistanceFromLatLonInKm(
                customerLat, customerLon,
                selectedWarehouse.getLatitude().doubleValue(),
                selectedWarehouse.getLongitude().doubleValue());
        log.info("Distance between customer and warehouse: {} km", distanceKm);

        BigDecimal totalWeight = BigDecimal.ZERO;
        for (CartItemDTO item : items) {
            BigDecimal itemWeight = item.getWeight() != null ? item.getWeight() : BigDecimal.ZERO;
            BigDecimal itemTotalWeight = itemWeight.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalWeight = totalWeight.add(itemTotalWeight);
            log.info("Item: {} - Weight: {} kg × {} = {} kg", 
                    item.getProductName(), itemWeight, item.getQuantity(), itemTotalWeight);
        }
        log.info("Total order weight: {} kg", totalWeight);

        // Calculate cost components
        BigDecimal baseFee = shippingCost.getBaseFee() != null ? shippingCost.getBaseFee() : BigDecimal.ZERO;
        BigDecimal distanceCost = BigDecimal.ZERO;
        BigDecimal weightCost = BigDecimal.ZERO;
        BigDecimal internationalFee = BigDecimal.ZERO;

        if (shippingCost.getDistanceKmCost() != null) {
            distanceCost = shippingCost.getDistanceKmCost().multiply(BigDecimal.valueOf(distanceKm));
            log.info("Distance cost: {} × {} km = {}", 
                    shippingCost.getDistanceKmCost(), distanceKm, distanceCost);
        }

        if (shippingCost.getWeightKgCost() != null && totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            weightCost = shippingCost.getWeightKgCost().multiply(totalWeight);
            log.info("Weight cost: {} × {} kg = {}", 
                    shippingCost.getWeightKgCost(), totalWeight, weightCost);
        }

        if (isInternational && shippingCost.getInternationalFee() != null) {
            internationalFee = shippingCost.getInternationalFee();
            log.info("International fee: {} (shipping from {} to {})", 
                    shippingCost.getInternationalFee(), 
                    selectedWarehouse.getCountry(), 
                    deliveryAddress.getCountry());
        }

        BigDecimal shippingCostTotal = baseFee.add(distanceCost).add(weightCost).add(internationalFee);

        log.info("=== SHIPPING DETAILS CALCULATION RESULT ===");
        log.info("Final shipping cost: {} (distance: {} km, weight: {} kg, international: {})",
                shippingCostTotal, distanceKm, totalWeight, isInternational);
        log.info("=== SHIPPING DETAILS CALCULATION END ===");

        return com.ecommerce.dto.ShippingDetailsDTO.builder()
                .shippingCost(shippingCostTotal)
                .distanceKm(distanceKm)
                .costPerKm(shippingCost.getDistanceKmCost())
                .selectedWarehouseName(selectedWarehouse.getName())
                .selectedWarehouseCountry(selectedWarehouse.getCountry())
                .isInternationalShipping(isInternational)
                .baseFee(baseFee)
                .distanceCost(distanceCost)
                .weightCost(weightCost)
                .internationalFee(internationalFee)
                .totalWeight(totalWeight)
                .build();
    }

    @Override
    @Transactional
    public ShippingCostDTO toggleShippingCostStatus(Long id) {
        log.info("Toggling shipping cost status for ID: {}", id);

        ShippingCost shippingCost = shippingCostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipping cost not found with id: " + id));

        // If the shipping cost is currently active, deactivate it
        if (shippingCost.getIsActive()) {
            shippingCost.setIsActive(false);
            log.info("Deactivated shipping cost: {}", shippingCost.getName());
        } else {
            // If it's inactive, first deactivate all others, then activate this one
            shippingCostRepository.deactivateAllShippingCosts();
            shippingCost.setIsActive(true);
            log.info("Activated shipping cost: {} and deactivated all others", shippingCost.getName());
        }

        ShippingCost savedShippingCost = shippingCostRepository.save(shippingCost);
        return mapToDTO(savedShippingCost);
    }

    private ShippingCostDTO mapToDTO(ShippingCost shippingCost) {
        return ShippingCostDTO.builder()
                .id(shippingCost.getId())
                .name(shippingCost.getName())
                .description(shippingCost.getDescription())
                .distanceKmCost(shippingCost.getDistanceKmCost())
                .weightKgCost(shippingCost.getWeightKgCost())
                .baseFee(shippingCost.getBaseFee())
                .internationalFee(shippingCost.getInternationalFee())
                .freeShippingThreshold(shippingCost.getFreeShippingThreshold())
                .isActive(shippingCost.getIsActive())
                .createdAt(shippingCost.getCreatedAt())
                .updatedAt(shippingCost.getUpdatedAt())
                .build();
    }
}
