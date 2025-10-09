package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderCustomerInfo;
import com.ecommerce.entity.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final OrderPDFService orderPDFService;

    @Value("${spring.mail.username:support@shopsphere.com}")
    private String fromEmail;

    @Value("${app.base-url:https://shopsphere-frontend.vercel.app}")
    private String baseUrl;

    /**
     * Sends order confirmation email with PDF invoice attachment
     */
    public void sendOrderConfirmationEmail(Order order) {
        try {
            String customerEmail = getCustomerEmail(order);
            if (customerEmail == null) {
                log.warn("No customer email found for order: {}", order.getOrderCode());
                return;
            }

            // Generate PDF invoice
            byte[] pdfInvoice = orderPDFService.generateOrderInvoicePDF(order);

            // Create email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ShopSphere Team");
            helper.setTo(customerEmail);
            helper.setSubject("Order Confirmation - " + order.getOrderCode());

            // Generate HTML content
            String htmlContent = generateOrderConfirmationHtml(order);
            helper.setText(htmlContent, true);

            // Attach PDF invoice
            helper.addAttachment(
                "Order-" + order.getOrderCode() + "-Invoice.pdf",
                new ByteArrayResource(pdfInvoice)
            );

            // Send email
            mailSender.send(message);
            log.info("Order confirmation email sent successfully to {} for order: {}", 
                    customerEmail, order.getOrderCode());

        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order: {}", 
                    order.getOrderCode(), e);
            throw new RuntimeException("Failed to send order confirmation email", e);
        }
    }

    /**
     * Sends order status update email
     */
    public void sendOrderStatusUpdateEmail(Order order, String statusMessage) {
        try {
            String customerEmail = getCustomerEmail(order);
            if (customerEmail == null) {
                log.warn("No customer email found for order: {}", order.getOrderCode());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ShopSphere Team");
            helper.setTo(customerEmail);
            helper.setSubject("Order Update - " + order.getOrderCode());

            String htmlContent = generateOrderStatusUpdateHtml(order, statusMessage);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Order status update email sent successfully to {} for order: {}", 
                    customerEmail, order.getOrderCode());

        } catch (Exception e) {
            log.error("Failed to send order status update email for order: {}", 
                    order.getOrderCode(), e);
        }
    }

    private String getCustomerEmail(Order order) {
        if (order.getOrderCustomerInfo() != null && order.getOrderCustomerInfo().getEmail() != null) {
            return order.getOrderCustomerInfo().getEmail();
        }
        if (order.getUser() != null && order.getUser().getUserEmail() != null) {
            return order.getUser().getUserEmail();
        }
        return null;
    }

    private String generateOrderConfirmationHtml(Order order) {
        Context context = new Context();
        
        // Order details
        context.setVariable("orderNumber", order.getOrderCode());
        context.setVariable("orderDate", order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        context.setVariable("orderStatus", order.getOrderStatus().toString());
        
        // Customer info
        OrderCustomerInfo customerInfo = order.getOrderCustomerInfo();
        if (customerInfo != null) {
            context.setVariable("customerName", customerInfo.getFirstName() + " " + customerInfo.getLastName());
            context.setVariable("customerEmail", customerInfo.getEmail());
        } else if (order.getUser() != null) {
            context.setVariable("customerName", order.getUser().getFirstName() + " " + order.getUser().getLastName());
            context.setVariable("customerEmail", order.getUser().getUserEmail());
        }
        
        // Order totals
        if (order.getOrderInfo() != null) {
            // Calculate subtotal from order items since OrderInfo doesn't have subtotal field
            BigDecimal subtotal = order.getOrderItems().stream()
                    .map(OrderItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            context.setVariable("subtotal", formatCurrency(subtotal));
            context.setVariable("shippingCost", formatCurrency(order.getOrderInfo().getShippingCost()));
            context.setVariable("taxAmount", formatCurrency(order.getOrderInfo().getTaxAmount()));
            context.setVariable("discountAmount", formatCurrency(order.getOrderInfo().getDiscountAmount()));
            context.setVariable("totalAmount", formatCurrency(order.getOrderInfo().getTotalAmount()));
        }
        
        // Points information for points and hybrid payments
        if (order.getOrderTransaction() != null) {
            Integer pointsUsed = order.getOrderTransaction().getPointsUsed();
            BigDecimal pointsValue = order.getOrderTransaction().getPointsValue();
            
            if (pointsUsed != null && pointsUsed > 0) {
                context.setVariable("pointsUsed", pointsUsed);
                context.setVariable("pointsValue", formatCurrency(pointsValue));
                context.setVariable("hasPointsPayment", true);
                
                // Determine payment method
                String paymentMethod = order.getOrderTransaction().getPaymentMethod().toString();
                boolean isHybridPayment = pointsValue != null && 
                    order.getOrderInfo() != null && 
                    pointsValue.compareTo(order.getOrderInfo().getTotalAmount()) < 0;
                
                context.setVariable("isHybridPayment", isHybridPayment);
                context.setVariable("paymentMethod", paymentMethod);
                
                if (isHybridPayment && order.getOrderInfo() != null) {
                    BigDecimal remainingAmount = order.getOrderInfo().getTotalAmount().subtract(pointsValue);
                    context.setVariable("remainingAmount", formatCurrency(remainingAmount));
                }
            } else {
                context.setVariable("hasPointsPayment", false);
            }
        } else {
            context.setVariable("hasPointsPayment", false);
        }
        
        List<OrderItemEmailDTO> emailItems = order.getOrderItems().stream()
                .map(this::convertToEmailDTO)
                .collect(java.util.stream.Collectors.toList());
        context.setVariable("orderItems", emailItems);
        
        // Pickup token
        String pickupToken = order.getPickupToken() != null ? order.getPickupToken() : 
                            "PICKUP-" + order.getOrderCode() + "-" + System.currentTimeMillis();
        context.setVariable("pickupToken", pickupToken);
        
        // URLs
        context.setVariable("baseUrl", baseUrl);
        context.setVariable("trackingUrl", baseUrl + "/orders/" + order.getOrderCode());
        
        return templateEngine.process("order-confirmation", context);
    }

    private String generateOrderStatusUpdateHtml(Order order, String statusMessage) {
        Context context = new Context();
        
        context.setVariable("orderNumber", order.getOrderCode());
        context.setVariable("orderStatus", order.getOrderStatus().toString());
        context.setVariable("statusMessage", statusMessage);
        context.setVariable("baseUrl", baseUrl);
        context.setVariable("trackingUrl", baseUrl + "/orders/" + order.getOrderCode());
        
        // Customer info
        OrderCustomerInfo customerInfo = order.getOrderCustomerInfo();
        if (customerInfo != null) {
            context.setVariable("customerName", customerInfo.getFirstName() + " " + customerInfo.getLastName());
        } else if (order.getUser() != null) {
            context.setVariable("customerName", order.getUser().getFirstName() + " " + order.getUser().getLastName());
        }
        
        return templateEngine.process("order-status-update", context);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%.2f", amount);
    }
    
    private OrderItemEmailDTO convertToEmailDTO(OrderItem item) {
        OrderItemEmailDTO dto = new OrderItemEmailDTO();
        
        // Get product name
        if (item.isVariantBased() && item.getProductVariant() != null) {
            dto.setProductName(item.getProductVariant().getProduct().getProductName());
            dto.setVariantName(item.getProductVariant().getVariantName());
        } else if (item.getProduct() != null) {
            dto.setProductName(item.getProduct().getProductName());
            dto.setVariantName(null);
        } else {
            dto.setProductName("Unknown Product");
            dto.setVariantName(null);
        }
        
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(formatCurrency(item.getPrice()));
        dto.setTotalPrice(formatCurrency(item.getSubtotal()));
        
        return dto;
    }
    
    // DTO class for email template
    public static class OrderItemEmailDTO {
        private String productName;
        private String variantName;
        private Integer quantity;
        private String unitPrice;
        private String totalPrice;
        
        // Getters and setters
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public String getVariantName() { return variantName; }
        public void setVariantName(String variantName) { this.variantName = variantName; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public String getUnitPrice() { return unitPrice; }
        public void setUnitPrice(String unitPrice) { this.unitPrice = unitPrice; }
        
        public String getTotalPrice() { return totalPrice; }
        public void setTotalPrice(String totalPrice) { this.totalPrice = totalPrice; }
    }
}
