package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

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
     * Adds an item to the cart
     * 
     * @param item The cart item to add
     */
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }
    
    /**
     * Removes an item from the cart
     * 
     * @param item The cart item to remove
     */
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }
    
    /**
     * Clears all items from the cart
     */
    public void clear() {
        items.clear();
    }

    /**
     * Gets the total number of items in the cart
     * 
     * @return The total quantity of all items
     */
    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    /**
     * Checks if the cart is empty
     * 
     * @return true if the cart has no items, false otherwise
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
}