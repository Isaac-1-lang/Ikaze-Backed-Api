package com.ecommerce.service;

import com.ecommerce.dto.AddressDto;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.entity.Stock;
import com.ecommerce.entity.Warehouse;
import com.ecommerce.repository.StockRepository;
import com.ecommerce.repository.WarehouseRepository;
import com.ecommerce.service.GeocodingService;
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
            log.info("Starting stock allocation for {} items", items.size());
            for (CartItemDTO item : items) {
                log.info("Item details: productId={}, variantId={}, isVariantBased={}, quantity={}",
                        item.getProductId(), item.getVariantId(), item.isVariantBased(), item.getQuantity());
            }

            Map<String, Double> coordinates = geocodingService.getCoordinates(address);
            if (coordinates == null) {
                log.warn("Could not get coordinates for address, using first available warehouse");
                List<Warehouse> warehouses = warehouseRepository.findAll();
                if (warehouses.isEmpty()) {
                    log.error("No warehouses found in database");
                    return new HashMap<>();
                }
                return allocateFromSingleWarehouse(items, warehouses.get(0));
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

            // Use a consistent key generation strategy
            Long key = generateItemKey(item);
            allocationMap.put(key, allocations);
        }

        return allocationMap;
    }

    private Stock findStockForItem(CartItemDTO item, Warehouse warehouse) {
        log.info(
                "Finding stock for item: productId={}, variantId={}, isVariantBased={}, warehouseId={}, warehouseName={}",
                item.getProductId(), item.getVariantId(), item.isVariantBased(), warehouse.getId(),
                warehouse.getName());

        if (item.isVariantBasedItem()) {
            log.info("Looking for variant-based stock: variantId={}, warehouseId={}",
                    item.getVariantId(), warehouse.getId());
            Optional<Stock> stockOpt = stockRepository.findByProductVariantVariantIdAndWarehouseWarehouseId(
                    item.getVariantId(), warehouse.getId());
            if (stockOpt.isPresent()) {
                Stock stock = stockOpt.get();
                log.info("Found variant stock: stockId={}, quantity={}", stock.getId(), stock.getQuantity());
                return stock;
            } else {
                log.warn("No variant stock found for variantId={}, warehouseId={}",
                        item.getVariantId(), warehouse.getId());
                return null;
            }
        } else if (item.isProductBasedItem()) {
            log.info("Looking for product-based stock: productId={}, warehouseId={}",
                    item.getProductId(), warehouse.getId());
            Optional<Stock> stockOpt = stockRepository.findByProductProductIdAndWarehouseWarehouseId(
                    item.getProductId(), warehouse.getId());
            if (stockOpt.isPresent()) {
                Stock stock = stockOpt.get();
                log.info("Found product stock: stockId={}, quantity={}", stock.getId(), stock.getQuantity());
                return stock;
            } else {
                log.warn("No product stock found for productId={}, warehouseId={}",
                        item.getProductId(), warehouse.getId());
                return null;
            }
        }
        log.error("Cannot find stock for item: productId={}, variantId={}, isVariantBased={}",
                item.getProductId(), item.getVariantId(), item.isVariantBased());
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

            // Use a consistent key generation strategy
            Long key = generateItemKey(item);
            allocationMap.put(key, allocations);
        }

        return allocationMap;
    }

    /**
     * Generate a consistent key for cart items
     * For variant-based items: use variantId
     * For non-variant items: use productId converted to Long
     */
    private Long generateItemKey(CartItemDTO item) {
        if (item.isVariantBasedItem()) {
            return item.getVariantId();
        } else if (item.isProductBasedItem()) {
            // Convert UUID to a consistent Long value
            return Math.abs((long) item.getProductId().hashCode());
        } else {
            log.warn("Cannot generate key for item: productId={}, variantId={}, isVariantBased={}",
                    item.getProductId(), item.getVariantId(), item.isVariantBased());
            return 0L;
        }
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
