package com.ecommerce.service;

import com.ecommerce.dto.OrderActivityLogDTO;
import com.ecommerce.entity.OrderActivityLog;
import com.ecommerce.repository.OrderActivityLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderActivityLogService {

        private final OrderActivityLogRepository activityLogRepository;
        private final ObjectMapper objectMapper;
        private final com.ecommerce.repository.OrderTrackingTokenRepository orderTrackingTokenRepository;
        private final com.ecommerce.repository.OrderRepository orderRepository;
        private final com.ecommerce.repository.OrderDeliveryNoteRepository orderDeliveryNoteRepository;

        @Transactional
        public OrderActivityLog logActivity(
                        Long orderId,
                        OrderActivityLog.ActivityType activityType,
                        String title,
                        String description,
                        String actorType,
                        String actorId,
                        String actorName,
                        String referenceId,
                        String referenceType,
                        Map<String, Object> metadata) {
                try {
                        // Get the order entity for proper relationship
                        com.ecommerce.entity.Order order = orderRepository.findById(orderId)
                                        .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

                        OrderActivityLog activityLog = OrderActivityLog.builder()
                                        .order(order)
                                        .activityType(activityType)
                                        .title(title)
                                        .description(description)
                                        .timestamp(LocalDateTime.now())
                                        .actorType(actorType)
                                        .actorId(actorId)
                                        .actorName(actorName)
                                        .referenceId(referenceId)
                                        .referenceType(referenceType)
                                        .metadata(metadata != null ? objectMapper.writeValueAsString(metadata) : null)
                                        .build();

                        OrderActivityLog saved = activityLogRepository.save(activityLog);
                        return saved;

                } catch (JsonProcessingException e) {
                        log.error("Failed to serialize metadata for order activity log: {}", e.getMessage());

                        // Get the order entity for proper relationship
                        com.ecommerce.entity.Order order = orderRepository.findById(orderId)
                                        .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

                        OrderActivityLog activityLog = OrderActivityLog.builder()
                                        .order(order)
                                        .activityType(activityType)
                                        .title(title)
                                        .description(description)
                                        .timestamp(LocalDateTime.now())
                                        .actorType(actorType)
                                        .actorId(actorId)
                                        .actorName(actorName)
                                        .referenceId(referenceId)
                                        .referenceType(referenceType)
                                        .build();

                        return activityLogRepository.save(activityLog);
                }
        }

        /**
         * Simplified method to log activity without metadata
         */
        @Transactional
        public OrderActivityLog logActivity(
                        Long orderId,
                        OrderActivityLog.ActivityType activityType,
                        String title,
                        String description) {
                return logActivity(orderId, activityType, title, description,
                                "SYSTEM", null, "System", null, null, null);
        }

        /**
         * Log activity with actor information
         */
        @Transactional
        public OrderActivityLog logActivityWithActor(
                        Long orderId,
                        OrderActivityLog.ActivityType activityType,
                        String title,
                        String description,
                        String actorType,
                        String actorId,
                        String actorName) {
                return logActivity(orderId, activityType, title, description,
                                actorType, actorId, actorName, null, null, null);
        }

        /**
         * Get complete timeline for an order
         */
        @Transactional(readOnly = true)
        public List<OrderActivityLogDTO> getOrderTimeline(Long orderId) {
                log.info("Fetching complete timeline for order {}", orderId);
                List<OrderActivityLog> logs = activityLogRepository.findByOrderIdOrderByTimestampAsc(orderId);
                return logs.stream()
                                .map(OrderActivityLogDTO::fromEntity)
                                .map(this::enrichWithDeliveryNote)
                                .collect(Collectors.toList());
        }

        /**
         * Enrich activity DTO with delivery note details if applicable
         */
        private OrderActivityLogDTO enrichWithDeliveryNote(OrderActivityLogDTO dto) {
                if ("DELIVERY_NOTE".equals(dto.getReferenceType()) && dto.getReferenceId() != null) {
                        try {
                                Long noteId = Long.parseLong(dto.getReferenceId());
                                orderDeliveryNoteRepository.findById(noteId).ifPresent(note -> {
                                        dto.setDeliveryNote(com.ecommerce.dto.OrderDeliveryNoteDTO.fromEntity(note));
                                });
                        } catch (NumberFormatException e) {
                                log.warn("Invalid delivery note ID format: {}", dto.getReferenceId());
                        }
                }
                return dto;
        }

        @Transactional(readOnly = true)
        public List<OrderActivityLogDTO> getOrderTimelineWithToken(Long orderId, String token) {
                log.info("Fetching timeline for order {} with token validation", orderId);

                com.ecommerce.entity.Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

                com.ecommerce.entity.OrderTrackingToken trackingToken = orderTrackingTokenRepository
                                .findValidToken(token, LocalDateTime.now())
                                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired tracking token"));

                String orderEmail = null;
                if (order.getUser() != null) {
                        orderEmail = order.getUser().getUserEmail();
                } else if (order.getOrderCustomerInfo() != null) {
                        orderEmail = order.getOrderCustomerInfo().getEmail();
                }

                if (orderEmail == null || !orderEmail.equalsIgnoreCase(trackingToken.getEmail())) {
                        throw new IllegalArgumentException("Token does not match order");
                }

                List<OrderActivityLog> logs = activityLogRepository.findByOrderIdOrderByTimestampAsc(orderId);
                return logs.stream()
                                .map(OrderActivityLogDTO::fromEntity)
                                .map(this::enrichWithDeliveryNote)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<OrderActivityLogDTO> getOrderTimelineByDateRange(
                        Long orderId,
                        LocalDateTime startDate,
                        LocalDateTime endDate) {
                log.info("Fetching timeline for order {} between {} and {}", orderId, startDate, endDate);
                List<OrderActivityLog> logs = activityLogRepository.findByOrderIdAndDateRange(
                                orderId, startDate, endDate);
                return logs.stream()
                                .map(OrderActivityLogDTO::fromEntity)
                                .map(this::enrichWithDeliveryNote)
                                .collect(Collectors.toList());
        }

        /**
         * Get activities by type
         */
        @Transactional(readOnly = true)
        public List<OrderActivityLogDTO> getActivitiesByType(
                        Long orderId,
                        OrderActivityLog.ActivityType activityType) {
                log.info("Fetching {} activities for order {}", activityType, orderId);
                List<OrderActivityLog> logs = activityLogRepository
                                .findByOrderIdAndActivityTypeOrderByTimestampAsc(orderId, activityType);
                return logs.stream()
                                .map(OrderActivityLogDTO::fromEntity)
                                .map(this::enrichWithDeliveryNote)
                                .collect(Collectors.toList());
        }

        /**
         * Get recent activities
         */
        @Transactional(readOnly = true)
        public List<OrderActivityLogDTO> getRecentActivities(Long orderId, int limit) {
                log.info("Fetching {} recent activities for order {}", limit, orderId);
                List<OrderActivityLog> logs = activityLogRepository.findRecentByOrderId(orderId);
                return logs.stream()
                                .limit(limit)
                                .map(OrderActivityLogDTO::fromEntity)
                                .map(this::enrichWithDeliveryNote)
                                .collect(Collectors.toList());
        }

        /**
         * Count total activities for an order
         */
        @Transactional(readOnly = true)
        public long countActivities(Long orderId) {
                return activityLogRepository.countByOrderId(orderId);
        }

        /**
         * Delete all logs for an order (for GDPR compliance)
         */
        @Transactional
        public void deleteOrderLogs(Long orderId) {
                log.warn("Deleting all activity logs for order {}", orderId);
                activityLogRepository.deleteByOrderId(orderId);
        }

        // ==================== Helper Methods for Common Activities
        // ====================

        /**
         * Log order placement
         */
        public void logOrderPlaced(Long orderId, String customerName) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.ORDER_PLACED,
                                "Order Placed",
                                String.format("Order placed by %s", customerName),
                                "CUSTOMER",
                                null,
                                customerName,
                                null,
                                null,
                                null);
        }

        public void logPaymentCompleted(Long orderId, String paymentMethod, Double amount) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.PAYMENT_COMPLETED,
                                "Payment Completed",
                                String.format("Payment of $%.2f completed via %s", amount, paymentMethod),
                                "SYSTEM",
                                null,
                                "Payment System",
                                null,
                                null,
                                Map.of("paymentMethod", paymentMethod, "amount", amount));
        }

        public void logAddedToDeliveryGroup(
                        Long orderId,
                        String deliveryGroupName,
                        String deliveryAgentName,
                        String deliveryAgentPhone,
                        Long deliveryGroupId) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.ADDED_TO_DELIVERY_GROUP,
                                "Added to Delivery Group",
                                String.format("Added to delivery group '%s' assigned to %s (%s)",
                                                deliveryGroupName, deliveryAgentName, deliveryAgentPhone),
                                "SYSTEM",
                                null,
                                "Delivery System",
                                String.valueOf(deliveryGroupId),
                                "DELIVERY_GROUP",
                                Map.of(
                                                "deliveryGroupName", deliveryGroupName,
                                                "agentName", deliveryAgentName,
                                                "agentPhone", deliveryAgentPhone));
        }

        /**
         * Log delivery started
         */
        public void logDeliveryStarted(Long orderId, String deliveryGroupName, String deliveryAgentName,
                        String agentId) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.DELIVERY_STARTED,
                                "Delivery Started",
                                String.format("Delivery agent %s started delivery for group '%s'", deliveryAgentName,
                                                deliveryGroupName),
                                "DELIVERY_AGENT",
                                agentId,
                                deliveryAgentName,
                                deliveryGroupName,
                                "DELIVERY_GROUP",
                                Map.of(
                                                "deliveryGroupName", deliveryGroupName,
                                                "agentName", deliveryAgentName));
        }

        /**
         * Log delivery note added for a specific order
         */
        public void logDeliveryNoteAdded(
                        Long orderId,
                        String note,
                        String deliveryAgentName,
                        String agentId,
                        Long deliveryNoteId,
                        String noteCategory) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("note", note);
                metadata.put("noteId", deliveryNoteId);
                if (noteCategory != null) {
                        metadata.put("category", noteCategory);
                }

                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.DELIVERY_NOTE_ADDED,
                                "Delivery Note Added",
                                String.format("Delivery note added by %s: %s", deliveryAgentName, note),
                                "DELIVERY_AGENT",
                                agentId,
                                deliveryAgentName,
                                String.valueOf(deliveryNoteId),
                                "DELIVERY_NOTE",
                                metadata);
        }

        /**
         * Log delivery note added for all orders in a delivery group
         */
        public void logGroupDeliveryNoteAdded(
                        List<Long> orderIds,
                        String note,
                        String deliveryAgentName,
                        String agentId,
                        Long deliveryNoteId,
                        String noteCategory,
                        String deliveryGroupName) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("note", note);
                metadata.put("noteId", deliveryNoteId);
                metadata.put("deliveryGroupName", deliveryGroupName);
                if (noteCategory != null) {
                        metadata.put("category", noteCategory);
                }

                for (Long orderId : orderIds) {
                        logActivity(
                                        orderId,
                                        OrderActivityLog.ActivityType.DELIVERY_NOTE_ADDED,
                                        "Group Delivery Note Added",
                                        String.format("Group note added by %s for delivery group '%s': %s",
                                                        deliveryAgentName, deliveryGroupName, note),
                                        "DELIVERY_AGENT",
                                        agentId,
                                        deliveryAgentName,
                                        String.valueOf(deliveryNoteId),
                                        "DELIVERY_NOTE",
                                        metadata);
                }
        }

        /**
         * Log successful delivery
         */
        public void logDeliveryCompleted(Long orderId, String deliveryAgentName) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.DELIVERY_COMPLETED,
                                "Delivery Completed",
                                String.format("Order successfully delivered by %s. Scan successful.",
                                                deliveryAgentName),
                                "DELIVERY_AGENT",
                                null,
                                deliveryAgentName,
                                null,
                                null,
                                null);
        }

        /**
         * Log return request
         */
        public void logReturnRequested(
                        Long orderId,
                        String customerName,
                        String reason,
                        Long returnRequestId) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.RETURN_REQUESTED,
                                "Return Requested",
                                String.format("Return requested by %s. Reason: %s", customerName, reason),
                                "CUSTOMER",
                                null,
                                customerName,
                                String.valueOf(returnRequestId),
                                "RETURN_REQUEST",
                                Map.of("reason", reason));
        }

        /**
         * Log return approval
         */
        public void logReturnApproved(Long orderId, String adminName, Long returnRequestId) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.RETURN_APPROVED,
                                "Return Approved",
                                String.format("Return request approved by %s", adminName),
                                "ADMIN",
                                null,
                                adminName,
                                String.valueOf(returnRequestId),
                                "RETURN_REQUEST",
                                null);
        }

        /**
         * Log return denial
         */
        public void logReturnDenied(
                        Long orderId,
                        String adminName,
                        String reason,
                        Long returnRequestId) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.RETURN_DENIED,
                                "Return Denied",
                                String.format("Return request denied by %s. Reason: %s", adminName, reason),
                                "ADMIN",
                                null,
                                adminName,
                                String.valueOf(returnRequestId),
                                "RETURN_REQUEST",
                                Map.of("denialReason", reason));
        }

        /**
         * Log appeal submission
         */
        public void logAppealSubmitted(
                        Long orderId,
                        String customerName,
                        String reason,
                        Long appealId,
                        Long returnRequestId) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.APPEAL_SUBMITTED,
                                "Appeal Submitted",
                                String.format("Appeal submitted by %s for denied return. Reason: %s", customerName,
                                                reason),
                                "CUSTOMER",
                                null,
                                customerName,
                                String.valueOf(appealId),
                                "APPEAL",
                                Map.of("reason", reason, "returnRequestId", returnRequestId));
        }

        /**
         * Log appeal approval
         */
        public void logAppealApproved(Long orderId, String adminName, Long appealId) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.APPEAL_APPROVED,
                                "Appeal Approved",
                                String.format("Appeal approved by %s", adminName),
                                "ADMIN",
                                null,
                                adminName,
                                String.valueOf(appealId),
                                "APPEAL",
                                null);
        }

        /**
         * Log appeal denial
         */
        public void logAppealDenied(Long orderId, String adminName, String reason, Long appealId) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.APPEAL_DENIED,
                                "Appeal Denied",
                                String.format("Appeal denied by %s. Reason: %s", adminName, reason),
                                "ADMIN",
                                null,
                                adminName,
                                String.valueOf(appealId),
                                "APPEAL",
                                Map.of("denialReason", reason));
        }

        /**
         * Log refund completion
         */
        public void logRefundCompleted(Long orderId, Double amount, String refundMethod) {
                logActivity(
                                orderId,
                                OrderActivityLog.ActivityType.REFUND_COMPLETED,
                                "Refund Completed",
                                String.format("Refund of $%.2f completed via %s", amount, refundMethod),
                                "SYSTEM",
                                null,
                                "Refund System",
                                null,
                                null,
                                Map.of("amount", amount, "method", refundMethod));
        }
}
