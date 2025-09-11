package com.ecommerce.service;

import com.ecommerce.dto.AddressDto;
import com.ecommerce.entity.Warehouse;
import com.ecommerce.repository.WarehouseRepository;
import com.ecommerce.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseFinderService {

    private final WarehouseRepository warehouseRepository;
    private final GeocodingService geocodingService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public Warehouse findNearestWarehouse(AddressDto address) {
        try {
            Map<String, Double> coordinates = geocodingService.getCoordinates(address);
            if (coordinates == null) {
                log.warn("Could not get coordinates for address, using first available warehouse");
                return warehouseRepository.findAll().stream().findFirst().orElse(null);
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

            return futures.stream()
                    .map(CompletableFuture::join)
                    .min((w1, w2) -> Double.compare(w1.distance, w2.distance))
                    .map(wd -> wd.warehouse)
                    .orElse(null);

        } catch (Exception e) {
            log.error("Error finding nearest warehouse: {}", e.getMessage());
            return warehouseRepository.findAll().stream().findFirst().orElse(null);
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
