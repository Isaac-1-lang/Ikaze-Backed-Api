# Order Activity Log Integration Guide

## Overview

The Order Activity Log system provides a comprehensive timeline of all events that happen to an order. This guide shows how to integrate activity logging into existing services.

## Architecture

### Entity: `OrderActivityLog`
- Stores all order events in a single table
- Indexed for fast queries by orderId, activityType, and timestamp
- Supports metadata as JSON for flexible data storage

### Service: `OrderActivityLogService`
- Provides methods to log activities
- Retrieves timeline data
- Helper methods for common activities

### Controller: `OrderActivityLogController`
- REST API endpoints to fetch order timeline
- Supports date range filtering
- Returns chronological activity list

---

## Integration Steps

### Step 1: Inject OrderActivityLogService

Add the service to any existing service that handles order events:

```java
@Service
@RequiredArgsConstructor
public class YourExistingService {
    
    private final OrderActivityLogService activityLogService;
    // ... other dependencies
}
```

### Step 2: Log Activities

Call the appropriate logging method when events occur:

```java
// Simple logging
activityLogService.logActivity(
    orderId,
    OrderActivityLog.ActivityType.ORDER_PLACED,
    "Order Placed",
    "Order placed by customer"
);

// With actor information
activityLogService.logActivityWithActor(
    orderId,
    OrderActivityLog.ActivityType.RETURN_APPROVED,
    "Return Approved",
    "Return request has been approved",
    "ADMIN",
    adminId.toString(),
    adminName
);

// With full details and metadata
activityLogService.logActivity(
    orderId,
    OrderActivityLog.ActivityType.ADDED_TO_DELIVERY_GROUP,
    "Added to Delivery Group",
    "Added to delivery group 'Nyabihu deliveries'",
    "SYSTEM",
    null,
    "Delivery System",
    deliveryGroupId.toString(),
    "DELIVERY_GROUP",
    Map.of("groupName", "Nyabihu deliveries", "agentName", "John Doe")
);
```

---

## Integration Examples

### 1. Order Service (Order Placement)

**Location:** `OrderService.java` or `OrderServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderActivityLogService activityLogService;
    private final OrderRepository orderRepository;
    
    @Transactional
    public Order createOrder(OrderDTO orderDTO) {
        // Create order
        Order order = new Order();
        // ... set order properties
        Order savedOrder = orderRepository.save(order);
        
        // LOG ACTIVITY: Order Placed
        String customerName = order.getUser() != null 
            ? order.getUser().getFirstName() + " " + order.getUser().getLastName()
            : order.getOrderCustomerInfo().getFullName();
            
        activityLogService.logOrderPlaced(savedOrder.getOrderId(), customerName);
        
        return savedOrder;
    }
    
    @Transactional
    public void confirmPayment(Long orderId, PaymentDetails payment) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Update order status
        order.setOrderStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);
        
        // LOG ACTIVITY: Payment Completed
        activityLogService.logPaymentCompleted(
            orderId,
            payment.getPaymentMethod(),
            payment.getAmount()
        );
    }
}
```

### 2. ReadyForDeliveryGroupService (Delivery Group Assignment)

**Location:** `ReadyForDeliveryGroupServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
public class ReadyForDeliveryGroupServiceImpl implements ReadyForDeliveryGroupService {
    
    private final OrderActivityLogService activityLogService;
    private final ReadyForDeliveryGroupRepository groupRepository;
    
    @Transactional
    public void addOrderToDeliveryGroup(Long orderId, Long deliveryGroupId) {
        ReadyForDeliveryGroup group = groupRepository.findById(deliveryGroupId)
            .orElseThrow(() -> new RuntimeException("Delivery group not found"));
        
        // Add order to group
        // ... existing logic
        
        // LOG ACTIVITY: Added to Delivery Group
        String agentName = group.getDeliveryAgent() != null 
            ? group.getDeliveryAgent().getFirstName() + " " + group.getDeliveryAgent().getLastName()
            : "Unassigned";
        String agentPhone = group.getDeliveryAgent() != null 
            ? group.getDeliveryAgent().getPhoneNumber()
            : "N/A";
            
        activityLogService.logAddedToDeliveryGroup(
            orderId,
            group.getGroupName(),
            agentName,
            agentPhone,
            deliveryGroupId
        );
    }
    
    @Transactional
    public void startDelivery(Long deliveryGroupId, UUID deliveryAgentId) {
        ReadyForDeliveryGroup group = groupRepository.findById(deliveryGroupId)
            .orElseThrow(() -> new RuntimeException("Delivery group not found"));
        
        // Start delivery
        group.setStatus(ReadyForDeliveryGroup.GroupStatus.IN_TRANSIT);
        groupRepository.save(group);
        
        // LOG ACTIVITY for each order in the group
        String agentName = group.getDeliveryAgent().getFirstName() + " " + 
                          group.getDeliveryAgent().getLastName();
        
        for (Order order : group.getOrders()) {
            activityLogService.logDeliveryStarted(
                order.getOrderId(),
                agentName,
                deliveryGroupId
            );
        }
    }
}
```

### 3. OrderDeliveryNoteService (Delivery Notes)

**Location:** Create or update service handling delivery notes

```java
@Service
@RequiredArgsConstructor
public class OrderDeliveryNoteService {
    
    private final OrderActivityLogService activityLogService;
    private final OrderDeliveryNoteRepository noteRepository;
    
    @Transactional
    public OrderDeliveryNote addDeliveryNote(Long orderId, String note, UUID agentId, String agentName) {
        OrderDeliveryNote deliveryNote = new OrderDeliveryNote();
        deliveryNote.setOrderId(orderId);
        deliveryNote.setNote(note);
        deliveryNote.setCreatedBy(agentId);
        deliveryNote.setCreatedAt(LocalDateTime.now());
        
        OrderDeliveryNote saved = noteRepository.save(deliveryNote);
        
        // LOG ACTIVITY: Delivery Note Added
        activityLogService.logDeliveryNoteAdded(
            orderId,
            note,
            agentName,
            saved.getId()
        );
        
        return saved;
    }
}
```

### 4. DeliveryService (Successful Delivery)

**Location:** Service handling delivery completion

```java
@Service
@RequiredArgsConstructor
public class DeliveryService {
    
    private final OrderActivityLogService activityLogService;
    private final OrderRepository orderRepository;
    
    @Transactional
    public void completeDelivery(Long orderId, UUID deliveryAgentId, String agentName) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Update order status
        order.setOrderStatus(Order.OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        orderRepository.save(order);
        
        // LOG ACTIVITY: Delivery Completed
        activityLogService.logDeliveryCompleted(orderId, agentName);
    }
}
```

### 5. ReturnService (Return Requests)

**Location:** `ReturnService.java` (already exists)

```java
@Service
@RequiredArgsConstructor
public class ReturnService {
    
    private final OrderActivityLogService activityLogService;
    private final ReturnRequestRepository returnRequestRepository;
    
    @Transactional
    public ReturnRequestDTO submitReturnRequest(SubmitReturnRequestDTO submitDTO, MultipartFile[] mediaFiles) {
        // ... existing return request creation logic
        
        ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);
        
        // LOG ACTIVITY: Return Requested
        Order order = savedRequest.getOrder();
        String customerName = order.getUser() != null 
            ? order.getUser().getFirstName() + " " + order.getUser().getLastName()
            : order.getOrderCustomerInfo().getFullName();
            
        activityLogService.logReturnRequested(
            order.getOrderId(),
            customerName,
            submitDTO.getReason(),
            savedRequest.getId()
        );
        
        return convertToDTO(savedRequest);
    }
    
    @Transactional
    public ReturnRequestDTO reviewReturnRequest(ReturnDecisionDTO decisionDTO) {
        ReturnRequest returnRequest = returnRequestRepository.findById(decisionDTO.getReturnRequestId())
            .orElseThrow(() -> new RuntimeException("Return request not found"));
        
        if ("APPROVED".equals(decisionDTO.getDecision())) {
            returnRequest.approve(decisionDTO.getDecisionNotes());
            
            // LOG ACTIVITY: Return Approved
            activityLogService.logReturnApproved(
                returnRequest.getOrderId(),
                "Admin", // Get actual admin name from security context
                returnRequest.getId()
            );
        } else if ("DENIED".equals(decisionDTO.getDecision())) {
            returnRequest.deny(decisionDTO.getDecisionNotes());
            
            // LOG ACTIVITY: Return Denied
            activityLogService.logReturnDenied(
                returnRequest.getOrderId(),
                "Admin", // Get actual admin name from security context
                decisionDTO.getDecisionNotes(),
                returnRequest.getId()
            );
        }
        
        return convertToDTO(returnRequestRepository.save(returnRequest));
    }
}
```

