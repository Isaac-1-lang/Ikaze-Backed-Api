package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ecommerce.enums.BatchStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a batch of stock items
 * Each StockBatch belongs to a specific Stock entry and tracks
 * the lifecycle of a particular batch of products
 */
@Entity
@Table(name = "stock_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the parent Stock entity
     * StockBatch belongs to a specific Stock, which links Product/ProductVariant to
     * Warehouse
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    @JsonBackReference
    private Stock stock;

    /**
     * Unique batch number for tracking purposes
     * Format could be: BATCH-YYYY-NNNN or supplier-specific format
     */
    @NotBlank(message = "Batch number is required")
    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    /**
     * Date when the batch was manufactured
     * Optional field - may not be available for all products
     */
    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    /**
     * Date when the batch expires
     * Required for products with expiry dates (food, medicine, etc.)
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /**
     * Current quantity of items in this batch
     * Must be >= 0
     */
    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity must be zero or positive")
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    /**
     * Current status of the batch
     * Tracks the lifecycle: ACTIVE -> EMPTY/EXPIRED/RECALLED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status = BatchStatus.ACTIVE;

    /**
     * Supplier name for this batch
     * Optional field - tracks which supplier provided this batch
     */
    @Column(name = "supplier_name")
    private String supplierName;

    /**
     * Supplier's batch number for this batch
     * Optional field - tracks the supplier's own batch identifier
     */
    @Column(name = "supplier_batch_number")
    private String supplierBatchNumber;

    /**
     * Cost price for this batch
     * Optional field - tracks the cost price for this specific batch
     */
    @Column(name = "cost_price", precision = 10, scale = 2)
    private java.math.BigDecimal costPrice;

    /**
     * Timestamp when the batch was created
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Timestamp when the batch was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        updateStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateStatus();
    }

    /**
     * Automatically updates the batch status based on current conditions
     */
    private void updateStatus() {
        // Don't override manually set statuses like RECALLED
        if (status == BatchStatus.RECALLED) {
            return;
        }

        // Check if batch is empty
        if (quantity <= 0) {
            status = BatchStatus.EMPTY;
            return;
        }

        // Check if batch has expired
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            status = BatchStatus.EXPIRED;
            return;
        }

        // Default to active if quantity > 0 and not expired
        if (quantity > 0) {
            status = BatchStatus.ACTIVE;
        }
    }

    /**
     * Checks if the batch is currently available for use
     * 
     * @return true if batch is active and has quantity
     */
    public boolean isAvailable() {
        return status == BatchStatus.ACTIVE && quantity > 0;
    }

    /**
     * Checks if the batch has expired
     * 
     * @return true if expiry date has passed
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Checks if the batch is expiring soon
     * 
     * @param daysThreshold number of days to consider as "soon"
     * @return true if batch expires within the threshold
     */
    public boolean isExpiringSoon(int daysThreshold) {
        if (expiryDate == null) {
            return false;
        }
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        return expiryDate.isBefore(thresholdDate) && !isExpired();
    }

    /**
     * Checks if the batch is empty (quantity = 0)
     * 
     * @return true if quantity is zero or negative
     */
    public boolean isEmpty() {
        return quantity <= 0;
    }

    /**
     * Checks if the batch has been recalled
     * 
     * @return true if status is RECALLED
     */
    public boolean isRecalled() {
        return status == BatchStatus.RECALLED;
    }

    /**
     * Reduces the quantity of the batch
     * 
     * @param amount amount to reduce
     * @throws IllegalArgumentException if amount is negative or exceeds available
     *                                  quantity
     */
    public void reduceQuantity(Integer amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (amount > quantity) {
            throw new IllegalArgumentException("Cannot reduce quantity by more than available");
        }
        this.quantity -= amount;
        updateStatus();
    }

    /**
     * Increases the quantity of the batch
     * 
     * @param amount amount to increase
     * @throws IllegalArgumentException if amount is negative
     */
    public void increaseQuantity(Integer amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.quantity += amount;
        updateStatus();
    }

    /**
     * Recalls the batch, setting status to RECALLED
     * 
     * @param reason reason for recall
     */
    public void recall(String reason) {
        this.status = BatchStatus.RECALLED;
        // Could add a reason field if needed
    }

    /**
     * Gets the effective product name (from Stock -> Product/ProductVariant)
     * 
     * @return product name with variant info if applicable
     */
    public String getEffectiveProductName() {
        if (stock != null) {
            return stock.getProductName();
        }
        return "Unknown Product";
    }

    /**
     * Gets the warehouse name (from Stock -> Warehouse)
     * 
     * @return warehouse name
     */
    public String getWarehouseName() {
        if (stock != null && stock.getWarehouse() != null) {
            return stock.getWarehouse().getName();
        }
        return "Unknown Warehouse";
    }
}
