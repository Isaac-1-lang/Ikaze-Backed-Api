package com.ecommerce.service;

import com.ecommerce.dto.AddressDto;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.entity.*;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.WarehouseRepository;
import com.ecommerce.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedMultiWarehouseAllocator {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final FEFOStockAllocationService fefoService;
    private final GeocodingService geocodingService;

    public Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocateStockWithFEFO(
            List<CartItemDTO> items, AddressDto address) {
        
        Map<String, Double> coordinates = geocodingService.getCoordinates(address);
        List<Warehouse> sortedWarehouses = getSortedWarehouses(coordinates);
        
        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allAllocations = new HashMap<>();
        
        for (CartItemDTO item : items) {
            List<FEFOStockAllocationService.BatchAllocation> itemAllocations = allocateItemAcrossWarehouses(item, sortedWarehouses);
            allAllocations.put(item, itemAllocations);
        }
        
        return allAllocations;
    }

    private List<FEFOStockAllocationService.BatchAllocation> allocateItemAcrossWarehouses(
            CartItemDTO item, List<Warehouse> sortedWarehouses) {
        
        Product product = null;
        ProductVariant variant = null;
        
        if (item.getVariantId() != null) {
            variant = variantRepository.findById(item.getVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + item.getVariantId()));
            product = variant.getProduct();
        } else {
            product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));
        }
        
        List<FEFOStockAllocationService.BatchAllocation> totalAllocations = new ArrayList<>();
        int remainingQuantity = item.getQuantity();
        
        for (Warehouse warehouse : sortedWarehouses) {
            if (remainingQuantity <= 0) break;
            
            try {
                List<FEFOStockAllocationService.BatchAllocation> warehouseAllocations = 
                    fefoService.allocateStock(product, variant, remainingQuantity, warehouse.getId());
                
                int allocatedFromWarehouse = warehouseAllocations.stream()
                    .mapToInt(FEFOStockAllocationService.BatchAllocation::getQuantityAllocated)
                    .sum();
                
                totalAllocations.addAll(warehouseAllocations);
                remainingQuantity -= allocatedFromWarehouse;
                
            } catch (IllegalStateException e) {
                log.debug("Warehouse {} cannot fulfill remaining quantity {}: {}", 
                    warehouse.getName(), remainingQuantity, e.getMessage());
            }
        }
        
        if (remainingQuantity > 0) {
            String itemName = variant != null ? variant.getVariantSku() : product.getProductName();
            throw new IllegalStateException("Insufficient stock across all warehouses for " + itemName + 
                ". Missing " + remainingQuantity + " units");
        }
        
        return totalAllocations;
    }

    private List<Warehouse> getSortedWarehouses(Map<String, Double> coordinates) {
        List<Warehouse> warehouses = warehouseRepository.findByIsActiveTrue();
        
        if (coordinates == null) {
            return warehouses;
        }
        
        double userLat = coordinates.get("latitude");
        double userLon = coordinates.get("longitude");
        
        return warehouses.stream()
            .map(warehouse -> {
                double distance = DistanceCalculator.getDistanceFromLatLonInKm(
                    userLat, userLon, 
                    warehouse.getLatitude().doubleValue(),
                    warehouse.getLongitude().doubleValue()
                );
                return new WarehouseDistance(warehouse, distance);
            })
            .sorted(Comparator.comparing(wd -> wd.distance))
            .map(wd -> wd.warehouse)
            .collect(Collectors.toList());
    }

    private static class WarehouseDistance {
        final Warehouse warehouse;
        final double distance;

        WarehouseDistance(Warehouse warehouse, double distance) {
            this.warehouse = warehouse;
            this.distance = distance;
        }
    }
}
