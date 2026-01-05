package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopDetailsDTO {
    private UUID shopId;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private String category;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private Boolean isActive;
    private String status;
    private Double rating;
    private Integer totalReviews;
    private Integer productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Owner information
    private OwnerInfo owner;

    // Featured products from this shop
    private List<FeaturedProduct> featuredProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfo {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String avatar;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeaturedProduct {
        private UUID productId;
        private String name;
        private String slug;
        private Double price;
        private Double compareAtPrice;
        private String primaryImage;
        private Double rating;
        private Integer reviewCount;
        private String categoryName;
        private Boolean isInStock;
    }
}
