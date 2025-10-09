package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for comprehensive order search and filtering
 * All fields are optional, but at least one must be provided
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSearchDTO {

    // Basic order identifiers
    private String orderNumber;
    private String userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Status filters
    private String orderStatus;
    private List<String> orderStatuses;
    private String paymentStatus;
    private List<String> paymentStatuses;

    // Location filters
    private String city;
    private String state;
    private String country;
    private String streetAddress;

    // Amount filters
    private BigDecimal totalMin;
    private BigDecimal totalMax;
    private BigDecimal subtotalMin;
    private BigDecimal subtotalMax;
    private BigDecimal taxMin;
    private BigDecimal taxMax;
    private BigDecimal shippingMin;
    private BigDecimal shippingMax;

    // Date filters
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAtMin;
    private LocalDateTime createdAtMax;
    private LocalDateTime updatedAtMin;
    private LocalDateTime updatedAtMax;

    // Payment method filters
    private String paymentMethod;
    private List<String> paymentMethods;

    // Shipping filters
    private String shippingMethod;
    private String trackingNumber;

    // Item filters
    private Integer itemCountMin;
    private Integer itemCountMax;
    private String productName;
    private String productSku;

    // Text search (searches across multiple fields)
    private String searchKeyword;

    // Pagination and sorting
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;

    /**
     * Validate that at least one filter criterion is provided
     */
    public boolean hasAtLeastOneFilter() {
        return (orderNumber != null && !orderNumber.trim().isEmpty()) ||
                (userId != null && !userId.trim().isEmpty()) ||
                (customerName != null && !customerName.trim().isEmpty()) ||
                (customerEmail != null && !customerEmail.trim().isEmpty()) ||
                (customerPhone != null && !customerPhone.trim().isEmpty()) ||
                (orderStatus != null && !orderStatus.trim().isEmpty()) ||
                (orderStatuses != null && !orderStatuses.isEmpty()) ||
                (paymentStatus != null && !paymentStatus.trim().isEmpty()) ||
                (paymentStatuses != null && !paymentStatuses.isEmpty()) ||
                (city != null && !city.trim().isEmpty()) ||
                (state != null && !state.trim().isEmpty()) ||
                (country != null && !country.trim().isEmpty()) ||
                (streetAddress != null && !streetAddress.trim().isEmpty()) ||
                totalMin != null ||
                totalMax != null ||
                subtotalMin != null ||
                subtotalMax != null ||
                taxMin != null ||
                taxMax != null ||
                shippingMin != null ||
                shippingMax != null ||
                startDate != null ||
                endDate != null ||
                createdAtMin != null ||
                createdAtMax != null ||
                updatedAtMin != null ||
                updatedAtMax != null ||
                (paymentMethod != null && !paymentMethod.trim().isEmpty()) ||
                (paymentMethods != null && !paymentMethods.isEmpty()) ||
                (shippingMethod != null && !shippingMethod.trim().isEmpty()) ||
                (trackingNumber != null && !trackingNumber.trim().isEmpty()) ||
                itemCountMin != null ||
                itemCountMax != null ||
                (productName != null && !productName.trim().isEmpty()) ||
                (productSku != null && !productSku.trim().isEmpty()) ||
                (searchKeyword != null && !searchKeyword.trim().isEmpty());
    }
}
