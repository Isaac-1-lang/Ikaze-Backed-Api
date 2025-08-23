package com.ecommerce.repository;

import com.ecommerce.entity.Stock;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.Warehouse;
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
     * Find stock by warehouse and variant
     */
    Optional<Stock> findByWarehouseAndVariant(Warehouse warehouse, ProductVariant variant);
    
    /**
     * Find all stock entries for a product across all warehouses
     */
    List<Stock> findByProduct(Product product);
    
    /**
     * Find all stock entries for a variant across all warehouses
     */
    List<Stock> findByVariant(ProductVariant variant);
    
    /**
     * Find all stock entries for a warehouse
     */
    List<Stock> findByWarehouse(Warehouse warehouse);
    
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
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.product = :product AND s.variant IS NULL")
    Integer getTotalProductStock(@Param("product") Product product);
    
    /**
     * Get total stock quantity for a variant across all warehouses
     */
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s WHERE s.variant = :variant")
    Integer getTotalVariantStock(@Param("variant") ProductVariant variant);
}
