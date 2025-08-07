package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wishlists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<WishlistProduct> wishlistProducts = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Adds a product variant to the wishlist
     * 
     * @param wishlistProduct The wishlist product to add
     * @return The updated wishlist
     */
    public Wishlist addProduct(WishlistProduct wishlistProduct) {
        wishlistProducts.add(wishlistProduct);
        wishlistProduct.setWishlist(this);
        return this;
    }
    
    /**
     * Removes a product variant from the wishlist
     * 
     * @param wishlistProduct The wishlist product to remove
     * @return The updated wishlist
     */
    public Wishlist removeProduct(WishlistProduct wishlistProduct) {
        wishlistProducts.remove(wishlistProduct);
        wishlistProduct.setWishlist(null);
        return this;
    }
    
    /**
     * Checks if the wishlist is empty
     * 
     * @return true if the wishlist has no products, false otherwise
     */
    public boolean isEmpty() {
        return wishlistProducts.isEmpty();
    }
    
    /**
     * Gets the number of products in the wishlist
     * 
     * @return The number of products
     */
    public int getProductCount() {
        return wishlistProducts.size();
    }
}