package com.ecommerce.repository;

import com.ecommerce.entity.WarehouseImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseImageRepository extends JpaRepository<WarehouseImage, Long> {

    /**
     * Find all images for a warehouse
     */
    List<WarehouseImage> findByWarehouseId(Long warehouseId);

    /**
     * Find primary image for a warehouse
     */
    WarehouseImage findByWarehouseIdAndIsPrimaryTrue(Long warehouseId);

    /**
     * Find images by warehouse ID ordered by sort order
     */
    List<WarehouseImage> findByWarehouseIdOrderBySortOrderAsc(Long warehouseId);
}
