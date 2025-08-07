package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_transaction_id")
    private Long orderTransactionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_ref")
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "receipt_url")
    private String receiptUrl;

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
     * Payment method enum
     */
    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER, CASH_ON_DELIVERY, APPLE_PAY, GOOGLE_PAY
    }

    /**
     * Transaction status enum
     */
    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED
    }

    /**
     * Checks if the transaction is successful
     * 
     * @return True if the transaction is successful, false otherwise
     */
    @Transient
    public boolean isSuccessful() {
        return status == TransactionStatus.COMPLETED;
    }

    /**
     * Checks if the transaction is pending
     * 
     * @return True if the transaction is pending, false otherwise
     */
    @Transient
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    /**
     * Checks if the transaction has failed
     * 
     * @return True if the transaction has failed, false otherwise
     */
    @Transient
    public boolean hasFailed() {
        return status == TransactionStatus.FAILED || status == TransactionStatus.CANCELLED;
    }
}