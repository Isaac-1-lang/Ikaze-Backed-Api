package com.ecommerce.service;

import com.ecommerce.dto.AddressDto;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.entity.Stock;
import com.ecommerce.entity.Warehouse;
import com.ecommerce.repository.StockRepository;
import com.ecommerce.repository.WarehouseRepository;
import com.ecommerce.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiWarehouseStockAllocator {

    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final GeocodingService geocodingService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public Map<Long, List<StockAllocation>> allocateStockAcrossWarehouses(List<CartItemDTO> items, AddressDto address) {
        try {
            Map<String, Double> coordinates = geocodingService.getCoordinates(address);
            if (coordinates == null) {
                log.warn("Could not get coordinates for address, using first available warehouse");
                return allocateFromSingleWarehouse(items, warehouseRepository.findAll().get(0));
            }

            double userLat = coordinates.get("latitude");
            double userLon = coordinates.get("longitude");

            List<Warehouse> warehouses = warehouseRepository.findAll();

            List<CompletableFuture<WarehouseDistance>> futures = warehouses.stream()
                    .map(warehouse -> CompletableFuture.supplyAsync(() -> {
                        double distance = DistanceCalculator.getDistanceFromLatLonInKm(
                                userLat, userLon, warehouse.getLatitude().doubleValue(),
                                warehouse.getLongitude().doubleValue());
                        return new WarehouseDistance(warehouse, distance);
                    }, executorService))
                    .collect(Collectors.toList());

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            allFutures.join();

            List<WarehouseDistance> sortedWarehouses = futures.stream()
                    .map(CompletableFuture::join)
                    .sorted((w1, w2) -> Double.compare(w1.distance, w2.distance))
                    .collect(Collectors.toList());

            return allocateStockFromMultipleWarehouses(items, sortedWarehouses);

        } catch (Exception e) {
            log.error("Error allocating stock across warehouses: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, List<StockAllocation>> allocateStockFromMultipleWarehouses(
            List<CartItemDTO> items, List<WarehouseDistance> sortedWarehouses) {

        Map<Long, List<StockAllocation>> allocationMap = new HashMap<>();

        for (CartItemDTO item : items) {
            List<StockAllocation> allocations = new ArrayList<>();
            int remainingQuantity = item.getQuantity();

            for (WarehouseDistance warehouseDistance : sortedWarehouses) {
                if (remainingQuantity <= 0)
                    break;

                Warehouse warehouse = warehouseDistance.warehouse;
                Stock stock = findStockForItem(item, warehouse);

                if (stock != null && stock.getQuantity() > 0) {
                    int availableQuantity = stock.getQuantity();
                    int allocatedQuantity = Math.min(remainingQuantity, availableQuantity);

                    allocations.add(new StockAllocation(
                            warehouse.getId(),
                            warehouse.getName(),
                            stock.getId(),
                            allocatedQuantity,
                            warehouseDistance.distance));

                    remainingQuantity -= allocatedQuantity;
                    log.info("Allocated {} units of item {} from warehouse {} (distance: {} km)",
                            allocatedQuantity, item.getProductId(), warehouse.getName(), warehouseDistance.distance);
                }
            }

            if (remainingQuantity > 0) {
                log.error("Insufficient stock for item {}: {} units still needed", item.getProductId(),
                        remainingQuantity);
                return new HashMap<>();
            }

            Long key = item.getVariantId() != null ? item.getVariantId()
                    : (item.getProductId() != null ? item.getProductId().hashCode() : 0L);
            allocationMap.put(key, allocations);
        }

        return allocationMap;
    }

    private Stock findStockForItem(CartItemDTO item, Warehouse warehouse) {
        if (item.getVariantId() != null) {
            return stockRepository.findByProductVariantVariantIdAndWarehouseWarehouseId(
                    item.getVariantId(), warehouse.getId()).orElse(null);
        } else if (item.getProductId() != null) {
            return stockRepository.findByProductProductIdAndWarehouseWarehouseId(
                    item.getProductId(), warehouse.getId()).orElse(null);
        }
        return null;
    }

    private Map<Long, List<StockAllocation>> allocateFromSingleWarehouse(List<CartItemDTO> items, Warehouse warehouse) {
        Map<Long, List<StockAllocation>> allocationMap = new HashMap<>();

        for (CartItemDTO item : items) {
            Stock stock = findStockForItem(item, warehouse);
            if (stock == null || stock.getQuantity() < item.getQuantity()) {
                log.error("Insufficient stock in single warehouse for item {}", item.getProductId());
                return new HashMap<>();
            }

            List<StockAllocation> allocations = Arrays.asList(
                    new StockAllocation(warehouse.getId(), warehouse.getName(),
                            stock.getId(), item.getQuantity(), 0.0));

            Long key = item.getVariantId() != null ? item.getVariantId()
                    : (item.getProductId() != null ? item.getProductId().hashCode() : 0L);
            allocationMap.put(key, allocations);
        }

        return allocationMap;
    }

    public static class StockAllocation {
        private final Long warehouseId;
        private final String warehouseName;
        private final Long stockId;
        private final Integer quantity;
        private final Double distance;

        public StockAllocation(Long warehouseId, String warehouseName, Long stockId, Integer quantity,
                Double distance) {
            this.warehouseId = warehouseId;
            this.warehouseName = warehouseName;
            this.stockId = stockId;
            this.quantity = quantity;
            this.distance = distance;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public String getWarehouseName() {
            return warehouseName;
        }

        public Long getStockId() {
            return stockId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public Double getDistance() {
            return distance;
        }
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
