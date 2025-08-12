package com.ecommerce.service.impl;

import com.ecommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

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

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
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
}
