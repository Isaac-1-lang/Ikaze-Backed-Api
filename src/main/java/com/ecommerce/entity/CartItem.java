package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant productVariant;

    @Column(name = "quantity")
    private int quantity;
    
    @Column(name = "added_at")
    private LocalDateTime addedAt;
    
    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
    
    /**
     * Sets the quantity of this cart item, ensuring it doesn't exceed the available stock.
     * 
     * @param quantity The quantity to set
     * @throws IllegalArgumentException if quantity exceeds available stock
     */
    public void setQuantity(int quantity) {
        if (productVariant != null && quantity > productVariant.getStockQuantity()) {
            throw new IllegalArgumentException("Quantity cannot exceed available stock of " + productVariant.getStockQuantity());
        }
        this.quantity = quantity;
    }
}