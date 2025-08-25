package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;

public interface EmailNotificationService {
    
    /**
     * Send order confirmation email to customer
     */
    void sendOrderConfirmationEmail(Order order, User customer);
    
    /**
     * Send order status update email to customer
     */
    void sendOrderStatusUpdateEmail(Order order, User customer, String previousStatus, String newStatus);
    
    /**
     * Send order cancellation email to customer
     */
    void sendOrderCancellationEmail(Order order, User customer);
    
    /**
     * Send order shipped email to customer
     */
    void sendOrderShippedEmail(Order order, User customer, String trackingNumber);
    
    /**
     * Send order delivered email to customer
     */
    void sendOrderDeliveredEmail(Order order, User customer);
    
    /**
     * Send order tracking update email to customer
     */
    void sendTrackingUpdateEmail(Order order, User customer, String trackingNumber, String estimatedDelivery);
}
