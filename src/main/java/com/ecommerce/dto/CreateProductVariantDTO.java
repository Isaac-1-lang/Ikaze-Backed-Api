package com.ecommerce.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class CreateProductVariantDTO {
    private String variantSku;
    private String variantBarcode;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Boolean isActive;
    private Integer sortOrder;
    
    // Variant-specific attributes (color, size, material, etc.)
    private Map<String, String> attributes; // attribute_name -> attribute_value
    
    // Variant-specific media
    private List<String> variantImages;
    private String imageMetadata;
    
    // Variant-specific pricing
    private BigDecimal salePrice;
    
    // Variant-specific physical properties
    @Positive(message = "Variant height must be positive")
    private BigDecimal heightCm;
    
    @Positive(message = "Variant width must be positive")
    private BigDecimal widthCm;
    
    @Positive(message = "Variant length must be positive")
    private BigDecimal lengthCm;
    
    @Positive(message = "Variant weight must be positive")
    private BigDecimal weightKg;
    
    // Variant-specific warehouse stock
    private List<WarehouseStockDTO> warehouseStock;
    
    // Variant-specific specifications
    private String material;
    private String color;
    private String size;
    private String shape;
    private String style;
    
    // Variant-specific availability
    private Boolean isInStock;
    private Boolean isBackorderable;
    private Integer backorderQuantity;
    private String backorderMessage;
    
    // Variant-specific shipping
    private Boolean requiresSpecialShipping;
    private String shippingNotes;
    private BigDecimal additionalShippingCost;
}
