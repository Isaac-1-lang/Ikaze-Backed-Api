package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAddressDTO {
    private String id;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phone;
}


