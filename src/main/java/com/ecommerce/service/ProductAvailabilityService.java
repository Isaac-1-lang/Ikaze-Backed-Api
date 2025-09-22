package com.ecommerce.service;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.Stock;
import com.ecommerce.repository.StockBatchRepository;
import com.ecommerce.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductAvailabilityService {

    private final StockRepository stockRepository;
    private final StockBatchRepository stockBatchRepository;

    public boolean isProductAvailableForCustomers(Product product) {
        if (!isProductValidForDisplay(product)) {
            return false;
        }
        
        return hasAvailableStock(product);
    }

    public boolean isProductValidForDisplay(Product product) {
        return product.isActive() && 
               Boolean.TRUE.equals(product.getDisplayToCustomers());
    }

    public boolean hasAvailableStock(Product product) {
        List<ProductVariant> variants = product.getVariants();
        
        if (variants == null || variants.isEmpty()) {
            return hasProductStock(product);
        } else {
            return hasAnyVariantStock(variants);
        }
    }

    private boolean hasProductStock(Product product) {
        List<Stock> productStocks = stockRepository.findByProduct(product);
        
        for (Stock stock : productStocks) {
            if (hasStockAvailable(stock)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyVariantStock(List<ProductVariant> variants) {
        for (ProductVariant variant : variants) {
            if (variant.isActive() && hasVariantStock(variant)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasVariantStock(ProductVariant variant) {
        List<Stock> variantStocks = stockRepository.findByProductVariant(variant);
        
        for (Stock stock : variantStocks) {
            if (hasStockAvailable(stock)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStockAvailable(Stock stock) {
        Integer totalActiveQuantity = stockBatchRepository.getTotalActiveQuantityByStock(stock);
        return totalActiveQuantity != null && totalActiveQuantity > 0;
    }

    public boolean isVariantAvailableForCustomers(ProductVariant variant) {
        return variant.isActive() && hasVariantStock(variant);
    }

    public List<ProductVariant> getAvailableVariants(Product product) {
        if (product.getVariants() == null) {
            return List.of();
        }
        
        return product.getVariants().stream()
                .filter(this::isVariantAvailableForCustomers)
                .toList();
    }

    public int getTotalAvailableStock(Product product) {
        List<ProductVariant> variants = product.getVariants();
        
        if (variants == null || variants.isEmpty()) {
            return getProductTotalStock(product);
        } else {
            return getVariantsTotalStock(variants);
        }
    }

    private int getProductTotalStock(Product product) {
        List<Stock> productStocks = stockRepository.findByProduct(product);
        
        return productStocks.stream()
                .mapToInt(stock -> {
                    Integer quantity = stockBatchRepository.getTotalActiveQuantityByStock(stock);
                    return quantity != null ? quantity : 0;
                })
                .sum();
    }

    private int getVariantsTotalStock(List<ProductVariant> variants) {
        return variants.stream()
                .filter(ProductVariant::isActive)
                .mapToInt(this::getVariantTotalStock)
                .sum();
    }

    public int getVariantTotalStock(ProductVariant variant) {
        List<Stock> variantStocks = stockRepository.findByProductVariant(variant);
        
        return variantStocks.stream()
                .mapToInt(stock -> {
                    Integer quantity = stockBatchRepository.getTotalActiveQuantityByStock(stock);
                    return quantity != null ? quantity : 0;
                })
                .sum();
    }

    public boolean isProductLowStock(Product product) {
        List<ProductVariant> variants = product.getVariants();
        
        if (variants == null || variants.isEmpty()) {
            return isProductDirectlyLowStock(product);
        } else {
            return areVariantsLowStock(variants);
        }
    }

    private boolean isProductDirectlyLowStock(Product product) {
        List<Stock> productStocks = stockRepository.findByProduct(product);
        
        for (Stock stock : productStocks) {
            Integer totalQuantity = stockBatchRepository.getTotalActiveQuantityByStock(stock);
            if (totalQuantity != null && totalQuantity > 0 && totalQuantity <= stock.getLowStockThreshold()) {
                return true;
            }
        }
        return false;
    }

    private boolean areVariantsLowStock(List<ProductVariant> variants) {
        for (ProductVariant variant : variants) {
            if (variant.isActive() && isVariantLowStock(variant)) {
                return true;
            }
        }
        return false;
    }

    public boolean isVariantLowStock(ProductVariant variant) {
        List<Stock> variantStocks = stockRepository.findByProductVariant(variant);
        
        for (Stock stock : variantStocks) {
            Integer totalQuantity = stockBatchRepository.getTotalActiveQuantityByStock(stock);
            if (totalQuantity != null && totalQuantity > 0 && totalQuantity <= stock.getLowStockThreshold()) {
                return true;
            }
        }
        return false;
    }
}
