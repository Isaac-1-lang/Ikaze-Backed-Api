package com.ecommerce.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
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
    private Boolean isActive;
    private Integer sortOrder;
    private Map<String, String> attributes; // attribute_name -> attribute_value
    private List<MultipartFile> variantImages; // Changed from List<String> to List<MultipartFile>
    private String imageMetadata;
    private BigDecimal salePrice;

    // Warehouse Assignment for Variants
    private List<WarehouseStockDTO> warehouseStock;
}
