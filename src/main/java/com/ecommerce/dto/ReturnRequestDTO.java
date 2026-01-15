package com.ecommerce.dto;

import com.ecommerce.entity.ReturnRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.ecommerce.entity.OrderAddress;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDTO {

    private Long id;

    @NotNull(message = "Shop Order ID is required")
    private Long shopOrderId;

    private Long orderId;

    private UUID customerId;

    @NotBlank(message = "Return reason is required")
    private String reason;

    private ReturnRequest.ReturnStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime decisionAt;
    private String decisionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ReturnRequest.DeliveryStatus deliveryStatus;

    // Related data
    private List<ReturnMediaDTO> returnMedia;
    private List<ReturnItemDTO> returnItems;
    private ReturnAppealDTO returnAppeal;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String orderNumber;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private OrderAddress shippingAddress;

    // Delivery agent information
    private UUID deliveryAgentId;
    private String deliveryAgentName;

    // Helper fields
    private boolean canBeAppealed;
    private int daysUntilExpiry;
    private boolean isEligibleForReturn;

    // Expected refund breakdown
    private ExpectedRefundDTO expectedRefund;
}
