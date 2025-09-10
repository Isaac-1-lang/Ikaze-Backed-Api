package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWarehouseDTO {
    private String name;
    private String description;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phone;

    @Email(message = "Email should be valid")
    private String email;

    @Positive(message = "Capacity must be positive")
    private Integer capacity;

    private Double latitude;
    private Double longitude;

    private Boolean isActive;
}