### 6. AppealService (Appeals)

**Location:** `AppealService.java` (already exists)

```java
@Service
@RequiredArgsConstructor
public class AppealService {
    
    private final OrderActivityLogService activityLogService;
    private final ReturnAppealRepository returnAppealRepository;
    private final ReturnRequestRepository returnRequestRepository;
    
    @Transactional
    public ReturnAppealDTO submitAppeal(SubmitAppealRequestDTO submitDTO, MultipartFile[] mediaFiles) {
        // ... existing appeal creation logic
        
        ReturnAppeal savedAppeal = returnAppealRepository.save(appeal);
        
        // LOG ACTIVITY: Appeal Submitted
        ReturnRequest returnRequest = returnRequestRepository.findById(submitDTO.getReturnRequestId())
            .orElseThrow(() -> new RuntimeException("Return request not found"));
        
        Order order = returnRequest.getOrder();
        String customerName = order.getUser() != null 
            ? order.getUser().getFirstName() + " " + order.getUser().getLastName()
            : order.getOrderCustomerInfo().getFullName();
            
        activityLogService.logAppealSubmitted(
            order.getOrderId(),
            customerName,
            submitDTO.getReason(),
            savedAppeal.getId(),
            returnRequest.getId()
        );
        
        return convertToDTO(savedAppeal);
    }
    
    @Transactional
    public ReturnAppealDTO reviewAppeal(AppealDecisionDTO decisionDTO) {
        ReturnAppeal appeal = returnAppealRepository.findByIdWithReturnRequest(decisionDTO.getAppealId())
            .orElseThrow(() -> new RuntimeException("Appeal not found"));
        
        Long orderId = appeal.getReturnRequest().getOrderId();
        
        if ("APPROVED".equals(decisionDTO.getDecision())) {
            appeal.approve(decisionDTO.getDecisionNotes());
            
            // LOG ACTIVITY: Appeal Approved
            activityLogService.logAppealApproved(
                orderId,
                "Admin", // Get actual admin name
                appeal.getId()
            );
        } else if ("DENIED".equals(decisionDTO.getDecision())) {
            appeal.deny(decisionDTO.getDecisionNotes());
            
            // LOG ACTIVITY: Appeal Denied
            activityLogService.logAppealDenied(
                orderId,
                "Admin", // Get actual admin name
                decisionDTO.getDecisionNotes(),
                appeal.getId()
            );
        }
        
        return convertToDTO(returnAppealRepository.save(appeal));
    }
}
```

### 7. RefundService (Refunds)

**Location:** `RefundService.java` (already exists)

```java
@Service
@RequiredArgsConstructor
public class RefundService {
    
    private final OrderActivityLogService activityLogService;
    
    @Transactional
    public void processRefund(Long orderId, Double amount, String refundMethod) {
        // ... existing refund processing logic
        
        // LOG ACTIVITY: Refund Completed
        activityLogService.logRefundCompleted(orderId, amount, refundMethod);
    }
}
```

---

## API Endpoints

### Get Complete Order Timeline

```http
GET /api/v1/orders/{orderId}/activity-logs
Authorization: Bearer {token}
```

