package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class WarehouseDTO {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phone;
    private String email;
    private Integer capacity;
    private Double latitude;
    private Double longitude;
    private Boolean isActive;
    private Integer productCount;
    private UUID shopId;
    private String shopName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WarehouseImageDTO> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarehouseImageDTO {
        private Long id;
        private String imageUrl;
        private Boolean isPrimary;
        private Integer sortOrder;
        private Integer width;
        private Integer height;
        private Long fileSize;
        private String mimeType;
    }
}