package com.ecommerce.service.impl;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;
import com.ecommerce.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

    @Override
    public void sendOrderConfirmationEmail(Order order, User customer) {
        log.info("Sending order confirmation email to customer: {} for order: {}",
                customer.getUserEmail(), order.getOrderCode());

        // TODO: Implement actual email sending logic
        // For now, just log the action
        String subject = "Order Confirmation - " + order.getOrderCode();
        String message = String.format(
                "Dear %s %s,\n\n" +
                        "Thank you for your order! Your order has been confirmed.\n\n" +
                        "Order Details:\n" +
                        "Order Number: %s\n" +
                        "Total Amount: $%.2f\n" +
                        "Status: %s\n\n" +
                        "We will notify you when your order ships.\n\n" +
                        "Best regards,\n" +
                        "Your E-commerce Team",
                customer.getFirstName(),
                customer.getLastName(),
                order.getOrderCode(),
                order.getOrderInfo() != null ? order.getOrderInfo().getFinalAmount().doubleValue() : 0.0,
                order.getStatus());

        log.info("Email content:\nSubject: {}\nMessage:\n{}", subject, message);
    }

    @Override
    public void sendOrderStatusUpdateEmail(Order order, User customer, String previousStatus, String newStatus) {
        log.info("Sending order status update email to customer: {} for order: {} ({} -> {})",
                customer.getUserEmail(), order.getOrderCode(), previousStatus, newStatus);

        String subject = "Order Status Update - " + order.getOrderCode();
        String message = String.format(
                "Dear %s %s,\n\n" +
                        "Your order status has been updated.\n\n" +
                        "Order Details:\n" +
                        "Order Number: %s\n" +
                        "Previous Status: %s\n" +
                        "New Status: %s\n\n" +
                        "Thank you for your patience.\n\n" +
                        "Best regards,\n" +
                        "Your E-commerce Team",
                customer.getFirstName(),
                customer.getLastName(),
                order.getOrderCode(),
                previousStatus,
                newStatus);

        log.info("Email content:\nSubject: {}\nMessage:\n{}", subject, message);
    }

    @Override
    public void sendOrderCancellationEmail(Order order, User customer) {
        log.info("Sending order cancellation email to customer: {} for order: {}",
                customer.getUserEmail(), order.getOrderCode());

        String subject = "Order Cancelled - " + order.getOrderCode();
        String message = String.format(
                "Dear %s %s,\n\n" +
                        "Your order has been cancelled as requested.\n\n" +
                        "Order Details:\n" +
                        "Order Number: %s\n" +
                        "Cancelled Amount: $%.2f\n\n" +
                        "If you have any questions, please contact our support team.\n\n" +
                        "Best regards,\n" +
                        "Your E-commerce Team",
                customer.getFirstName(),
                customer.getLastName(),
                order.getOrderCode(),
                order.getOrderInfo() != null ? order.getOrderInfo().getFinalAmount().doubleValue() : 0.0);

        log.info("Email content:\nSubject: {}\nMessage:\n{}", subject, message);
    }

    @Override
    public void sendOrderShippedEmail(Order order, User customer, String trackingNumber) {
        log.info("Sending order shipped email to customer: {} for order: {}",
                customer.getUserEmail(), order.getOrderCode());

        String subject = "Your Order Has Shipped - " + order.getOrderCode();
        String message = String.format(
                "Dear %s %s,\n\n" +
                        "Great news! Your order has been shipped.\n\n" +
                        "Order Details:\n" +
                        "Order Number: %s\n" +
                        "Tracking Number: %s\n\n" +
                        "You can track your package using the tracking number above.\n\n" +
                        "Best regards,\n" +
                        "Your E-commerce Team",
                customer.getFirstName(),
                customer.getLastName(),
                order.getOrderCode(),
                trackingNumber != null ? trackingNumber : "Not available");

        log.info("Email content:\nSubject: {}\nMessage:\n{}", subject, message);
    }

    @Override
    public void sendOrderDeliveredEmail(Order order, User customer) {
        log.info("Sending order delivered email to customer: {} for order: {}",
                customer.getUserEmail(), order.getOrderCode());

        String subject = "Your Order Has Been Delivered - " + order.getOrderCode();
        String message = String.format(
                "Dear %s %s,\n\n" +
                        "Your order has been successfully delivered!\n\n" +
                        "Order Details:\n" +
                        "Order Number: %s\n" +
                        "Delivery Date: %s\n\n" +
                        "Thank you for shopping with us. We hope you enjoy your purchase!\n\n" +
                        "Best regards,\n" +
                        "Your E-commerce Team",
                customer.getFirstName(),
                customer.getLastName(),
                order.getOrderCode(),
                order.getUpdatedAt());

        log.info("Email content:\nSubject: {}\nMessage:\n{}", subject, message);
    }

    @Override
    public void sendTrackingUpdateEmail(Order order, User customer, String trackingNumber, String estimatedDelivery) {
        log.info("Sending tracking update email to customer: {} for order: {}",
                customer.getUserEmail(), order.getOrderCode());

        String subject = "Tracking Update - " + order.getOrderCode();
        String message = String.format(
                "Dear %s %s,\n\n" +
                        "Your order tracking information has been updated.\n\n" +
                        "Order Details:\n" +
                        "Order Number: %s\n" +
                        "Tracking Number: %s\n" +
                        "Estimated Delivery: %s\n\n" +
                        "You can track your package using the tracking number above.\n\n" +
                        "Best regards,\n" +
                        "Your E-commerce Team",
                customer.getFirstName(),
                customer.getLastName(),
                order.getOrderCode(),
                trackingNumber != null ? trackingNumber : "Not available",
                estimatedDelivery != null ? estimatedDelivery : "Not available");

        log.info("Email content:\nSubject: {}\nMessage:\n{}", subject, message);
    }
}
