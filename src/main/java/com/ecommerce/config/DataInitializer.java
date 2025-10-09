package com.ecommerce.config;

import com.ecommerce.entity.Warehouse;
import com.ecommerce.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Initialize sample data for testing delivery availability
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final WarehouseRepository warehouseRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeWarehouses();
    }

    private void initializeWarehouses() {
        try {
            // Check if warehouses already exist
            if (warehouseRepository.count() > 0) {
                log.info("Warehouses already exist, skipping initialization");
                return;
            }

            log.info("Initializing sample warehouses for delivery testing...");

            // Create sample warehouses in different countries
            createWarehouse("US Central Warehouse", "123 Main St", "New York", "NY", "10001", 
                    "United States", "40.7128", "-74.0060");
            
            createWarehouse("UK London Warehouse", "456 Oxford St", "London", "England", "SW1A 1AA", 
                    "United Kingdom", "51.5074", "-0.1278");
            
            createWarehouse("Canada Toronto Warehouse", "789 Queen St", "Toronto", "ON", "M5H 2N2", 
                    "Canada", "43.6532", "-79.3832");
            
            createWarehouse("Germany Berlin Warehouse", "101 Unter den Linden", "Berlin", "Berlin", "10117", 
                    "Germany", "52.5200", "13.4050");
            
            createWarehouse("Australia Sydney Warehouse", "202 George St", "Sydney", "NSW", "2000", 
                    "Australia", "-33.8688", "151.2093");

            log.info("Sample warehouses initialized successfully");

        } catch (Exception e) {
            log.error("Error initializing sample warehouses: {}", e.getMessage(), e);
        }
    }

    private void createWarehouse(String name, String address, String city, String state, 
                               String zipCode, String country, String lat, String lng) {
        try {
            Warehouse warehouse = new Warehouse();
            warehouse.setName(name);
            warehouse.setAddress(address);
            warehouse.setCity(city);
            warehouse.setState(state);
            warehouse.setZipCode(zipCode);
            warehouse.setCountry(country);
            warehouse.setLatitude(new BigDecimal(lat));
            warehouse.setLongitude(new BigDecimal(lng));
            warehouse.setActive(true);
            warehouse.setCapacity(1000);
            warehouse.setContactNumber("+1-555-0123");
            warehouse.setEmail("warehouse@shopsphere.com");

            warehouseRepository.save(warehouse);
            log.info("Created warehouse: {} in {}", name, country);

        } catch (Exception e) {
            log.error("Error creating warehouse {}: {}", name, e.getMessage());
        }
    }
}
