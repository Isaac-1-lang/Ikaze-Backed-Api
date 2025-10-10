package com.ecommerce.controller;

import com.ecommerce.dto.CreateWarehouseDTO;
import com.ecommerce.dto.ProductVariantWarehouseDTO;
import com.ecommerce.dto.UpdateWarehouseDTO;
import com.ecommerce.dto.WarehouseDTO;
import com.ecommerce.dto.WarehouseProductDTO;
import com.ecommerce.dto.CountryValidationRequest;
import com.ecommerce.service.WarehouseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
@Slf4j
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final ObjectMapper objectMapper;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<WarehouseDTO> createWarehouse(
            @RequestParam("warehouse") String warehouseJson,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        try {
            CreateWarehouseDTO createWarehouseDTO = objectMapper.readValue(warehouseJson, CreateWarehouseDTO.class);

            WarehouseDTO warehouse = warehouseService.createWarehouse(createWarehouseDTO, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(warehouse);
        } catch (Exception e) {
            log.error("Error creating warehouse: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{warehouseId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<WarehouseDTO> getWarehouseById(@PathVariable Long warehouseId) {
        try {
            WarehouseDTO warehouse = warehouseService.getWarehouseById(warehouseId);
            return ResponseEntity.ok(warehouse);
        } catch (Exception e) {
            log.error("Error getting warehouse by ID {}: {}", warehouseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<WarehouseDTO>> getAllWarehouses(Pageable pageable) {
        try {
            Page<WarehouseDTO> warehouses = warehouseService.getAllWarehouses(pageable);
            return ResponseEntity.ok(warehouses);
        } catch (Exception e) {
            log.error("Error getting all warehouses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<List<WarehouseDTO>> getAllWarehousesList() {
        try {
            List<WarehouseDTO> warehouses = warehouseService.getAllWarehouses();
            return ResponseEntity.ok(warehouses);
        } catch (Exception e) {
            log.error("Error getting all warehouses list: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{warehouseId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<WarehouseDTO> updateWarehouse(
            @PathVariable Long warehouseId,
            @Valid @RequestBody UpdateWarehouseDTO updateWarehouseDTO,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        try {
            log.info("Updating warehouse with ID: {}", warehouseId);
            WarehouseDTO warehouse = warehouseService.updateWarehouse(warehouseId, updateWarehouseDTO, images);
            return ResponseEntity.ok(warehouse);
        } catch (Exception e) {
            log.error("Error updating warehouse with ID {}: {}", warehouseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{warehouseId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable Long warehouseId) {
        try {
            log.info("Deleting warehouse with ID: {}", warehouseId);
            boolean deleted = warehouseService.deleteWarehouse(warehouseId);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            log.error("Error deleting warehouse with ID {}: {}", warehouseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{warehouseId}/products")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<WarehouseProductDTO>> getProductsInWarehouse(
            @PathVariable Long warehouseId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        try {
            log.info("Getting products for warehouse {}", warehouseId);
            Page<WarehouseProductDTO> products = warehouseService.getProductsInWarehouse(warehouseId, pageable);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting products in warehouse {}: {}", warehouseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{warehouseId}/products/{productId}/variants")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<List<ProductVariantWarehouseDTO>> getProductVariantsInWarehouse(
            @PathVariable Long warehouseId,
            @PathVariable UUID productId) {
        try {
            log.info("Getting variants for product {} in warehouse {}", productId, warehouseId);
            List<ProductVariantWarehouseDTO> variants = warehouseService.getProductVariantsInWarehouse(warehouseId, productId);
            return ResponseEntity.ok(variants);
        } catch (Exception e) {
            log.error("Error getting variants for product {} in warehouse {}: {}", productId, warehouseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{warehouseId}/products/{productId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Void> removeProductFromWarehouse(
            @PathVariable Long warehouseId,
            @PathVariable UUID productId) {
        try {
            log.info("Removing product {} from warehouse {}", productId, warehouseId);
            boolean removed = warehouseService.removeProductFromWarehouse(warehouseId, productId);
            if (removed) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            log.error("Error removing product {} from warehouse {}: {}", productId, warehouseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{warehouseId}/variants/{variantId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Void> removeVariantFromWarehouse(
            @PathVariable Long warehouseId,
            @PathVariable Long variantId) {
        try {
            log.info("Removing variant {} from warehouse {}", variantId, warehouseId);
            boolean removed = warehouseService.removeVariantFromWarehouse(warehouseId, variantId);
            if (removed) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            log.error("Error removing variant {} from warehouse {}: {}", variantId, warehouseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{warehouseId}/images")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<WarehouseDTO> addImagesToWarehouse(
            @PathVariable Long warehouseId,
            @RequestParam("images") List<MultipartFile> images) {
        try {
            log.info("Adding {} images to warehouse {}", images.size(), warehouseId);
            WarehouseDTO warehouse = warehouseService.addImagesToWarehouse(warehouseId, images);
            return ResponseEntity.ok(warehouse);
        } catch (Exception e) {
            log.error("Error adding images to warehouse {}: {}", warehouseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{warehouseId}/images/{imageId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Void> removeImageFromWarehouse(
            @PathVariable Long warehouseId,
            @PathVariable Long imageId) {
        try {
            log.info("Removing image {} from warehouse {}", imageId, warehouseId);
            boolean removed = warehouseService.removeImageFromWarehouse(warehouseId, imageId);
            if (removed) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            log.error("Error removing image {} from warehouse {}: {}", imageId, warehouseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/location")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<List<WarehouseDTO>> getWarehousesByLocation(
            @RequestParam String location) {
        try {
            List<WarehouseDTO> warehouses = warehouseService.getWarehousesByLocation(location);
            return ResponseEntity.ok(warehouses);
        } catch (Exception e) {
            log.error("Error getting warehouses by location: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<WarehouseDTO>> getWarehousesNearLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusKm) {
        try {
            List<WarehouseDTO> warehouses = warehouseService.getWarehousesNearLocation(latitude, longitude, radiusKm);
            return ResponseEntity.ok(warehouses);
        } catch (Exception e) {
            log.error("Error getting warehouses near location: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/countries")
    public ResponseEntity<List<String>> getWarehouseCountries() {
        log.info("Fetching all countries with warehouses");
        List<String> countries = warehouseService.getWarehouseCountries();
        return ResponseEntity.ok(countries);
    }

    @GetMapping("/countries/paginated")
    public ResponseEntity<Page<String>> getWarehouseCountriesPaginated(Pageable pageable) {
        log.info("Fetching countries with warehouses - page: {}, size: {}", pageable.getPageNumber(),
                pageable.getPageSize());
        Page<String> countries = warehouseService.getWarehouseCountriesPaginated(pageable);
        return ResponseEntity.ok(countries);
    }

    @PostMapping("/countries/validate-country")
    public ResponseEntity<Boolean> validateCountry(@RequestBody CountryValidationRequest request) {
        log.info("Validating country: {}", request.getCountry());
        boolean isValid = warehouseService.hasWarehouseInCountry(request.getCountry());
        return ResponseEntity.ok(isValid);
    }
}
