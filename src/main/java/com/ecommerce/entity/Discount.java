package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discount_name", nullable = false)
    private String discountName;

    @Column(name = "discount_description", columnDefinition = "TEXT")
    private String discountDescription;

    @Column(name = "percentage", nullable = false)
    private Integer percentage;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "discount")
    private List<Product> products = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Checks if the discount is currently valid based on start and end dates
     * 
     * @return true if the current date is between start and end dates
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active && now.isAfter(startDate) && now.isBefore(endDate);
    }

    /**
     * Adds a product to this discount
     * 
     * @param product The product to add
     * @return The updated discount
     */
    public Discount addProduct(Product product) {
        products.add(product);
        product.setDiscount(this);
        return this;
    }

    /**
     * Removes a product from this discount
     * 
     * @param product The product to remove
     * @return The updated discount
     */
    public Discount removeProduct(Product product) {
        products.remove(product);
        product.setDiscount(null);
        return this;
    }
}