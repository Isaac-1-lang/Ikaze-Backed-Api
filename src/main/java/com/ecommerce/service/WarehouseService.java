package com.ecommerce.service;

import com.ecommerce.dto.WarehouseDTO;
import com.ecommerce.dto.WarehouseInventoryDTO;
import com.ecommerce.entity.Warehouse;
import com.ecommerce.entity.Stock;
import com.ecommerce.repository.WarehouseRepository;
import com.ecommerce.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;

    public List<WarehouseDTO> getAllActiveWarehouses() {
        List<Warehouse> warehouses = warehouseRepository.findByIsActiveTrue();
        return warehouses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public WarehouseDTO getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with ID: " + id));
        return convertToDTO(warehouse);
    }

    public WarehouseDTO createWarehouse(WarehouseDTO warehouseDTO) {
        Warehouse warehouse = convertToEntity(warehouseDTO);
        warehouse.setActive(true);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        return convertToDTO(savedWarehouse);
    }

    public WarehouseDTO updateWarehouse(Long id, WarehouseDTO warehouseDTO) {
        Warehouse existingWarehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with ID: " + id));
        
        existingWarehouse.setName(warehouseDTO.getName());
        existingWarehouse.setLocation(warehouseDTO.getLocation());
        existingWarehouse.setContactNumber(warehouseDTO.getContactNumber());
        existingWarehouse.setEmail(warehouseDTO.getEmail());
        existingWarehouse.setActive(warehouseDTO.isActive());
        
        Warehouse updatedWarehouse = warehouseRepository.save(existingWarehouse);
        return convertToDTO(updatedWarehouse);
    }

    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with ID: " + id));
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
    }

    public List<WarehouseInventoryDTO> getWarehouseInventory(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with ID: " + warehouseId));
        List<Stock> stocks = stockRepository.findByWarehouse(warehouse);
        return stocks.stream()
                .map(this::convertStockToInventoryDTO)
                .collect(Collectors.toList());
    }

    public List<WarehouseInventoryDTO> getAllWarehouseInventory() {
        List<Stock> stocks = stockRepository.findAll();
        return stocks.stream()
                .map(this::convertStockToInventoryDTO)
                .collect(Collectors.toList());
    }

    private WarehouseInventoryDTO convertStockToInventoryDTO(Stock stock) {
        WarehouseInventoryDTO dto = new WarehouseInventoryDTO();
        dto.setStockId(stock.getId());
        dto.setWarehouseId(stock.getWarehouse().getId());
        dto.setWarehouseName(stock.getWarehouse().getName());
        
        if (stock.getProduct() != null) {
            dto.setProductId(stock.getProduct().getProductId().toString());
            dto.setProductName(stock.getProduct().getProductName());
            dto.setProductSku(stock.getProduct().getSku());
            dto.setProductPrice(stock.getProduct().getPrice());
            dto.setCategory(stock.getProduct().getCategory() != null ? stock.getProduct().getCategory().getName() : null);
            dto.setBrand(stock.getProduct().getBrand() != null ? stock.getProduct().getBrand().getBrandName() : null);
            
            // Get first product image if available
            if (stock.getProduct().getImages() != null && !stock.getProduct().getImages().isEmpty()) {
                dto.setProductImage(stock.getProduct().getImages().get(0).getImageUrl());
            }
        }
        
        if (stock.getVariant() != null) {
            dto.setVariantId(stock.getVariant().getId().toString());
            dto.setVariantSku(stock.getVariant().getVariantSku());
            dto.setProductPrice(stock.getVariant().getPrice());
        }
        
        dto.setQuantity(stock.getQuantity());
        dto.setLowStockThreshold(stock.getLowStockThreshold());
        dto.setLastUpdated(stock.getUpdatedAt());
        
        // Calculate stock status
        dto.setIsInStock(stock.isInStock());
        dto.setIsLowStock(stock.isLowStock());
        dto.setIsOutOfStock(stock.isOutOfStock());
        dto.setNeedsReorder(stock.getQuantity() <= stock.getLowStockThreshold());
        
        return dto;
    }

    private WarehouseDTO convertToDTO(Warehouse warehouse) {
        return new WarehouseDTO(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getLocation(),
                warehouse.getContactNumber(),
                warehouse.getEmail(),
                warehouse.isActive(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }

    private Warehouse convertToEntity(WarehouseDTO warehouseDTO) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(warehouseDTO.getName());
        warehouse.setLocation(warehouseDTO.getLocation());
        warehouse.setContactNumber(warehouseDTO.getContactNumber());
        warehouse.setEmail(warehouseDTO.getEmail());
        warehouse.setActive(warehouseDTO.isActive());
        return warehouse;
    }
}
