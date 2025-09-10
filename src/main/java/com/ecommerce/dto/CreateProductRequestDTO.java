package com.ecommerce.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.ecommerce.dto.ImageMetadata;

@Data
public class CreateProductRequestDTO {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 50, message = "SKU must be between 3 and 50 characters")
    private String sku;

    @Size(max = 50, message = "Barcode must not exceed 50 characters")
    private String barcode;

    @NotNull(message = "Base price is required")
    @Positive(message = "Base price must be positive")
    private BigDecimal basePrice;

    @Positive(message = "Sale price must be positive")
    private BigDecimal salePrice;

    @Positive(message = "Cost price must be positive")
    private BigDecimal costPrice;

    @Positive(message = "Stock quantity must be positive")
    private Integer stockQuantity;

    @Positive(message = "Low stock threshold must be positive")
    private Integer lowStockThreshold;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private UUID brandId;
    private UUID discountId;

    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    @Size(max = 255, message = "Slug must not exceed 255 characters")
    private String slug;

    private Boolean isActive;
    private Boolean isFeatured;
    private Boolean isBestseller;
    private Boolean isNewArrival;
    // File uploads - these will be handled separately
    private List<MultipartFile> productImages;
    private List<MultipartFile> productVideos;

    // Metadata as strings
    private String imageMetadata;
    private String videoMetadata;

    // Variants - will be sent as JSON string from frontend
    private String variants;

    // Variant images - these will be handled separately for each variant
    private List<MultipartFile> variantImages;
    private String variantImageMetadata;

    private String variantImageMapping; // JSON string mapping variant index to image indices

    // Convert to CreateProductDTO for service layer
    public CreateProductDTO toCreateProductDTO() {
        CreateProductDTO dto = new CreateProductDTO();
        dto.setName(this.name);
        dto.setDescription(this.description);
        dto.setSku(this.sku);
        dto.setBarcode(this.barcode);
        dto.setBasePrice(this.basePrice);
        dto.setSalePrice(this.salePrice);
        dto.setCostPrice(this.costPrice);
        // TODO: These methods are deprecated - use warehouseStock instead
        // dto.setStockQuantity(this.stockQuantity);
        // dto.setLowStockThreshold(this.lowStockThreshold);
        dto.setCategoryId(this.categoryId);
        dto.setBrandId(this.brandId);
        dto.setDiscountId(this.discountId);
        dto.setModel(this.model);
        dto.setSlug(this.slug);
        dto.setIsActive(this.isActive);
        dto.setIsFeatured(this.isFeatured);
        dto.setIsBestseller(this.isBestseller);
        dto.setIsNewArrival(this.isNewArrival);
        if (this.imageMetadata != null && !this.imageMetadata.trim().isEmpty()) {
            try {
                List<ImageMetadata> parsedImageMetadata = parseImageMetadataFromJson(this.imageMetadata);
                dto.setImageMetadata(parsedImageMetadata);
            } catch (Exception e) {
                System.err.println("Failed to parse image metadata JSON: " + e.getMessage());
                e.printStackTrace();
            }
        }

        dto.setVideoMetadata(this.videoMetadata);
        if (this.variants != null && !this.variants.trim().isEmpty()) {
            try {
                System.out.println("Raw variants JSON: " + this.variants);
                List<CreateProductVariantDTO> parsedVariants = parseVariantsFromJson(this.variants);
                System.out.println("Parsed variants count: " + parsedVariants.size());
                dto.setVariants(parsedVariants);
            } catch (Exception e) {
                System.err.println("Failed to parse variants JSON: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No variants provided or variants is empty");
        }

        // Set variant images and mapping
        dto.setVariantImages(this.variantImages);
        dto.setVariantImageMapping(this.variantImageMapping);

        return dto;
    }

    // Helper method to parse variants from JSON
    private List<CreateProductVariantDTO> parseVariantsFromJson(String variantsJson) {
        try {
            // Use Jackson ObjectMapper to parse JSON
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Parse the JSON string to List<CreateProductVariantDTO>
            return objectMapper.readValue(variantsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CreateProductVariantDTO.class));
        } catch (Exception e) {
            System.err.println("Failed to parse variants JSON: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Helper method to parse image metadata from JSON
    private List<ImageMetadata> parseImageMetadataFromJson(String imageMetadataJson) {
        try {
            // Use Jackson ObjectMapper to parse JSON
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Parse the JSON string to List<ImageMetadata>
            return objectMapper.readValue(imageMetadataJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ImageMetadata.class));
        } catch (Exception e) {
            System.err.println("Failed to parse image metadata JSON: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
