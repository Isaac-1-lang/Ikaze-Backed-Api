package com.ecommerce.service.impl;

import com.ecommerce.dto.CreateWarehouseDTO;
import com.ecommerce.dto.UpdateWarehouseDTO;
import com.ecommerce.dto.WarehouseDTO;
import com.ecommerce.dto.WarehouseProductDTO;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.CloudinaryService;
import com.ecommerce.service.WarehouseService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseImageRepository warehouseImageRepository;
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public WarehouseDTO createWarehouse(CreateWarehouseDTO createWarehouseDTO, List<MultipartFile> warehouseImages) {
        try {
            log.info("Creating warehouse: {}", createWarehouseDTO.getName());

            Warehouse warehouse = new Warehouse();
            warehouse.setName(createWarehouseDTO.getName());
            warehouse.setDescription(createWarehouseDTO.getDescription());
            warehouse.setAddress(createWarehouseDTO.getAddress());
            warehouse.setCity(createWarehouseDTO.getCity());
            warehouse.setState(createWarehouseDTO.getState());
            warehouse.setZipCode(createWarehouseDTO.getZipCode());
            warehouse.setCountry(createWarehouseDTO.getCountry());
            warehouse.setContactNumber(createWarehouseDTO.getPhone());
            warehouse.setEmail(createWarehouseDTO.getEmail());
            warehouse.setCapacity(createWarehouseDTO.getCapacity());
            if (createWarehouseDTO.getLatitude() != null) {
                warehouse.setLatitude(BigDecimal.valueOf(createWarehouseDTO.getLatitude()));
            }
            if (createWarehouseDTO.getLongitude() != null) {
                warehouse.setLongitude(BigDecimal.valueOf(createWarehouseDTO.getLongitude()));
            }
            warehouse.setActive(createWarehouseDTO.getIsActive() != null ? createWarehouseDTO.getIsActive() : true);

            Warehouse savedWarehouse = warehouseRepository.save(warehouse);
            log.info("Warehouse created with ID: {}", savedWarehouse.getId());

            if (warehouseImages != null && !warehouseImages.isEmpty()) {
                processWarehouseImages(savedWarehouse, warehouseImages);
            }

            return mapWarehouseToDTO(savedWarehouse);
        } catch (Exception e) {
            log.error("Error creating warehouse: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create warehouse: " + e.getMessage(), e);
        }
    }

    @Override
    public WarehouseDTO getWarehouseById(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with ID: " + warehouseId));
        return mapWarehouseToDTO(warehouse);
    }

    @Override
    public Page<WarehouseDTO> getAllWarehouses(Pageable pageable) {
        return warehouseRepository.findAll(pageable)
                .map(this::mapWarehouseToDTO);
    }

    @Override
    public List<WarehouseDTO> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::mapWarehouseToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WarehouseDTO updateWarehouse(Long warehouseId, UpdateWarehouseDTO updateWarehouseDTO,
            List<MultipartFile> newImages) {
        try {
            log.info("Updating warehouse with ID: {}", warehouseId);

            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with ID: " + warehouseId));

            if (updateWarehouseDTO.getName() != null) {
                warehouse.setName(updateWarehouseDTO.getName());
            }
            if (updateWarehouseDTO.getDescription() != null) {
                warehouse.setDescription(updateWarehouseDTO.getDescription());
            }
            if (updateWarehouseDTO.getAddress() != null) {
                warehouse.setAddress(updateWarehouseDTO.getAddress());
            }
            if (updateWarehouseDTO.getCity() != null) {
                warehouse.setCity(updateWarehouseDTO.getCity());
            }
            if (updateWarehouseDTO.getState() != null) {
                warehouse.setState(updateWarehouseDTO.getState());
            }
            if (updateWarehouseDTO.getZipCode() != null) {
                warehouse.setZipCode(updateWarehouseDTO.getZipCode());
            }
            if (updateWarehouseDTO.getCountry() != null) {
                warehouse.setCountry(updateWarehouseDTO.getCountry());
            }
            if (updateWarehouseDTO.getPhone() != null) {
                warehouse.setContactNumber(updateWarehouseDTO.getPhone());
            }
            if (updateWarehouseDTO.getEmail() != null) {
                warehouse.setEmail(updateWarehouseDTO.getEmail());
            }
            if (updateWarehouseDTO.getCapacity() != null) {
                warehouse.setCapacity(updateWarehouseDTO.getCapacity());
            }
            if (updateWarehouseDTO.getLatitude() != null) {
                warehouse.setLatitude(BigDecimal.valueOf(updateWarehouseDTO.getLatitude()));
            }
            if (updateWarehouseDTO.getLongitude() != null) {
                warehouse.setLongitude(BigDecimal.valueOf(updateWarehouseDTO.getLongitude()));
            }
            if (updateWarehouseDTO.getIsActive() != null) {
                warehouse.setActive(updateWarehouseDTO.getIsActive());
            }

            Warehouse savedWarehouse = warehouseRepository.save(warehouse);

            if (newImages != null && !newImages.isEmpty()) {
                processWarehouseImages(savedWarehouse, newImages);
            }

            log.info("Warehouse updated successfully with ID: {}", warehouseId);
            return mapWarehouseToDTO(savedWarehouse);
        } catch (Exception e) {
            log.error("Error updating warehouse with ID {}: {}", warehouseId, e.getMessage(), e);
            throw new RuntimeException("Failed to update warehouse: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteWarehouse(Long warehouseId) {
        try {
            log.info("Deleting warehouse with ID: {}", warehouseId);

            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with ID: " + warehouseId));

            List<Stock> stocks = stockRepository.findByWarehouse(warehouse);
            if (!stocks.isEmpty()) {
                log.warn("Cannot delete warehouse {} as it contains {} stock entries", warehouseId, stocks.size());
                throw new IllegalStateException(
                        "Cannot delete warehouse that contains stock entries. Please remove all stock first.");
            }

            deleteWarehouseImages(warehouse);
            warehouseRepository.delete(warehouse);

            log.info("Warehouse deleted successfully with ID: {}", warehouseId);
            return true;
        } catch (Exception e) {
            log.error("Error deleting warehouse with ID {}: {}", warehouseId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete warehouse: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<WarehouseProductDTO> getProductsInWarehouse(Long warehouseId, Pageable pageable) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with ID: " + warehouseId));

        Page<Stock> stockPage = stockRepository.findByWarehouse(warehouse, pageable);

        return stockPage.map(stock -> {
            WarehouseProductDTO dto = new WarehouseProductDTO();
            dto.setStockId(stock.getId());
            dto.setQuantity(stock.getQuantity());
            dto.setLowStockThreshold(stock.getLowStockThreshold());

            if (stock.getProduct() != null) {
                dto.setProductId(stock.getProduct().getProductId());
                dto.setProductName(stock.getProduct().getProductName());
                dto.setProductSku(stock.getProduct().getSku());
                dto.setIsVariant(false);
                // Set product images
                dto.setProductImages(stock.getProduct().getImages() != null
                        ? stock.getProduct().getImages().stream()
                                .map(image -> image.getImageUrl())
                                .collect(Collectors.toList())
                        : List.of());
            } else if (stock.getProductVariant() != null) {
                dto.setProductId(stock.getProductVariant().getProduct().getProductId());
                dto.setProductName(stock.getProductVariant().getProduct().getProductName());
                dto.setVariantId(stock.getProductVariant().getId());
                dto.setVariantSku(stock.getProductVariant().getVariantSku());
                dto.setIsVariant(true);
                // Set product images from the parent product
                dto.setProductImages(stock.getProductVariant().getProduct().getImages() != null
                        ? stock.getProductVariant().getProduct().getImages().stream()
                                .map(image -> image.getImageUrl())
                                .collect(Collectors.toList())
                        : List.of());
            }

            return dto;
        });
    }

    @Override
    @Transactional
    public boolean removeProductFromWarehouse(Long warehouseId, UUID productId) {
        try {
            log.info("Removing product {} from warehouse {}", productId, warehouseId);

            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with ID: " + warehouseId));

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            boolean removed = false;

            // First, try to remove stock directly linked to the product
            Optional<Stock> productStockOpt = stockRepository.findByWarehouseAndProduct(warehouse, product);
            if (productStockOpt.isPresent()) {
                stockRepository.delete(productStockOpt.get());
                removed = true;
                log.info("Removed product stock for product {} from warehouse {}", productId, warehouseId);
            }

            // Then, remove stock for all variants of this product
            List<ProductVariant> variants = productVariantRepository.findByProductProductId(productId);
            for (ProductVariant variant : variants) {
                List<Stock> variantStocks = stockRepository.findByWarehouseAndProductVariant(warehouse, variant);
                if (!variantStocks.isEmpty()) {
                    stockRepository.deleteAll(variantStocks);
                    removed = true;
                    log.info("Removed {} variant stock(s) for product {} from warehouse {}",
                            variantStocks.size(), productId, warehouseId);
                }
            }

            if (!removed) {
                log.warn("No stock found for product {} in warehouse {}", productId, warehouseId);
                return false;
            }

            log.info("Successfully removed product {} from warehouse {}", productId, warehouseId);
            return true;
        } catch (Exception e) {
            log.error("Error removing product {} from warehouse {}: {}", productId, warehouseId, e.getMessage(), e);
            throw new RuntimeException("Failed to remove product from warehouse: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean removeVariantFromWarehouse(Long warehouseId, Long variantId) {
        try {
            log.info("Removing variant {} from warehouse {}", variantId, warehouseId);

            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with ID: " + warehouseId));

            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));

            List<Stock> stocks = stockRepository.findByWarehouseAndProductVariant(warehouse, variant);
            if (stocks.isEmpty()) {
                log.warn("No stock found for variant {} in warehouse {}", variantId, warehouseId);
                return false;
            }

            stockRepository.deleteAll(stocks);
            log.info("Successfully removed variant {} from warehouse {}", variantId, warehouseId);
            return true;
        } catch (Exception e) {
            log.error("Error removing variant {} from warehouse {}: {}", variantId, warehouseId, e.getMessage(), e);
            throw new RuntimeException("Failed to remove variant from warehouse: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public WarehouseDTO addImagesToWarehouse(Long warehouseId, List<MultipartFile> images) {
        try {
            log.info("Adding {} images to warehouse {}", images.size(), warehouseId);

            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with ID: " + warehouseId));

            processWarehouseImages(warehouse, images);

            return mapWarehouseToDTO(warehouse);
        } catch (Exception e) {
            log.error("Error adding images to warehouse {}: {}", warehouseId, e.getMessage(), e);
            throw new RuntimeException("Failed to add images to warehouse: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean removeImageFromWarehouse(Long warehouseId, Long imageId) {
        try {
            log.info("Removing image {} from warehouse {}", imageId, warehouseId);

            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with ID: " + warehouseId));

            WarehouseImage image = warehouseImageRepository.findById(imageId)
                    .orElseThrow(() -> new EntityNotFoundException("Warehouse image not found with ID: " + imageId));

            if (!image.getWarehouse().getId().equals(warehouseId)) {
                throw new IllegalArgumentException("Image does not belong to the specified warehouse");
            }

            if (image.getImageUrl() != null && !image.getImageUrl().isEmpty()) {
                cloudinaryService.deleteImage(image.getImageUrl());
            }

            warehouseImageRepository.delete(image);
            log.info("Successfully removed image {} from warehouse {}", imageId, warehouseId);
            return true;
        } catch (Exception e) {
            log.error("Error removing image {} from warehouse {}: {}", imageId, warehouseId, e.getMessage(), e);
            throw new RuntimeException("Failed to remove image from warehouse: " + e.getMessage(), e);
        }
    }

    @Override
    public List<WarehouseDTO> getWarehousesByLocation(String location) {
        return warehouseRepository.findByAddressContaining(location).stream()
                .map(this::mapWarehouseToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WarehouseDTO> getWarehousesNearLocation(Double latitude, Double longitude, Double radiusKm) {
        return warehouseRepository.findWarehousesNearLocation(latitude, longitude, radiusKm).stream()
                .map(this::mapWarehouseToDTO)
                .collect(Collectors.toList());
    }

    private void processWarehouseImages(Warehouse warehouse, List<MultipartFile> images) {
        try {
            log.info("Processing {} warehouse images for warehouse {}", images.size(), warehouse.getId());

            for (MultipartFile image : images) {
                if (image.getSize() > 50 * 1024 * 1024) {
                    throw new IllegalArgumentException(
                            String.format("Warehouse image '%s' file size (%.2f MB) exceeds maximum allowed (50 MB)",
                                    image.getOriginalFilename(), image.getSize() / (1024.0 * 1024.0)));
                }
            }

            List<Map<String, String>> uploadResults = cloudinaryService.uploadMultipleImages(images);

            for (int i = 0; i < uploadResults.size(); i++) {
                Map<String, String> uploadResult = uploadResults.get(i);

                if (uploadResult.containsKey("error")) {
                    log.error("Failed to upload warehouse image {}: {}", i, uploadResult.get("error"));
                    throw new RuntimeException("Failed to upload warehouse image: " + uploadResult.get("error"));
                }

                WarehouseImage warehouseImage = new WarehouseImage();
                warehouseImage.setWarehouse(warehouse);
                warehouseImage.setImageUrl(uploadResult.get("url"));
                warehouseImage.setSortOrder(i);
                warehouseImage.setPrimary(i == 0);

                warehouseImageRepository.save(warehouseImage);
            }

            log.info("Successfully processed {} warehouse images for warehouse {}", uploadResults.size(),
                    warehouse.getId());
        } catch (Exception e) {
            log.error("Error processing warehouse images for warehouse {}: {}", warehouse.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process warehouse images: " + e.getMessage(), e);
        }
    }

    private void deleteWarehouseImages(Warehouse warehouse) {
        try {
            List<WarehouseImage> images = warehouseImageRepository.findByWarehouseId(warehouse.getId());
            if (images.isEmpty()) {
                log.debug("No images found for warehouse ID: {}", warehouse.getId());
                return;
            }

            log.info("Deleting {} images for warehouse ID: {}", images.size(), warehouse.getId());

            for (WarehouseImage image : images) {
                try {
                    if (image.getImageUrl() != null && !image.getImageUrl().isEmpty()) {
                        cloudinaryService.deleteImage(image.getImageUrl());
                        log.debug("Deleted image from Cloudinary: {}", image.getImageUrl());
                    }
                } catch (Exception e) {
                    log.warn("Failed to delete image from Cloudinary: {}. Error: {}",
                            image.getImageUrl(), e.getMessage());
                }
            }

            warehouseImageRepository.deleteAll(images);
            log.info("Successfully deleted {} warehouse images from database for warehouse ID: {}",
                    images.size(), warehouse.getId());
        } catch (Exception e) {
            log.error("Error deleting warehouse images for warehouse ID {}: {}", warehouse.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to delete warehouse images: " + e.getMessage(), e);
        }
    }

    private WarehouseDTO mapWarehouseToDTO(Warehouse warehouse) {
        WarehouseDTO dto = new WarehouseDTO();
        dto.setId(warehouse.getId());
        dto.setName(warehouse.getName());
        dto.setDescription(warehouse.getDescription());
        dto.setAddress(warehouse.getAddress());
        dto.setCity(warehouse.getCity());
        dto.setState(warehouse.getState());
        dto.setZipCode(warehouse.getZipCode());
        dto.setCountry(warehouse.getCountry());
        dto.setPhone(warehouse.getContactNumber());
        dto.setEmail(warehouse.getEmail());
        dto.setCapacity(warehouse.getCapacity());
        dto.setLatitude(warehouse.getLatitude() != null ? warehouse.getLatitude().doubleValue() : null);
        dto.setLongitude(warehouse.getLongitude() != null ? warehouse.getLongitude().doubleValue() : null);
        dto.setIsActive(warehouse.isActive());

        // Calculate product count for this warehouse
        List<Stock> stocks = stockRepository.findByWarehouse(warehouse);
        Set<String> uniqueProducts = stocks.stream()
                .map(stock -> stock.getProduct() != null ? stock.getProduct().getProductId().toString()
                        : stock.getProductVariant().getId().toString())
                .collect(Collectors.toSet());
        dto.setProductCount(uniqueProducts.size());

        dto.setCreatedAt(warehouse.getCreatedAt());
        dto.setUpdatedAt(warehouse.getUpdatedAt());

        List<WarehouseImage> images = warehouseImageRepository.findByWarehouseId(warehouse.getId());
        dto.setImages(images.stream()
                .map(this::mapWarehouseImageToDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    private WarehouseDTO.WarehouseImageDTO mapWarehouseImageToDTO(WarehouseImage image) {
        return WarehouseDTO.WarehouseImageDTO.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.isPrimary())
                .sortOrder(image.getSortOrder())
                .width(null)
                .height(null)
                .fileSize(null)
                .mimeType(null)
                .build();
    }

    @Override
    public List<String> getWarehouseCountries() {
        log.info("Fetching all countries with warehouses");
        List<String> countries = warehouseRepository.findDistinctCountries();
        log.info("Found {} unique countries: {}", countries.size(), countries);
        return countries;
    }

    @Override
    public Page<String> getWarehouseCountriesPaginated(Pageable pageable) {
        log.info("Fetching countries with warehouses - page: {}, size: {}", pageable.getPageNumber(),
                pageable.getPageSize());

        // Get all unique countries using repository method
        List<String> allCountries = warehouseRepository.findDistinctCountries();

        log.info("Found {} unique countries: {}", allCountries.size(), allCountries);

        // Calculate pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allCountries.size());

        List<String> pageContent = allCountries.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allCountries.size());
    }

    @Override
    public boolean hasWarehouseInCountry(String country) {
        log.info("Checking if warehouse exists in country: {}", country);

        if (country == null || country.trim().isEmpty()) {
            log.warn("Country is null or empty, returning false");
            return false;
        }

        List<Warehouse> allWarehouses = warehouseRepository.findAll();
        log.info("Total warehouses in database: {}", allWarehouses.size());

        for (Warehouse warehouse : allWarehouses) {
            log.info("Warehouse: {} - Country: '{}'", warehouse.getName(), warehouse.getCountry());
        }

        boolean exists = warehouseRepository.existsByCountryIgnoreCase(country.trim());
        log.info("Country '{}' exists: {}", country, exists);

        return exists;
    }
}
