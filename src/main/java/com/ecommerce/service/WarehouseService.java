package com.ecommerce.service;

import com.ecommerce.dto.CreateWarehouseDTO;
import com.ecommerce.dto.ProductVariantWarehouseDTO;
import com.ecommerce.dto.UpdateWarehouseDTO;
import com.ecommerce.dto.WarehouseDTO;
import com.ecommerce.dto.WarehouseProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface WarehouseService {

    WarehouseDTO createWarehouse(CreateWarehouseDTO createWarehouseDTO, List<MultipartFile> warehouseImages);

    WarehouseDTO getWarehouseById(Long warehouseId);

    Page<WarehouseDTO> getAllWarehouses(Pageable pageable);

    List<WarehouseDTO> getAllWarehouses();
    
    // Overloaded methods with shopId parameter
    default Page<WarehouseDTO> getAllWarehouses(UUID shopId, Pageable pageable) {
        return getAllWarehouses(pageable);
    }
    
    default List<WarehouseDTO> getAllWarehouses(UUID shopId) {
        return getAllWarehouses();
    }
    WarehouseDTO updateWarehouse(Long warehouseId, UpdateWarehouseDTO updateWarehouseDTO,
            List<MultipartFile> newImages);

    boolean deleteWarehouse(Long warehouseId);

    Page<WarehouseProductDTO> getProductsInWarehouse(Long warehouseId, Pageable pageable);

    List<ProductVariantWarehouseDTO> getProductVariantsInWarehouse(Long warehouseId, UUID productId);

    boolean removeProductFromWarehouse(Long warehouseId, UUID productId);

    boolean removeVariantFromWarehouse(Long warehouseId, Long variantId);

    WarehouseDTO addImagesToWarehouse(Long warehouseId, List<MultipartFile> images);
    boolean removeImageFromWarehouse(Long warehouseId, Long imageId);

    List<WarehouseDTO> getWarehousesByLocation(String location);
    
    // Overloaded method with shopId parameter
    default List<WarehouseDTO> getWarehousesByLocation(String location, UUID shopId) {
        return getWarehousesByLocation(location);
    }

    List<WarehouseDTO> getWarehousesNearLocation(Double latitude, Double longitude, Double radiusKm);
    
    // Overloaded method with shopId parameter
    default List<WarehouseDTO> getWarehousesNearLocation(Double latitude, Double longitude, Double radiusKm, UUID shopId) {
        return getWarehousesNearLocation(latitude, longitude, radiusKm);
    }

    List<String> getWarehouseCountries();

    Page<String> getWarehouseCountriesPaginated(Pageable pageable);

    boolean hasWarehouseInCountry(String country);
}