**Response:**
```json
{
  "orderId": 12345,
  "totalActivities": 15,
  "activities": [
    {
      "id": 1,
      "orderId": 12345,
      "activityType": "ORDER_PLACED",
      "title": "Order Placed",
      "description": "Order placed by John Doe",
      "timestamp": "2025-10-25T23:00:00",
      "actorType": "CUSTOMER",
      "actorName": "John Doe",
      "referenceId": null,
      "referenceType": null,
      "metadata": null
    },
    {
      "id": 2,
      "orderId": 12345,
      "activityType": "PAYMENT_COMPLETED",
      "title": "Payment Completed",
      "description": "Payment of $150.00 completed via Credit Card",
      "timestamp": "2025-10-25T23:05:00",
      "actorType": "SYSTEM",
      "actorName": "Payment System",
      "metadata": "{\"paymentMethod\":\"Credit Card\",\"amount\":150.0}"
    },
    {
      "id": 3,
      "orderId": 12345,
      "activityType": "ADDED_TO_DELIVERY_GROUP",
      "title": "Added to Delivery Group",
      "description": "Added to delivery group 'Nyabihu deliveries' assigned to John Agent (0788123456)",
      "timestamp": "2025-10-26T12:00:00",
      "actorType": "SYSTEM",
      "actorName": "Delivery System",
      "referenceId": "45",
      "referenceType": "DELIVERY_GROUP",
      "metadata": "{\"deliveryGroupName\":\"Nyabihu deliveries\",\"agentName\":\"John Agent\",\"agentPhone\":\"0788123456\"}"
    },
    {
      "id": 4,
      "orderId": 12345,
      "activityType": "DELIVERY_STARTED",
      "title": "Delivery Started",
      "description": "Delivery agent John Agent started delivery",
      "timestamp": "2025-10-26T13:00:00",
      "actorType": "DELIVERY_AGENT",
      "actorName": "John Agent",
      "referenceId": "45",
      "referenceType": "DELIVERY_GROUP"
    },
    {
      "id": 5,
      "orderId": 12345,
      "activityType": "DELIVERY_NOTE_ADDED",
      "title": "Delivery Note Added",
      "description": "Delivery note added by John Agent: Customer requested delivery at back door",
      "timestamp": "2025-10-26T14:00:00",
      "actorType": "DELIVERY_AGENT",
      "actorName": "John Agent",
      "referenceId": "78",
      "referenceType": "DELIVERY_NOTE",
      "metadata": "{\"note\":\"Customer requested delivery at back door\"}"
    },
    {
      "id": 6,
      "orderId": 12345,
      "activityType": "DELIVERY_COMPLETED",
      "title": "Delivery Completed",
      "description": "Order successfully delivered by John Agent. Scan successful.",
      "timestamp": "2025-10-26T15:00:00",
      "actorType": "DELIVERY_AGENT",
      "actorName": "John Agent"
    },
    {
      "id": 7,
      "orderId": 12345,
      "activityType": "RETURN_REQUESTED",
      "title": "Return Requested",
      "description": "Return requested by John Doe. Reason: Product defective",
      "timestamp": "2025-10-27T06:00:00",
      "actorType": "CUSTOMER",
      "actorName": "John Doe",
      "referenceId": "89",
      "referenceType": "RETURN_REQUEST",
      "metadata": "{\"reason\":\"Product defective\"}"
    },
    {
      "id": 8,
      "orderId": 12345,
      "activityType": "RETURN_DENIED",
      "title": "Return Denied",
      "description": "Return request denied by Admin Smith. Reason: Outside return window",
      "timestamp": "2025-10-27T06:30:00",
      "actorType": "ADMIN",
      "actorName": "Admin Smith",
      "referenceId": "89",
      "referenceType": "RETURN_REQUEST",
      "metadata": "{\"denialReason\":\"Outside return window\"}"
    },
    {
      "id": 9,
      "orderId": 12345,
      "activityType": "APPEAL_SUBMITTED",
      "title": "Appeal Submitted",
      "description": "Appeal submitted by John Doe for denied return. Reason: Product was defective on arrival",
      "timestamp": "2025-10-27T07:00:00",
      "actorType": "CUSTOMER",
      "actorName": "John Doe",
      "referenceId": "23",
      "referenceType": "APPEAL",
      "metadata": "{\"reason\":\"Product was defective on arrival\",\"returnRequestId\":89}"
    },
    {
      "id": 10,
      "orderId": 12345,
      "activityType": "APPEAL_APPROVED",
      "title": "Appeal Approved",
      "description": "Appeal approved by Admin Johnson",
      "timestamp": "2025-10-27T08:00:00",
      "actorType": "ADMIN",
      "actorName": "Admin Johnson",
      "referenceId": "23",
      "referenceType": "APPEAL"
    }
  ]
}
```

