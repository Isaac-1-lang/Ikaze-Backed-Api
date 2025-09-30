package com.ecommerce.service;

public interface EmailService {

        /**
         * Send invitation email to a new user with invitation token
         * 
         * @param toEmail           recipient email
         * @param firstName         recipient first name
         * @param lastName          recipient last name
         * @param invitationToken   unique invitation token
         * @param invitedByName     name of the admin who sent the invitation
         * @param assignedRole      role being assigned
         * @param invitationMessage optional custom message
         * @param department        optional department
         * @param position          optional position
         * @param expiresAt         when the invitation expires
         */
        void sendNewUserInvitationEmail(String toEmail, String firstName, String lastName,
                        String invitationToken, String invitedByName,
                        String assignedRole, String invitationMessage,
                        String department, String position, String expiresAt);

        /**
         * Send role update notification email to existing user
         * 
         * @param toEmail           recipient email
         * @param firstName         recipient first name
         * @param lastName          recipient last name
         * @param invitedByName     name of the admin who sent the invitation
         * @param assignedRole      new role being assigned
         * @param invitationMessage optional custom message
         * @param department        optional department
         * @param position          optional position
         */
        void sendExistingUserRoleUpdateEmail(String toEmail, String firstName, String lastName,
                        String invitedByName, String assignedRole,
                        String invitationMessage, String department,
                        String position);

        /**
         * Send invitation resend email to a new user with new invitation token
         * 
         * @param toEmail           recipient email
         * @param firstName         recipient first name
         * @param lastName          recipient last name
         * @param invitationToken   new invitation token
         * @param invitedByName     name of the admin who resent the invitation
         * @param assignedRole      role being assigned
         * @param invitationMessage optional custom message
         * @param department        optional department
         * @param position          optional position
         * @param expiresAt         when the invitation expires
         */
        void sendInvitationResendEmail(String toEmail, String firstName, String lastName,
                        String invitationToken, String invitedByName,
                        String assignedRole, String invitationMessage,
                        String department, String position, String expiresAt);

        /**
         * Send a generic email (e.g., password reset, notification) to a user
         * 
         * @param toEmail recipient email
         * @param subject email subject
         * @param body    email body content
         */
        void sendEmail(String toEmail, String subject, String body);

        /**
         * Send return request approval email with HTML template
         * 
         * @param toEmail           recipient email
         * @param customerName      customer's full name
         * @param returnRequestId   return request ID
         * @param orderNumber       order number
         * @param decisionNotes     admin's decision notes
         * @param returnItems       list of return items
         * @param submittedDate     when the return was submitted
         * @param approvedDate      when the return was approved
         */
        void sendReturnApprovalEmail(String toEmail, String customerName, Long returnRequestId, 
                                   String orderNumber, String decisionNotes, String returnItems,
                                   String submittedDate, String approvedDate);

        /**
         * Send return request denial email with HTML template
         * 
         * @param toEmail           recipient email
         * @param customerName      customer's full name
         * @param returnRequestId   return request ID
         * @param orderNumber       order number
         * @param decisionNotes     admin's decision notes
         * @param returnItems       list of return items
         * @param submittedDate     when the return was submitted
         * @param deniedDate        when the return was denied
         * @param canAppeal         whether customer can appeal this decision
         * @param appealDeadline    deadline for submitting an appeal
         */
        void sendReturnDenialEmail(String toEmail, String customerName, Long returnRequestId, 
                                 String orderNumber, String decisionNotes, String returnItems,
                                 String submittedDate, String deniedDate, boolean canAppeal, 
                                 String appealDeadline);
}
