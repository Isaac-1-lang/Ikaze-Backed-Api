package com.ecommerce.controller;

import com.ecommerce.dto.WarehouseDTO;
import com.ecommerce.dto.WarehouseInventoryDTO;
import com.ecommerce.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Slf4j
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<List<WarehouseDTO>> getAllWarehouses() {
        try {
            List<WarehouseDTO> warehouses = warehouseService.getAllActiveWarehouses();
            log.info("Retrieved {} active warehouses", warehouses.size());
            return ResponseEntity.ok(warehouses);
        } catch (Exception e) {
            log.error("Error retrieving warehouses", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDTO> getWarehouseById(@PathVariable Long id) {
        try {
            WarehouseDTO warehouse = warehouseService.getWarehouseById(id);
            return ResponseEntity.ok(warehouse);
        } catch (Exception e) {
            log.error("Error retrieving warehouse with ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseDTO> createWarehouse(@RequestBody WarehouseDTO warehouseDTO) {
        try {
            WarehouseDTO savedWarehouse = warehouseService.createWarehouse(warehouseDTO);
            log.info("Created warehouse: {}", savedWarehouse.getName());
            return ResponseEntity.ok(savedWarehouse);
        } catch (Exception e) {
            log.error("Error creating warehouse", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseDTO> updateWarehouse(@PathVariable Long id, @RequestBody WarehouseDTO warehouseDTO) {
        try {
            WarehouseDTO updatedWarehouse = warehouseService.updateWarehouse(id, warehouseDTO);
            log.info("Updated warehouse: {}", updatedWarehouse.getName());
            return ResponseEntity.ok(updatedWarehouse);
        } catch (Exception e) {
            log.error("Error updating warehouse with ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable Long id) {
        try {
            warehouseService.deleteWarehouse(id);
            log.info("Deactivated warehouse with ID: {}", id);
            return ResponseEntity.ok().<Void>build();
        } catch (Exception e) {
            log.error("Error deleting warehouse with ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/inventory")
    public ResponseEntity<List<WarehouseInventoryDTO>> getWarehouseInventory(@PathVariable Long id) {
        try {
            List<WarehouseInventoryDTO> inventory = warehouseService.getWarehouseInventory(id);
            log.info("Retrieved inventory for warehouse ID: {} with {} items", id, inventory.size());
            return ResponseEntity.ok(inventory);
        } catch (Exception e) {
            log.error("Error retrieving inventory for warehouse ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/inventory/all")
    public ResponseEntity<List<WarehouseInventoryDTO>> getAllWarehouseInventory() {
        try {
            List<WarehouseInventoryDTO> inventory = warehouseService.getAllWarehouseInventory();
            log.info("Retrieved all warehouse inventory with {} items", inventory.size());
            return ResponseEntity.ok(inventory);
        } catch (Exception e) {
            log.error("Error retrieving all warehouse inventory", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
