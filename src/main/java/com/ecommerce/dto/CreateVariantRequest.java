package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVariantRequest {

    @NotBlank(message = "Variant name is required")
    @Size(max = 255, message = "Variant name must not exceed 255 characters")
    private String variantName;

    @NotBlank(message = "Variant SKU is required")
    @Size(max = 100, message = "Variant SKU must not exceed 100 characters")
    private String variantSku;

    @Size(max = 100, message = "Variant barcode must not exceed 100 characters")
    private String variantBarcode;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Sale price must be non-negative")
    private BigDecimal salePrice;

    @DecimalMin(value = "0.0", message = "Cost price must be non-negative")
    private BigDecimal costPrice;

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    private List<VariantAttributeRequest> attributes;

    private List<MultipartFile> images;

    private List<WarehouseStockRequest> warehouseStocks;
}