### Get Timeline by Date Range

```http
GET /api/v1/orders/{orderId}/activity-logs/range?startDate=2025-10-26T00:00:00&endDate=2025-10-27T23:59:59
Authorization: Bearer {token}
```

### Get Recent Activities

```http
GET /api/v1/orders/{orderId}/activity-logs/recent?limit=5
Authorization: Bearer {token}
```

### Get Activity Count

```http
GET /api/v1/orders/{orderId}/activity-logs/count
Authorization: Bearer {token}
```

---

## Activity Types & Colors (Frontend Reference)

Suggested color coding for frontend timeline visualization:

| Activity Type | Category | Suggested Color |
|--------------|----------|----------------|
| ORDER_PLACED, PAYMENT_COMPLETED | Order Creation | `#10B981` (Green) |
| ORDER_CONFIRMED, ORDER_PROCESSING | Processing | `#3B82F6` (Blue) |
| ADDED_TO_DELIVERY_GROUP, DELIVERY_AGENT_ASSIGNED | Delivery Setup | `#8B5CF6` (Purple) |
| DELIVERY_STARTED, OUT_FOR_DELIVERY | In Transit | `#F59E0B` (Amber) |
| DELIVERY_NOTE_ADDED | Notes | `#6B7280` (Gray) |
| DELIVERY_COMPLETED | Success | `#059669` (Emerald) |
| RETURN_REQUESTED, APPEAL_SUBMITTED | Customer Action | `#EF4444` (Red) |
| RETURN_APPROVED, APPEAL_APPROVED | Approval | `#10B981` (Green) |
| RETURN_DENIED, APPEAL_DENIED | Denial | `#DC2626` (Dark Red) |
| REFUND_COMPLETED | Financial | `#14B8A6` (Teal) |
| ORDER_CANCELLED, DELIVERY_FAILED | Failure | `#991B1B` (Crimson) |

---

## Database Migration

Create the table using this SQL:

```sql
CREATE TABLE order_activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    timestamp DATETIME NOT NULL,
    actor_type VARCHAR(50),
    actor_id VARCHAR(100),
    actor_name VARCHAR(200),
    metadata TEXT,
    reference_id VARCHAR(100),
    reference_type VARCHAR(50),
    created_at DATETIME NOT NULL,
    
    INDEX idx_order_id (order_id),
    INDEX idx_activity_type (activity_type),
    INDEX idx_timestamp (timestamp),
    
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);
```

---

## Benefits

### 1. **Performance**
- ✅ Single query to get complete timeline
- ✅ Indexed for fast lookups
- ✅ No need to join multiple tables

### 2. **Flexibility**
- ✅ Easy to add new activity types
- ✅ Metadata field for custom data
- ✅ Reference fields link to related entities

### 3. **Audit Trail**
- ✅ Complete historical record
- ✅ Actor information tracked
- ✅ Immutable log entries

### 4. **Scalability**
- ✅ Efficient storage
- ✅ Can be archived/partitioned by date
- ✅ Supports millions of records

---

## Next Steps

1. ✅ Run database migration to create `order_activity_logs` table
2. ✅ Inject `OrderActivityLogService` into existing services
3. ✅ Add logging calls at appropriate points
4. ✅ Test API endpoints
5. ⏳ Build frontend timeline visualization (next phase)

---

## Summary

The Order Activity Log system provides:
- **Centralized logging** of all order events
- **Fast retrieval** with single query
- **Flexible metadata** storage
- **Complete audit trail** for compliance
- **Easy integration** with existing services

This approach is superior to querying multiple repositories because it's faster, more maintainable, and provides a complete chronological view of the order lifecycle.
