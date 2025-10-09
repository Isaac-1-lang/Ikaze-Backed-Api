package com.ecommerce.service;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.Stock;
import com.ecommerce.entity.StockBatch;
import com.ecommerce.enums.BatchStatus;
import com.ecommerce.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calculating stock quantities based on batch data
 * This service replaces direct Stock.quantity usage with batch-based calculations
 */
@Service
@RequiredArgsConstructor
public class StockCalculationService {

    private final StockRepository stockRepository;

    /**
     * Calculates total available stock for a product
     * Uses batch quantities for products with variants, direct batch calculation for products without variants
     * 
     * @param product The product to calculate stock for
     * @return Total available stock quantity
     */
    public Integer calculateProductStock(Product product) {
        if (product.hasVariants()) {
            // For products with variants, sum all variant stock quantities
            return product.getVariants().stream()
                    .mapToInt(this::calculateVariantStock)
                    .sum();
        } else {
            // For products without variants, sum batch quantities directly
            return product.getStocks().stream()
                    .mapToInt(this::calculateStockFromBatches)
                    .sum();
        }
    }

    /**
     * Calculates total available stock for a product variant
     * Sums active batch quantities across all warehouses for this variant
     * 
     * @param variant The product variant to calculate stock for
     * @return Total available stock quantity
     */
    public Integer calculateVariantStock(ProductVariant variant) {
        if (variant.getStocks() == null || variant.getStocks().isEmpty()) {
            return 0;
        }
        
        return variant.getStocks().stream()
                .mapToInt(this::calculateStockFromBatches)
                .sum();
    }

    /**
     * Calculates available stock from batches for a specific stock entry
     * Only counts ACTIVE batches with quantity > 0
     * 
     * @param stock The stock entry to calculate from
     * @return Total quantity from active batches
     */
    public Integer calculateStockFromBatches(Stock stock) {
        if (stock.getStockBatches() == null || stock.getStockBatches().isEmpty()) {
            return 0;
        }
        
        return stock.getStockBatches().stream()
                .filter(batch -> batch.getStatus() == BatchStatus.ACTIVE)
                .filter(batch -> batch.getQuantity() > 0)
                .mapToInt(StockBatch::getQuantity)
                .sum();
    }

    /**
     * Checks if a product is low stock based on batch calculations
     * 
     * @param product The product to check
     * @return true if product is low stock
     */
    public boolean isProductLowStock(Product product) {
        if (product.hasVariants()) {
            // For products with variants, check if any variant is low stock
            return product.getVariants().stream()
                    .anyMatch(this::isVariantLowStock);
        } else {
            // For products without variants, check batch quantities against thresholds
            return product.getStocks().stream()
                    .anyMatch(this::isStockLowStock);
        }
    }

    /**
     * Checks if a product variant is low stock based on batch calculations
     * 
     * @param variant The variant to check
     * @return true if variant is low stock
     */
    public boolean isVariantLowStock(ProductVariant variant) {
        if (variant.getStocks() == null || variant.getStocks().isEmpty()) {
            return false;
        }
        
        int totalQuantity = calculateVariantStock(variant);
        int totalThreshold = variant.getStocks().stream()
                .mapToInt(Stock::getLowStockThreshold)
                .sum();
        
        return totalQuantity <= totalThreshold && totalQuantity > 0;
    }

    /**
     * Checks if a stock entry is low stock based on batch calculations
     * 
     * @param stock The stock entry to check
     * @return true if stock is low stock
     */
    public boolean isStockLowStock(Stock stock) {
        int batchQuantity = calculateStockFromBatches(stock);
        return batchQuantity <= stock.getLowStockThreshold() && batchQuantity > 0;
    }

    /**
     * Gets all products that are low stock based on batch calculations
     * 
     * @param products List of products to check
     * @return List of products that are low stock
     */
    public List<Product> getLowStockProducts(List<Product> products) {
        return products.stream()
                .filter(this::isProductLowStock)
                .collect(Collectors.toList());
    }

    /**
     * Gets all variants that are low stock based on batch calculations
     * 
     * @param variants List of variants to check
     * @return List of variants that are low stock
     */
    public List<ProductVariant> getLowStockVariants(List<ProductVariant> variants) {
        return variants.stream()
                .filter(this::isVariantLowStock)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a product is out of stock based on batch calculations
     * 
     * @param product The product to check
     * @return true if product is out of stock
     */
    public boolean isProductOutOfStock(Product product) {
        return calculateProductStock(product) <= 0;
    }

    /**
     * Checks if a variant is out of stock based on batch calculations
     * 
     * @param variant The variant to check
     * @return true if variant is out of stock
     */
    public boolean isVariantOutOfStock(ProductVariant variant) {
        return calculateVariantStock(variant) <= 0;
    }

    /**
     * Gets detailed stock information for a product including batch breakdown
     * 
     * @param product The product to get stock info for
     * @return Detailed stock information
     */
    public String getDetailedStockInfo(Product product) {
        StringBuilder info = new StringBuilder();
        info.append("Product: ").append(product.getProductName()).append("\n");
        info.append("Total Stock: ").append(calculateProductStock(product)).append("\n");
        
        if (product.hasVariants()) {
            info.append("Variants:\n");
            for (ProductVariant variant : product.getVariants()) {
                info.append("  - ").append(variant.getVariantName())
                    .append(": ").append(calculateVariantStock(variant)).append("\n");
            }
        } else {
            info.append("Direct Stock Entries:\n");
            // Use repository to fetch stocks with warehouses eagerly loaded
            List<Stock> stocks = stockRepository.findByProductWithWarehouse(product);
            for (Stock stock : stocks) {
                info.append("  - Warehouse ").append(stock.getWarehouse().getName())
                    .append(": ").append(calculateStockFromBatches(stock)).append("\n");
            }
        }
        
        return info.toString();
    }
}