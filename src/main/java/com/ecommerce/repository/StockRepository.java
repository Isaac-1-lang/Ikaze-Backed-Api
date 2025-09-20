package com.ecommerce.repository;

import com.ecommerce.entity.Stock;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

        /**
         * Find stock by warehouse and product
         */
        Optional<Stock> findByWarehouseAndProduct(Warehouse warehouse, Product product);

        /**
         * Find stock by warehouse
         */
        List<Stock> findByWarehouse(Warehouse warehouse);

        /**
         * Find stock by warehouse with pagination
         */
        Page<Stock> findByWarehouse(Warehouse warehouse, Pageable pageable);

        /**
         * Find stock by warehouse and variant
         */
        List<Stock> findByWarehouseAndProductVariant(Warehouse warehouse, ProductVariant variant);

        /**
         * Find all stock entries for a product across all warehouses
         */
        List<Stock> findByProduct(Product product);

        /**
         * Find all stock entries for a variant across all warehouses
         */
        List<Stock> findByProductVariant(ProductVariant variant);

        /**
         * Find all stock entries for a variant across all warehouses with pagination
         */
        Page<Stock> findByProductVariant(ProductVariant variant, Pageable pageable);

        /**
         * Find stock entries with low stock (quantity <= lowStockThreshold)
         */
        @Query("SELECT s FROM Stock s WHERE s.quantity <= s.lowStockThreshold AND s.quantity > 0")
        List<Stock> findLowStockItems();

        /**
         * Find stock entries that are out of stock
         */
        @Query("SELECT s FROM Stock s WHERE s.quantity <= 0")
        List<Stock> findOutOfStockItems();

        /**
         * Get total stock quantity for a product across all warehouses
         */
        @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.product = :product AND s.productVariant IS NULL")
        Integer getTotalProductStock(@Param("product") Product product);

        /**
         * Get total stock quantity for a variant across all warehouses
         */
        @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.productVariant = :variant")
        Integer getTotalVariantStock(@Param("variant") ProductVariant variant);

        /**
         * Find stock by product ID and warehouse ID
         */
        @Query("SELECT s FROM Stock s WHERE s.product.productId = :productId AND s.warehouse.id = :warehouseId AND s.productVariant IS NULL")
        Optional<Stock> findByProductProductIdAndWarehouseWarehouseId(@Param("productId") java.util.UUID productId,
                        @Param("warehouseId") Long warehouseId);

        /**
         * Find stock by variant ID and warehouse ID
         */
        @Query("SELECT s FROM Stock s WHERE s.productVariant.id = :variantId AND s.warehouse.id = :warehouseId")
        Optional<Stock> findByProductVariantVariantIdAndWarehouseWarehouseId(@Param("variantId") Long variantId,
                        @Param("warehouseId") Long warehouseId);

        /**
         * Find all stock entries for a warehouse by warehouse ID
         */
        @Query("SELECT s FROM Stock s WHERE s.warehouse.id = :warehouseId")
        List<Stock> findByWarehouseWarehouseId(@Param("warehouseId") Long warehouseId);

        /**
         * Find all stock entries for a product or its variants with pagination
         */
        @Query("SELECT s FROM Stock s WHERE s.product = :product OR s.productVariant.product = :product")
        Page<Stock> findByProductOrProductVariantProduct(@Param("product") Product product, Pageable pageable);

        /**
         * Find all stock entries for a variant by variant ID
         */
        @Query("SELECT s FROM Stock s WHERE s.productVariant.id = :variantId")
        List<Stock> findByProductVariantId(@Param("variantId") Long variantId);
}
