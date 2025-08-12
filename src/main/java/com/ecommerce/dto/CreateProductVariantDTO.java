package com.ecommerce.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductVariantDTO {

    private String variantSku;
    private String variantBarcode;

    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private BigDecimal salePrice;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Boolean isActive;
    private Integer sortOrder;

    // Variant attributes (e.g., color, size)
    private List<VariantAttributeDTO> attributes;

    // Variant images
    private List<MultipartFile> variantImages;
    private List<VariantImageMetadata> imageMetadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantAttributeDTO {
        private Long attributeValueId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantImageMetadata {
        private String altText;
        private Boolean isPrimary;
        private Integer sortOrder;
    }
}
