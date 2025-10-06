package com.ecommerce.service.impl;

import com.ecommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.name:E-Commerce Platform}")
    private String appName;

    @Override
    public void sendNewUserInvitationEmail(String toEmail, String firstName, String lastName,
            String invitationToken, String invitedByName,
            String assignedRole, String invitationMessage,
            String department, String position, String expiresAt) {

        try {
            log.info("Sending new user invitation email to: {}", toEmail);

            String subject = String.format("You've been invited to join %s as %s", appName, assignedRole);

            String invitationUrl = String.format("%s/admin-invitation/accept?token=%s", frontendUrl, invitationToken);

            String emailBody = buildNewUserInvitationEmailBody(
                    firstName, lastName, invitationToken, invitedByName, assignedRole,
                    invitationMessage, department, position, expiresAt, invitationUrl);

            sendEmail(toEmail, subject, emailBody);

            log.info("New user invitation email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send new user invitation email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send invitation email", e);
        }
    }

    @Override
    public void sendExistingUserRoleUpdateEmail(String toEmail, String firstName, String lastName,
            String invitedByName, String assignedRole,
            String invitationMessage, String department,
            String position) {

        try {
            log.info("Sending role update email to existing user: {}", toEmail);

            String subject = String.format("Your role has been updated to %s in %s", assignedRole, appName);

            String emailBody = buildExistingUserRoleUpdateEmailBody(
                    firstName, lastName, invitedByName, assignedRole,
                    invitationMessage, department, position);

            sendEmail(toEmail, subject, emailBody);

            log.info("Role update email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send role update email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send role update email", e);
        }
    }

    @Override
    public void sendInvitationResendEmail(String toEmail, String firstName, String lastName,
            String invitationToken, String invitedByName,
            String assignedRole, String invitationMessage,
            String department, String position, String expiresAt) {

        try {
            log.info("Sending invitation resend email to: {}", toEmail);

            String subject = String.format("Invitation Reminder - Join %s as %s", appName, assignedRole);

            String invitationUrl = String.format("%s/admin-invitation/accept?token=%s", frontendUrl, invitationToken);

            String emailBody = buildInvitationResendEmailBody(
                    firstName, lastName, invitationToken, invitedByName, assignedRole,
                    invitationMessage, department, position, expiresAt, invitationUrl);

            sendEmail(toEmail, subject, emailBody);

            log.info("Invitation resend email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send invitation resend email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send invitation resend email", e);
        }
    }

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    @Override
    @Async
    public void sendReturnApprovalEmail(String toEmail, String customerName, Long returnRequestId, 
                                      String orderNumber, String decisionNotes, String returnItems,
                                      String submittedDate, String approvedDate) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Sending return approval email to: {} (async)", toEmail);

                String subject = String.format("Return Request Approved - Order #%s", orderNumber);
                String htmlBody = generateReturnApprovalHtml(customerName, returnRequestId, orderNumber, 
                                                           decisionNotes, returnItems, submittedDate, approvedDate);

                sendHtmlEmailInternal(toEmail, subject, htmlBody);

                log.info("Return approval email sent successfully to: {}", toEmail);

            } catch (Exception e) {
                log.error("Failed to send return approval email to: {}: {}", toEmail, e.getMessage(), e);
                // Don't throw exception in async context to avoid breaking the approval process
            }
        });
    }

    @Override
    @Async
    public void sendReturnDenialEmail(String toEmail, String customerName, Long returnRequestId, 
                                    String orderNumber, String decisionNotes, String returnItems,
                                    String submittedDate, String deniedDate, boolean canAppeal, 
                                    String appealDeadline) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Sending return denial email to: {} (async)", toEmail);

                String subject = String.format("❌ Return Request Decision - Order #%s", orderNumber);
                String htmlBody = generateReturnDenialHtml(customerName, returnRequestId, orderNumber, 
                                                         decisionNotes, returnItems, submittedDate, deniedDate, 
                                                         canAppeal, appealDeadline);

                sendHtmlEmailInternal(toEmail, subject, htmlBody);

                log.info("Return denial email sent successfully to: {}", toEmail);

            } catch (Exception e) {
                log.error("Failed to send return denial email to: {}: {}", toEmail, e.getMessage(), e);
                // Don't throw exception in async context to avoid breaking the denial process
            }
        });
    }

    private String buildNewUserInvitationEmailBody(String firstName, String lastName,
            String invitationToken, String invitedByName,
            String assignedRole, String invitationMessage,
            String department, String position,
            String expiresAt, String invitationUrl) {

        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(firstName).append(" ").append(lastName).append(",\n\n");

        body.append("You have been invited by ").append(invitedByName).append(" to join ")
                .append(appName).append(" as a ").append(assignedRole).append(".\n\n");

        if (department != null && !department.trim().isEmpty()) {
            body.append("Department: ").append(department).append("\n");
        }

        if (position != null && !position.trim().isEmpty()) {
            body.append("Position: ").append(position).append("\n");
        }

        body.append("\n");

        if (invitationMessage != null && !invitationMessage.trim().isEmpty()) {
            body.append("Message from ").append(invitedByName).append(":\n")
                    .append(invitationMessage).append("\n\n");
        }

        body.append("To accept this invitation and set up your account, please click the following link:\n");
        body.append(invitationUrl).append("\n\n");

        body.append("This invitation will expire on: ").append(expiresAt).append("\n\n");

        body.append("Important Notes:\n");
        body.append("- You will need to set a password for your account\n");
        body.append("- You may be asked to provide additional information\n");
        body.append("- This invitation link can only be used once\n\n");

        body.append("If you have any questions, please contact the administrator who sent this invitation.\n\n");

        body.append("Best regards,\n");
        body.append(appName).append(" Team");

        return body.toString();
    }

    private String buildExistingUserRoleUpdateEmailBody(String firstName, String lastName,
            String invitedByName, String assignedRole,
            String invitationMessage, String department,
            String position) {

        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(firstName).append(" ").append(lastName).append(",\n\n");

        body.append("Your role in ").append(appName).append(" has been updated by ")
                .append(invitedByName).append(".\n\n");

        body.append("New Role: ").append(assignedRole).append("\n");

        if (department != null && !department.trim().isEmpty()) {
            body.append("Department: ").append(department).append("\n");
        }

        if (position != null && !position.trim().isEmpty()) {
            body.append("Position: ").append(position).append("\n");
        }

        body.append("\n");

        if (invitationMessage != null && !invitationMessage.trim().isEmpty()) {
            body.append("Message from ").append(invitedByName).append(":\n")
                    .append(invitationMessage).append("\n\n");
        }

        body.append("Your account has been automatically updated with the new role. ");
        body.append("You can now access the additional features and permissions associated with your new role.\n\n");

        body.append("If you have any questions about your new role or need assistance, ");
        body.append("please contact the administrator who made this change.\n\n");

        body.append("Best regards,\n");
        body.append(appName).append(" Team");

        return body.toString();
    }

    private String buildInvitationResendEmailBody(String firstName, String lastName,
            String invitationToken, String invitedByName,
            String assignedRole, String invitationMessage,
            String department, String position,
            String expiresAt, String invitationUrl) {

        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(firstName).append(" ").append(lastName).append(",\n\n");

        body.append("This is a reminder about your invitation to join ").append(appName)
                .append(" as a ").append(assignedRole).append(".\n\n");

        body.append("You were originally invited by ").append(invitedByName)
                .append(", and we're sending you a fresh invitation link.\n\n");

        if (department != null && !department.trim().isEmpty()) {
            body.append("Department: ").append(department).append("\n");
        }

        if (position != null && !position.trim().isEmpty()) {
            body.append("Position: ").append(position).append("\n");
        }

        body.append("\n");

        if (invitationMessage != null && !invitationMessage.trim().isEmpty()) {
            body.append("Message from ").append(invitedByName).append(":\n")
                    .append(invitationMessage).append("\n\n");
        }

        body.append("To accept this invitation and set up your account, please click the following link:\n");
        body.append(invitationUrl).append("\n\n");

        body.append("This invitation will expire on: ").append(expiresAt).append("\n\n");

        body.append("Important Notes:\n");
        body.append("- This is a new invitation link (the previous one has been invalidated)\n");
        body.append("- You will need to set a password for your account\n");
        body.append("- You may be asked to provide additional information\n");
        body.append("- This invitation link can only be used once\n\n");

        body.append("If you have any questions, please contact the administrator who sent this invitation.\n\n");

        body.append("Best regards,\n");
        body.append(appName).append(" Team");

        return body.toString();
    }

    /**
     * Send HTML email using MimeMessage (internal method for async use)
     */
    private void sendHtmlEmailInternal(String toEmail, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true indicates HTML content
        
        mailSender.send(message);
    }

    /**
     * Generate return approval email HTML using Thymeleaf template
     */
    private String generateReturnApprovalHtml(String customerName, Long returnRequestId, String orderNumber, 
                                            String decisionNotes, String returnItems, String submittedDate, 
                                            String approvedDate) {
        Context context = new Context();
        
        // Set template variables
        context.setVariable("customerName", customerName);
        context.setVariable("returnRequestId", returnRequestId);
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("submittedDate", submittedDate);
        context.setVariable("approvedDate", approvedDate);
        context.setVariable("returnItems", returnItems);
        context.setVariable("decisionNotes", decisionNotes);
        
        return templateEngine.process("return-approval", context);
    }

    /**
     * Generate return denial email HTML using Thymeleaf template
     */
    private String generateReturnDenialHtml(String customerName, Long returnRequestId, String orderNumber, 
                                          String decisionNotes, String returnItems, String submittedDate, 
                                          String deniedDate, boolean canAppeal, String appealDeadline) {
        Context context = new Context();
        
        // Set template variables
        context.setVariable("customerName", customerName);
        context.setVariable("returnRequestId", returnRequestId);
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("submittedDate", submittedDate);
        context.setVariable("deniedDate", deniedDate);
        context.setVariable("returnItems", returnItems);
        context.setVariable("decisionNotes", decisionNotes);
        context.setVariable("canAppeal", canAppeal);
        context.setVariable("appealDeadline", appealDeadline);
        
        return templateEngine.process("return-denial", context);
    }

    @Override
    @Async
    public void sendAppealConfirmationEmail(String toEmail, String customerName, Long appealId,
                                          Long returnRequestId, String orderNumber, String appealReason,
                                          String submittedAt, String trackingUrl) {
        try {
            log.info("Sending appeal confirmation email to: {}", toEmail);

            String subject = "Appeal Submitted Successfully - " + appName;
            String htmlBody = generateAppealConfirmationHtml(customerName, appealId, returnRequestId, 
                                                           orderNumber, appealReason, submittedAt, trackingUrl);

            sendHtmlEmailInternal(toEmail, subject, htmlBody);
            
            log.info("Appeal confirmation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send appeal confirmation email to: {}", toEmail, e);
        }
    }

    @Override
    @Async
    public void sendAppealApprovalEmail(String toEmail, String customerName, Long appealId,
                                      Long returnRequestId, String orderNumber, String appealReason,
                                      String decisionNotes, String submittedAt, String approvedAt,
                                      String trackingUrl) {
        try {
            log.info("Sending appeal approval email to: {}", toEmail);

            String subject = String.format("Great News! Your Appeal #%d Has Been Approved - %s", appealId, appName);

            String emailBody = generateAppealApprovalHtml(customerName, appealId, returnRequestId,
                    orderNumber, appealReason, decisionNotes, submittedAt, approvedAt, trackingUrl);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(emailBody, true);

            mailSender.send(message);
            
            log.info("Appeal approval email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send appeal approval email to: {}", toEmail, e);
        }
    }

    @Override
    @Async
    public void sendAppealDenialEmail(String toEmail, String customerName, Long appealId,
                                    Long returnRequestId, String orderNumber, String appealReason,
                                    String decisionNotes, String submittedAt, String deniedAt) {
        try {
            log.info("Sending appeal denial email to: {}", toEmail);

            String subject = String.format("Appeal Decision Update - Appeal #%d - %s", appealId, appName);

            String emailBody = generateAppealDenialHtml(customerName, appealId, returnRequestId,
                    orderNumber, appealReason, decisionNotes, submittedAt, deniedAt);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(emailBody, true);

            mailSender.send(message);
            
            log.info("Appeal denial email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send appeal denial email to: {}", toEmail, e);
        }
    }

    /**
     * Generate appeal confirmation email HTML using Thymeleaf template
     */
    private String generateAppealConfirmationHtml(String customerName, Long appealId, Long returnRequestId,
                                                String orderNumber, String appealReason, String submittedAt,
                                                String trackingUrl) {
        Context context = new Context();
        
        // Set template variables
        context.setVariable("customerName", customerName);
        context.setVariable("appealId", appealId);
        context.setVariable("returnRequestId", returnRequestId);
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("appealReason", appealReason);
        context.setVariable("submittedAt", submittedAt);
        context.setVariable("trackingUrl", trackingUrl);
        
        return templateEngine.process("appeal-confirmation", context);
    }

    /**
     * Generate appeal approval email HTML using Thymeleaf template
     */
    private String generateAppealApprovalHtml(String customerName, Long appealId, Long returnRequestId,
                                            String orderNumber, String appealReason, String decisionNotes,
                                            String submittedAt, String approvedAt, String trackingUrl) {
        Context context = new Context();
        
        // Set template variables
        context.setVariable("customerName", customerName);
        context.setVariable("appealId", appealId);
        context.setVariable("returnRequestId", returnRequestId);
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("appealReason", appealReason);
        context.setVariable("decisionNotes", decisionNotes);
        context.setVariable("submittedAt", submittedAt);
        context.setVariable("approvedAt", approvedAt);
        context.setVariable("trackingUrl", trackingUrl);
        
        return templateEngine.process("appeal-approval", context);
    }

    @Override
    @Async
    public void sendOrderTrackingEmail(String toEmail, String token, String trackingUrl, String expiresAt) {
        try {
            log.info("Sending order tracking email to: {}", toEmail);

            String subject = "Track Your Orders - Secure Access Link";
            
            Context context = new Context();
            context.setVariable("token", token);
            context.setVariable("trackingUrl", trackingUrl);
            context.setVariable("expiryTime", expiresAt);
            context.setVariable("websiteUrl", frontendUrl);
            context.setVariable("supportUrl", frontendUrl + "/support");
            
            String htmlBody = templateEngine.process("order-tracking-email", context);

            sendHtmlEmailInternal(toEmail, subject, htmlBody);
            
            log.info("Order tracking email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send order tracking email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send order tracking email", e);
        }
    }

    /**
     * Generate appeal denial email HTML using Thymeleaf template
     */
    private String generateAppealDenialHtml(String customerName, Long appealId, Long returnRequestId,
                                          String orderNumber, String appealReason, String decisionNotes,
                                          String submittedAt, String deniedAt) {
        Context context = new Context();
        
        // Set template variables
        context.setVariable("customerName", customerName);
        context.setVariable("appealId", appealId);
        context.setVariable("returnRequestId", returnRequestId);
        context.setVariable("orderNumber", orderNumber);
        context.setVariable("appealReason", appealReason);
        context.setVariable("decisionNotes", decisionNotes);
        context.setVariable("submittedAt", submittedAt);
        context.setVariable("deniedAt", deniedAt);
        
        return templateEngine.process("appeal-denial", context);
    }
}
