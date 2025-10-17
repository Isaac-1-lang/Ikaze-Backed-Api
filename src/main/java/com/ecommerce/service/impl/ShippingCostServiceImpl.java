package com.ecommerce.service.impl;

import com.ecommerce.dto.AddressDto;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.CreateShippingCostDTO;
import com.ecommerce.dto.ShippingCostDTO;
import com.ecommerce.dto.UpdateShippingCostDTO;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductDetail;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.ShippingCost;
import com.ecommerce.entity.Warehouse;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.ShippingCostRepository;
import com.ecommerce.repository.WarehouseRepository;
import com.ecommerce.service.GeocodingService;
import com.ecommerce.service.ShippingCostService;
import com.ecommerce.service.EnhancedMultiWarehouseAllocator;
import com.ecommerce.service.FEFOStockAllocationService;
import com.ecommerce.util.DistanceCalculator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingCostServiceImpl implements ShippingCostService {

    private final ShippingCostRepository shippingCostRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final GeocodingService geocodingService;
    private final EnhancedMultiWarehouseAllocator enhancedWarehouseAllocator;

    @Override
    @Transactional
    public ShippingCostDTO createShippingCost(CreateShippingCostDTO createShippingCostDTO) {
        log.info("Creating shipping cost: {}", createShippingCostDTO.getName());

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

        if (updateShippingCostDTO.getName() != null &&
                shippingCostRepository.existsByNameAndIdNot(updateShippingCostDTO.getName(), id)) {
            throw new IllegalArgumentException(
                    "Shipping cost with name '" + updateShippingCostDTO.getName() + "' already exists");
        }

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

        List<ShippingCost> shippingCosts = shippingCostRepository.findByIsActiveTrue();

        if (shippingCosts.isEmpty()) {
            log.warn("No active shipping cost configuration found");
            return BigDecimal.ZERO;
        }

        ShippingCost shippingCost = shippingCosts.get(0);

        if (shippingCost.getFreeShippingThreshold() != null &&
                orderValue != null &&
                orderValue.compareTo(shippingCost.getFreeShippingThreshold()) >= 0) {
            log.info("Order qualifies for free shipping");
            return BigDecimal.ZERO;
        }

        BigDecimal totalCost = BigDecimal.ZERO;

        if (shippingCost.getBaseFee() != null) {
            totalCost = totalCost.add(shippingCost.getBaseFee());
        }

        if (shippingCost.getDistanceKmCost() != null && distance != null) {
            BigDecimal distanceCost = shippingCost.getDistanceKmCost().multiply(distance);
            totalCost = totalCost.add(distanceCost);
        }

        if (shippingCost.getWeightKgCost() != null && weight != null) {
            BigDecimal weightCost = shippingCost.getWeightKgCost().multiply(weight);
            totalCost = totalCost.add(weightCost);
        }

        if (shippingCost.getInternationalFee() != null) {
            totalCost = totalCost.add(shippingCost.getInternationalFee());
        }

        log.info("Calculated shipping cost: {}", totalCost);
        return totalCost;
    }

    @Override
    public BigDecimal calculateOrderShippingCost(AddressDto deliveryAddress, List<CartItemDTO> items,
            BigDecimal orderValue) {
        
        com.ecommerce.dto.ShippingDetailsDTO details = calculateEnhancedShippingDetails(deliveryAddress, items, orderValue);
        return details.getShippingCost();
    }

    @Override
    public com.ecommerce.dto.ShippingDetailsDTO calculateShippingDetails(AddressDto deliveryAddress,
            List<CartItemDTO> items, BigDecimal orderValue) {
        
        return calculateEnhancedShippingDetails(deliveryAddress, items, orderValue);
    }

    @Override
    public com.ecommerce.dto.ShippingDetailsDTO calculateEnhancedShippingDetails(AddressDto deliveryAddress,
            List<CartItemDTO> items, BigDecimal orderValue) {
        
        log.info("=== ENHANCED SHIPPING DETAILS CALCULATION START ===");
        log.info("Delivery Address: {} {}, {}, {}", deliveryAddress.getStreetAddress(),
                deliveryAddress.getCity(), deliveryAddress.getState(), deliveryAddress.getCountry());
        log.info("Order Value: {}, Items Count: {}", orderValue, items.size());

        // Step 1: Get active shipping configuration
        List<ShippingCost> activeShippingCosts = shippingCostRepository.findByIsActiveTrue();
        if (activeShippingCosts.isEmpty()) {
            log.warn("No active shipping cost configuration found");
            return buildEmptyShippingDetails();
        }

        ShippingCost shippingCost = activeShippingCosts.get(0);
        log.info("Using shipping cost configuration: {} (ID: {})", shippingCost.getName(), shippingCost.getId());

        // Step 2: Check for free shipping
        if (qualifiesForFreeShipping(shippingCost, orderValue)) {
            log.info("Order qualifies for free shipping (threshold: {}, order value: {})",
                    shippingCost.getFreeShippingThreshold(), orderValue);
            return buildFreeShippingDetails();
        }

        // Step 3: Get customer coordinates
        CustomerLocation customerLocation = getCustomerLocation(deliveryAddress);
        
        // Step 4: Allocate stock using FEFO across warehouses
        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> fefoAllocations;
        try {
            fefoAllocations = enhancedWarehouseAllocator.allocateStockWithFEFO(items, deliveryAddress);
        } catch (Exception e) {
            log.error("Failed to allocate stock with FEFO: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to allocate stock for shipping calculation");
        }

        // Step 5: Extract unique warehouses involved
        Set<Warehouse> involvedWarehouses = extractInvolvedWarehouses(fefoAllocations);
        
        if (involvedWarehouses.isEmpty()) {
            log.error("No warehouses available for stock allocation");
            throw new RuntimeException("No warehouses available for stock allocation");
        }

        involvedWarehouses.forEach(w -> {
            try {
                log.info("  - {} ({}, {})", w.getName(), w.getCountry(), w.getCity());
            } catch (Exception e) {
                log.info("  - Warehouse ID: {} (properties not accessible)", w.getId());
            }
        });

        DistanceCalculationResult distanceResult = calculateCumulativeDistance(
            involvedWarehouses, 
            customerLocation, 
            deliveryAddress.getCountry()
        );

        BigDecimal totalWeight = calculateTotalWeight(items);
        log.info("Total order weight: {} kg", totalWeight);

        CostComponents costs = calculateCostComponents(
            shippingCost, 
            distanceResult, 
            totalWeight
        );


        String furthestWarehouseName = "Unknown";
        String furthestWarehouseCountry = "Unknown";
        String hubWarehouseName = "Unknown";
        String hubWarehouseCountry = "Unknown";
        
        try {
            if (distanceResult.furthestWarehouse != null) {
                furthestWarehouseName = distanceResult.furthestWarehouse.getName();
                furthestWarehouseCountry = distanceResult.furthestWarehouse.getCountry();
            }
            if (distanceResult.hubWarehouse != null) {
                hubWarehouseName = distanceResult.hubWarehouse.getName();
                hubWarehouseCountry = distanceResult.hubWarehouse.getCountry();
            }
        } catch (Exception e) {
            log.warn("Error accessing warehouse properties: {}", e.getMessage());
        }

        com.ecommerce.dto.ShippingDetailsDTO shippingDetails = com.ecommerce.dto.ShippingDetailsDTO.builder()
                .shippingCost(costs.totalCost)
                .distanceKm(distanceResult.totalDistance)
                .costPerKm(shippingCost.getDistanceKmCost())
                .selectedWarehouseName(furthestWarehouseName)
                .selectedWarehouseCountry(furthestWarehouseCountry)
                .isInternationalShipping(distanceResult.isInternational)
                .baseFee(costs.baseFee)
                .distanceCost(costs.distanceCost)
                .weightCost(costs.weightCost)
                .internationalFee(costs.internationalFee)
                .totalWeight(totalWeight)
                .build();

        return shippingDetails;
    }

    @Override
    @Transactional
    public ShippingCostDTO toggleShippingCostStatus(Long id) {
        log.info("Toggling shipping cost status for ID: {}", id);

        ShippingCost shippingCost = shippingCostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipping cost not found with id: " + id));
        
        if (shippingCost.getIsActive()) {
            shippingCost.setIsActive(false);
            log.info("Deactivated shipping cost: {}", shippingCost.getName());
        } else {
            shippingCostRepository.deactivateAllShippingCosts();
            shippingCost.setIsActive(true);
            log.info("Activated shipping cost: {} and deactivated all others", shippingCost.getName());
        }

        ShippingCost savedShippingCost = shippingCostRepository.save(shippingCost);
        return mapToDTO(savedShippingCost);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Customer location holder
     */
    private static class CustomerLocation {
        double latitude;
        double longitude;

        CustomerLocation(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    /**
     * Distance calculation result holder
     */
    private static class DistanceCalculationResult {
        double totalDistance;
        Warehouse hubWarehouse;
        Warehouse furthestWarehouse;
        double maxWarehouseDistance;
        boolean isInternational;
    }

    /**
     * Cost components holder
     */
    private static class CostComponents {
        BigDecimal baseFee;
        BigDecimal distanceCost;
        BigDecimal weightCost;
        BigDecimal internationalFee;
        BigDecimal totalCost;
    }

    /**
     * Get customer location coordinates
     */
    private CustomerLocation getCustomerLocation(AddressDto deliveryAddress) {
        if (deliveryAddress.getLatitude() != null && deliveryAddress.getLongitude() != null) {
            log.info("Using provided coordinates: ({}, {})", 
                deliveryAddress.getLatitude(), deliveryAddress.getLongitude());
            return new CustomerLocation(deliveryAddress.getLatitude(), deliveryAddress.getLongitude());
        }

        log.info("No coordinates provided, using geocoding service");
        Map<String, Double> coords = geocodingService.getCoordinates(deliveryAddress);
        if (coords == null) {
            log.error("Could not determine customer location for address: {}", deliveryAddress);
            throw new RuntimeException("Could not determine customer location");
        }

        double lat = coords.get("latitude");
        double lon = coords.get("longitude");
        log.info("Geocoded coordinates: ({}, {})", lat, lon);
        return new CustomerLocation(lat, lon);
    }

    /**
     * Extract unique warehouses from FEFO allocations
     */
    private Set<Warehouse> extractInvolvedWarehouses(
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> fefoAllocations) {
        
        Set<Long> warehouseIds = new HashSet<>();
        
        // First, safely extract warehouse IDs
        for (List<FEFOStockAllocationService.BatchAllocation> allocations : fefoAllocations.values()) {
            for (FEFOStockAllocationService.BatchAllocation allocation : allocations) {
                try {
                    // Try to get the warehouse ID safely
                    Warehouse warehouse = allocation.getWarehouse();
                    if (warehouse != null) {
                        Long warehouseId = warehouse.getId();
                        if (warehouseId != null) {
                            warehouseIds.add(warehouseId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not access warehouse from allocation: {}", e.getMessage());
                }
            }
        }
        
        if (warehouseIds.isEmpty()) {
            log.error("No warehouse IDs could be extracted from allocations");
            throw new RuntimeException("No warehouse IDs available for shipping calculation");
        }
        
        // Fetch all warehouses in one query to ensure they're properly loaded
        List<Warehouse> warehouseList = warehouseRepository.findAllById(warehouseIds);
        Set<Warehouse> warehouses = new HashSet<>(warehouseList);
        
        if (warehouses.isEmpty()) {
            log.error("No warehouses could be loaded from IDs: {}", warehouseIds);
            throw new RuntimeException("No warehouses available for shipping calculation");
        }
        
        log.info("Successfully loaded {} warehouses for shipping calculation", warehouses.size());
        return warehouses;
    }

    /**
     * Calculate cumulative distance using hub warehouse logic
     * 
     * Logic:
     * 1. Find nearest warehouse to customer that has ANY products (hub warehouse)
     * 2. Calculate distance from each fulfilling warehouse to hub
     * 3. Calculate distance from hub to customer
     * 4. Sum all distances: Σ(warehouse_i → hub) + (hub → customer)
     */
    private DistanceCalculationResult calculateCumulativeDistance(
            Set<Warehouse> involvedWarehouses,
            CustomerLocation customerLocation,
            String customerCountry) {

        DistanceCalculationResult result = new DistanceCalculationResult();

        // Step 1: Find hub warehouse (nearest to customer)
        Warehouse hubWarehouse = null;
        double minDistanceToCustomer = Double.MAX_VALUE;

        for (Warehouse warehouse : involvedWarehouses) {
            double distance = DistanceCalculator.getDistanceFromLatLonInKm(
                customerLocation.latitude,
                customerLocation.longitude,
                warehouse.getLatitude().doubleValue(),
                warehouse.getLongitude().doubleValue()
            );

            try {
                log.info("Distance from {} to customer: {} km", warehouse.getName(), distance);
            } catch (Exception e) {
                log.info("Distance from warehouse ID {} to customer: {} km", warehouse.getId(), distance);
            }

            if (distance < minDistanceToCustomer) {
                minDistanceToCustomer = distance;
                hubWarehouse = warehouse;
            }
        }

        if (hubWarehouse == null) {
            hubWarehouse = involvedWarehouses.iterator().next();
            minDistanceToCustomer = DistanceCalculator.getDistanceFromLatLonInKm(
                customerLocation.latitude,
                customerLocation.longitude,
                hubWarehouse.getLatitude().doubleValue(),
                hubWarehouse.getLongitude().doubleValue()
            );
        }

        try {
            log.info("Hub warehouse selected: {} ({}) at {} km from customer", 
                hubWarehouse.getName(), hubWarehouse.getCountry(), minDistanceToCustomer);
        } catch (Exception e) {
            log.info("Hub warehouse selected: ID {} at {} km from customer", 
                hubWarehouse.getId(), minDistanceToCustomer);
        }

        result.hubWarehouse = hubWarehouse;

        // Step 2: Calculate distances from each warehouse to hub and find furthest
        double cumulativeWarehouseDistance = 0.0;
        Warehouse furthestWarehouse = hubWarehouse;
        double maxWarehouseToHubDistance = 0.0;
        boolean isInternational = false;

        for (Warehouse warehouse : involvedWarehouses) {
            // Check if international shipping
            try {
                if (!warehouse.getCountry().equalsIgnoreCase(customerCountry)) {
                    isInternational = true;
                }
            } catch (Exception e) {
                log.warn("Could not access warehouse country for international shipping check: {}", e.getMessage());
                // Assume international if we can't determine
                isInternational = true;
            }

            // Skip hub warehouse in warehouse-to-hub calculations
            if (warehouse.getId().equals(hubWarehouse.getId())) {
                continue;
            }

            double distanceToHub = DistanceCalculator.getDistanceFromLatLonInKm(
                warehouse.getLatitude().doubleValue(),
                warehouse.getLongitude().doubleValue(),
                hubWarehouse.getLatitude().doubleValue(),
                hubWarehouse.getLongitude().doubleValue()
            );

            try {
                log.info("Distance from {} to hub warehouse {}: {} km",
                    warehouse.getName(), hubWarehouse.getName(), distanceToHub);
            } catch (Exception e) {
                log.info("Distance from warehouse ID {} to hub warehouse ID {}: {} km",
                    warehouse.getId(), hubWarehouse.getId(), distanceToHub);
            }

            cumulativeWarehouseDistance += distanceToHub;

            // Track furthest warehouse
            if (distanceToHub > maxWarehouseToHubDistance) {
                maxWarehouseToHubDistance = distanceToHub;
                furthestWarehouse = warehouse;
            }
        }

        // Step 3: Calculate total distance
        // Total = Σ(warehouse → hub) + (hub → customer)
        result.totalDistance = cumulativeWarehouseDistance + minDistanceToCustomer;
        result.furthestWarehouse = furthestWarehouse;
        result.maxWarehouseDistance = maxWarehouseToHubDistance;
        result.isInternational = isInternational;

        log.info("Distance breakdown:");
        log.info("  - Cumulative warehouse-to-hub distances: {} km", cumulativeWarehouseDistance);
        log.info("  - Hub to customer: {} km", minDistanceToCustomer);
        log.info("  - TOTAL DISTANCE: {} km", result.totalDistance);

        return result;
    }

    /**
     * Calculate total weight from cart items
     */
    private BigDecimal calculateTotalWeight(List<CartItemDTO> items) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (CartItemDTO item : items) {
            BigDecimal itemWeight = getEffectiveProductWeight(item);
            BigDecimal itemTotalWeight = itemWeight.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalWeight = totalWeight.add(itemTotalWeight);
            
            log.debug("Item weight: {} kg × {} = {} kg", itemWeight, item.getQuantity(), itemTotalWeight);
        }
        
        return totalWeight;
    }

    /**
     * Calculate all cost components
     */
    private CostComponents calculateCostComponents(
            ShippingCost shippingCost,
            DistanceCalculationResult distanceResult,
            BigDecimal totalWeight) {

        CostComponents costs = new CostComponents();

        // Base fee
        costs.baseFee = shippingCost.getBaseFee() != null ? shippingCost.getBaseFee() : BigDecimal.ZERO;
        log.info("Base fee: {}", costs.baseFee);

        // Distance cost (using cumulative distance)
        costs.distanceCost = BigDecimal.ZERO;
        if (shippingCost.getDistanceKmCost() != null) {
            costs.distanceCost = shippingCost.getDistanceKmCost()
                .multiply(BigDecimal.valueOf(distanceResult.totalDistance));
            log.info("Distance cost: {} × {} km = {}",
                shippingCost.getDistanceKmCost(), distanceResult.totalDistance, costs.distanceCost);
        }

        // Weight cost
        costs.weightCost = BigDecimal.ZERO;
        if (shippingCost.getWeightKgCost() != null && totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            costs.weightCost = shippingCost.getWeightKgCost().multiply(totalWeight);
            log.info("Weight cost: {} × {} kg = {}",
                shippingCost.getWeightKgCost(), totalWeight, costs.weightCost);
        }

        // International fee
        costs.internationalFee = BigDecimal.ZERO;
        if (distanceResult.isInternational && shippingCost.getInternationalFee() != null) {
            costs.internationalFee = shippingCost.getInternationalFee();
            log.info("International fee: {}", costs.internationalFee);
        }

        // Total cost
        costs.totalCost = costs.baseFee
            .add(costs.distanceCost)
            .add(costs.weightCost)
            .add(costs.internationalFee);

        return costs;
    }

    /**
     * Check if order qualifies for free shipping
     */
    private boolean qualifiesForFreeShipping(ShippingCost shippingCost, BigDecimal orderValue) {
        return shippingCost.getFreeShippingThreshold() != null &&
               shippingCost.getFreeShippingThreshold().compareTo(BigDecimal.ZERO) > 0 &&
               orderValue != null &&
               orderValue.compareTo(shippingCost.getFreeShippingThreshold()) >= 0;
    }

    /**
     * Build empty shipping details response
     */
    private com.ecommerce.dto.ShippingDetailsDTO buildEmptyShippingDetails() {
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

    /**
     * Build free shipping details response
     */
    private com.ecommerce.dto.ShippingDetailsDTO buildFreeShippingDetails() {
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

    /**
     * Get effective product weight for cart item
     */
    private BigDecimal getEffectiveProductWeight(CartItemDTO item) {
        try {
            Product effectiveProduct = getEffectiveProduct(item);

            if (effectiveProduct != null && effectiveProduct.getProductDetail() != null) {
                ProductDetail productDetail = effectiveProduct.getProductDetail();
                BigDecimal productWeight = productDetail.getWeightKg();

                if (productWeight != null && productWeight.compareTo(BigDecimal.ZERO) > 0) {
                    return productWeight;
                }
            }

            // Default weight: 0.1 kg
            return new BigDecimal("0.1");

        } catch (Exception e) {
            log.warn("Error getting product weight for cart item {}: {}",
                    item.getProductId() != null ? item.getProductId() : item.getVariantId(),
                    e.getMessage());
            return new BigDecimal("0.1");
        }
    }

    /**
     * Get effective product from cart item
     */
    private Product getEffectiveProduct(CartItemDTO item) {
        if (item.isVariantBasedItem() && item.getVariantId() != null) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found with ID: " + item.getVariantId()));
            return variant.getProduct();
        } else if (item.isProductBasedItem() && item.getProductId() != null) {
            return productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + item.getProductId()));
        }

        return null;
    }

    /**
     * Map ShippingCost entity to DTO
     */
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