package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_batch_locks")
@Getter
@Setter
public class StockBatchLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_batch_id", nullable = false)
    private StockBatch stockBatch;

    @Column(name = "locked_quantity", nullable = false)
    private Integer lockedQuantity;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "product_id")
    private java.util.UUID productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Constructors
    public StockBatchLock() {
    }

    public StockBatchLock(String sessionId, StockBatch stockBatch, Integer lockedQuantity, 
                         Long warehouseId, String warehouseName) {
        this.sessionId = sessionId;
        this.stockBatch = stockBatch;
        this.lockedQuantity = lockedQuantity;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(2);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
