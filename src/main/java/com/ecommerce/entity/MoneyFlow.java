package com.ecommerce.entity;

import com.ecommerce.enums.MoneyFlowType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "money_flow", indexes = {
    @Index(name = "idx_money_flow_created_at", columnList = "created_at"),
    @Index(name = "idx_money_flow_type", columnList = "type"),
    @Index(name = "idx_money_flow_type_created_at", columnList = "type, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoneyFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 500, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MoneyFlowType type;

    @NotNull(message = "Amount is required")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Remaining balance is required")
    @Column(name = "remaining_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
