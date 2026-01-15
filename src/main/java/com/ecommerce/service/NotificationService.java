package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.ReturnAppeal;
import com.ecommerce.entity.ReturnRequest;
import com.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for handling notifications related to returns and appeals
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final OrderRepository orderRepository;

    /**
     * Notify customer that return request has been submitted
     */
    public void notifyReturnSubmitted(ReturnRequest returnRequest) {
        try {
            Order order = orderRepository.findById(returnRequest.getShopOrder().getOrder().getOrderId())
                    .orElseThrow(
                            () -> new RuntimeException("Order not found for return request: " + returnRequest.getId()));

            if (order.getOrderCustomerInfo() != null && order.getOrderCustomerInfo().getEmail() != null) {
                String customerEmail = order.getOrderCustomerInfo().getEmail();
                String customerName = order.getOrderCustomerInfo().getFullName();

                sendReturnRequestSubmittedEmail(customerEmail, customerName, returnRequest, order);
            } else {
                log.warn("No customer email found for return request {}, skipping email notification",
                        returnRequest.getId());
            }

        } catch (Exception e) {
            log.error("Failed to send return submission notification for request {}: {}",
                    returnRequest.getId(), e.getMessage(), e);
            // Don't throw exception to avoid breaking the return submission process
        }
    }

    public void notifyReturnApproved(ReturnRequest returnRequest) {
        log.info("Sending return approval notification to customer {} for request {}",
                returnRequest.getCustomerId(), returnRequest.getId());

        try {
            Order order = orderRepository.findById(returnRequest.getShopOrder().getOrder().getOrderId())
                    .orElseThrow(
                            () -> new RuntimeException("Order not found for return request: " + returnRequest.getId()));

            String customerEmail = getCustomerEmail(returnRequest, order);
            String customerName = getCustomerName(returnRequest, order);

            if (customerEmail != null && customerName != null) {
                String returnItems = buildReturnItemsList(returnRequest);
                String submittedDate = returnRequest.getSubmittedAt().toLocalDate().toString();
                String approvedDate = returnRequest.getDecisionAt() != null
                        ? returnRequest.getDecisionAt().toLocalDate().toString()
                        : java.time.LocalDate.now().toString();

                emailService.sendReturnApprovalEmail(
                        customerEmail,
                        customerName,
                        returnRequest.getId(),
                        order.getOrderCode(),
                        returnRequest.getDecisionNotes(),
                        returnItems,
                        submittedDate,
                        approvedDate);

                log.info("Return approval email sent successfully to {} for request {}",
                        customerEmail, returnRequest.getId());
            } else {
                log.warn("No customer email found for return request {}, skipping email notification",
                        returnRequest.getId());
            }

        } catch (Exception e) {
            log.error("Failed to send return approval notification for request {}: {}",
                    returnRequest.getId(), e.getMessage(), e);
            // Don't throw exception to avoid breaking the approval process
        }
    }

    /**
     * Notify customer that return request has been denied
     */
    public void notifyReturnDenied(ReturnRequest returnRequest) {
        log.info("Sending return denial notification to customer {} for request {}",
                returnRequest.getCustomerId(), returnRequest.getId());

        try {
            Order order = orderRepository.findById(returnRequest.getShopOrder().getOrder().getOrderId())
                    .orElseThrow(
                            () -> new RuntimeException("Order not found for return request: " + returnRequest.getId()));

            String customerEmail = getCustomerEmail(returnRequest, order);
            String customerName = getCustomerName(returnRequest, order);

            if (customerEmail != null && customerName != null) {
                String returnItems = buildReturnItemsList(returnRequest);
                String submittedDate = returnRequest.getSubmittedAt().toLocalDate().toString();
                String deniedDate = returnRequest.getDecisionAt() != null
                        ? returnRequest.getDecisionAt().toLocalDate().toString()
                        : java.time.LocalDate.now().toString();

                boolean canAppeal = returnRequest.canBeAppealed();
                String appealDeadline = canAppeal ? java.time.LocalDate.now().plusDays(7).toString() : // 7 days to
                                                                                                       // appeal
                        null;

                emailService.sendReturnDenialEmail(
                        customerEmail,
                        customerName,
                        returnRequest.getId(),
                        order.getOrderCode(),
                        returnRequest.getDecisionNotes(),
                        returnItems,
                        submittedDate,
                        deniedDate,
                        canAppeal,
                        appealDeadline);

                log.info("Return denial email sent successfully to {} for request {}",
                        customerEmail, returnRequest.getId());
            } else {
                log.warn("No customer email found for return request {}, skipping email notification",
                        returnRequest.getId());
            }

        } catch (Exception e) {
            log.error("Failed to send return denial notification for request {}: {}",
                    returnRequest.getId(), e.getMessage(), e);
            // Don't throw exception to avoid breaking the denial process
        }
    }

    /**
     * Notify customer that returned item has been received
     */
    public void notifyReturnReceived(ReturnRequest returnRequest) {
        log.info("Sending return received notification to customer {} for request {}",
                returnRequest.getCustomerId(), returnRequest.getId());

        // emailService.sendReturnReceivedEmail(returnRequest);

        log.info("Return received notification sent for request {}", returnRequest.getId());
    }

    public void notifyAppealApproved(ReturnAppeal appeal, ReturnRequest returnRequest) {
        log.info("Sending appeal approval notification to customer {} for appeal {}",
                returnRequest.getCustomerId(), appeal.getId());

        // emailService.sendAppealApprovedEmail(appeal, returnRequest);

        log.info("Appeal approval notification sent for appeal {}", appeal.getId());
    }

    /**
     * Notify customer that appeal has been denied (final decision)
     */
    public void notifyAppealDenied(ReturnAppeal appeal, ReturnRequest returnRequest) {
        log.info("Sending final appeal denial notification to customer {} for appeal {}",
                returnRequest.getCustomerId(), appeal.getId());

        // emailService.sendAppealDeniedEmail(appeal, returnRequest);

        log.info("Final appeal denial notification sent for appeal {}", appeal.getId());
    }

    public void notifyRefundProcessed(ReturnRequest returnRequest, String refundMethod, Double amount) {
        log.info("Sending refund processed notification to customer {} for request {} - amount: {}",
                returnRequest.getCustomerId(), returnRequest.getId(), amount);

        // emailService.sendRefundProcessedEmail(returnRequest, refundMethod, amount);

        log.info("Refund processed notification sent for request {}", returnRequest.getId());
    }

    /**
     * Send return request submitted email to customer
     */
    private void sendReturnRequestSubmittedEmail(String customerEmail, String customerName,
            ReturnRequest returnRequest, Order order) {
        try {
            String subject = "Return Request Submitted Successfully - Order #" + order.getOrderCode();
            String emailBody = buildReturnRequestSubmittedEmailBody(customerName, returnRequest, order);

            emailService.sendEmail(customerEmail, subject, emailBody);
        } catch (Exception e) {
            log.error("Failed to send return request submitted email to: {}", customerEmail, e);
            throw new RuntimeException("Failed to send return request submitted email", e);
        }
    }

    /**
     * Build the email body for return request submitted notification
     */
    private String buildReturnRequestSubmittedEmailBody(String customerName,
            ReturnRequest returnRequest, Order order) {
        StringBuilder body = new StringBuilder();

        body.append("Dear ").append(customerName).append(",\n\n");

        body.append(
                "Thank you for submitting your return request. We have successfully received your request and it is now being processed.\n\n");

        body.append("Return Request Details:\n");
        body.append("- Return Request ID: #").append(returnRequest.getId()).append("\n");
        body.append("- Order Number: #").append(order.getOrderCode()).append("\n");
        body.append("- Reason for Return: ").append(returnRequest.getReason()).append("\n");
        body.append("- Request Status: ").append(returnRequest.getStatus().toString()).append("\n");
        body.append("- Submitted Date: ").append(returnRequest.getSubmittedAt().toLocalDate()).append("\n\n");

        body.append("What happens next?\n");
        body.append("1. Our team will review your return request within 1-2 business days\n");
        body.append(
                "2. You will receive an email notification with the decision (approved or requires additional information)\n");
        body.append("3. If approved, you will receive instructions on how to return the item(s)\n");
        body.append("4. Once we receive and inspect the returned item(s), we will process your refund\n\n");

        body.append("Important Notes:\n");
        body.append("- Please keep this email for your records\n");
        body.append(
                "- You will receive email notifications at this address for any updates regarding your return request\n");
        body.append("- If you have any questions, please contact our customer service team\n\n");

        body.append("We appreciate your business and will process your return request as quickly as possible.\n\n");

        body.append("Best regards,\n");
        body.append("Customer Service Team\n");
        body.append("ShopSphere E-commerce Platform");

        return body.toString();
    }

    /**
     * Get customer email from return request and order
     */
    private String getCustomerEmail(ReturnRequest returnRequest, Order order) {
        // For registered customers
        if (returnRequest.getCustomerId() != null && returnRequest.getCustomer() != null) {
            return returnRequest.getCustomer().getUserEmail();
        }
        // For guest customers
        else if (order.getOrderCustomerInfo() != null) {
            return order.getOrderCustomerInfo().getEmail();
        }
        return null;
    }

    /**
     * Get customer name from return request and order
     */
    private String getCustomerName(ReturnRequest returnRequest, Order order) {
        // For registered customers
        if (returnRequest.getCustomerId() != null && returnRequest.getCustomer() != null) {
            return returnRequest.getCustomer().getFirstName() + " " + returnRequest.getCustomer().getLastName();
        }
        // For guest customers
        else if (order.getOrderCustomerInfo() != null) {
            return order.getOrderCustomerInfo().getFullName();
        }
        return "Valued Customer";
    }

    /**
     * Build a formatted list of return items
     */
    private String buildReturnItemsList(ReturnRequest returnRequest) {
        if (returnRequest.getReturnItems() == null || returnRequest.getReturnItems().isEmpty()) {
            return "No items specified";
        }

        StringBuilder itemsList = new StringBuilder();
        for (var item : returnRequest.getReturnItems()) {
            try {
                String productName = "Unknown Product";
                String variantName = "";

                if (item.getProduct() != null) {
                    productName = item.getProduct().getProductName();
                } else if (item.getProductVariant() != null && item.getProductVariant().getProduct() != null) {
                    productName = item.getProductVariant().getProduct().getProductName();
                    variantName = " (" + item.getProductVariant().getVariantName() + ")";
                }

                itemsList.append("• ")
                        .append(productName)
                        .append(variantName)
                        .append(" - Quantity: ")
                        .append(item.getReturnQuantity());

                if (item.getItemReason() != null && !item.getItemReason().trim().isEmpty()) {
                    itemsList.append(" - Reason: ").append(item.getItemReason());
                }

                itemsList.append("\n");
            } catch (Exception e) {
                log.warn("Error building item description for return item: {}", e.getMessage());
                itemsList.append("• Item - Quantity: ").append(item.getReturnQuantity()).append("\n");
            }
        }

        return itemsList.toString();
    }
}
