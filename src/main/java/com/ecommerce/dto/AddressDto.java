package com.ecommerce.dto;

import lombok.Data;

@Data
public class AddressDto {
    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
