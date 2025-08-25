# 🚀 Order System API Integration Guide

## 📋 Overview

This document provides a comprehensive guide to the integrated order management system, including all endpoints, authentication, and usage examples.

## 🔐 Authentication & Authorization

All endpoints require JWT authentication. Include the token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

### Role-Based Access Control
- **CUSTOMER**: Can view and manage their own orders
- **ADMIN/EMPLOYEE**: Can view and manage all orders
- **DELIVERY_AGENT**: Can view and update delivery-related order information

## 🛒 Order Creation & Management

### 1. Create Order
**POST** `/api/v1/orders`

**Access**: CUSTOMER, ADMIN, EMPLOYEE

**Request Body**:
```json
{
  "items": [
    {
      "productVariantId": 123,
      "quantity": 2
    }
  ],
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA",
    "phone": "+1-555-0123"
  },
  "stripePaymentIntentId": "pi_1234567890",
  "stripeSessionId": "cs_1234567890"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderCode": "ORD-2024-001",
    "orderStatus": "PENDING",
    "totalAmount": 199.98,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

## 👤 Customer Order Endpoints

### 2. Get Customer Orders
**GET** `/api/v1/customer/orders`

**Access**: CUSTOMER

**Query Parameters**:
- `status` (optional): Filter by order status
- `page` (optional): Page number for pagination
- `size` (optional): Page size

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "orderId": 1,
      "orderCode": "ORD-2024-001",
      "orderStatus": "PENDING",
      "totalAmount": 199.98,
      "createdAt": "2024-01-15T10:30:00",
      "items": [...],
      "address": {...}
    }
  ]
}
```

### 3. Get Customer Order by ID
**GET** `/api/v1/customer/orders/{orderId}`

**Access**: CUSTOMER

### 4. Cancel Order
**PUT** `/api/v1/customer/orders/{orderId}/cancel`

**Access**: CUSTOMER

**Note**: Only orders with PENDING or PROCESSING status can be cancelled.

## 👨‍💼 Admin Order Endpoints

### 5. Get All Orders
**GET** `/api/v1/admin/orders`

**Access**: ADMIN, EMPLOYEE

**Query Parameters**:
- `status` (optional): Filter by order status
- `startDate` (optional): Filter orders from this date
- `endDate` (optional): Filter orders until this date
- `page` (optional): Page number
- `size` (optional): Page size

### 6. Get Order by ID
**GET** `/api/v1/admin/orders/{orderId}`

**Access**: ADMIN, EMPLOYEE

### 7. Update Order Status
**PUT** `/api/v1/admin/orders/{orderId}/status`

**Access**: ADMIN, EMPLOYEE

**Request Body**:
```json
{
  "status": "PROCESSING"
}
```

**Available Statuses**:
- `PENDING` → `PROCESSING`
- `PROCESSING` → `OUT_FOR_DELIVERY`
- `OUT_FOR_DELIVERY` → `DELIVERED`
- `CANCELLED` (from PENDING/PROCESSING)

### 8. Update Order Tracking
**PUT** `/api/v1/admin/orders/{orderId}/tracking`

**Access**: ADMIN, EMPLOYEE

**Request Body**:
```json
{
  "trackingNumber": "TRACK123456",
  "carrier": "FedEx"
}
```

## 🚚 Delivery Order Endpoints

### 9. Get Delivery Orders
**GET** `/api/v1/delivery/orders`

**Access**: DELIVERY_AGENT

**Query Parameters**:
- `status` (optional): Filter by order status
- `page` (optional): Page number
- `size` (optional): Page size

### 10. Get Delivery Orders by Status
**GET** `/api/v1/delivery/orders/status/{status}`

**Access**: DELIVERY_AGENT

### 11. Update Order Status (Delivery)
**PUT** `/api/v1/delivery/orders/{orderId}/status`

**Access**: DELIVERY_AGENT

**Request Body**:
```json
{
  "status": "OUT_FOR_DELIVERY"
}
```

### 12. Update Order Tracking (Delivery)
**PUT** `/api/v1/delivery/orders/{orderId}/tracking`

**Access**: DELIVERY_AGENT

## 📊 Analytics Endpoints

### 13. Dashboard Analytics
**GET** `/api/v1/admin/analytics/dashboard`

**Access**: ADMIN, EMPLOYEE

**Response**:
```json
{
  "success": true,
  "data": {
    "totalOrders": 150,
    "totalRevenue": 15750.00,
    "averageOrderValue": 105.00,
    "ordersByStatus": {
      "PENDING": 25,
      "PROCESSING": 30,
      "OUT_FOR_DELIVERY": 15,
      "DELIVERED": 80
    }
  }
}
```

### 14. Revenue Analytics
**GET** `/api/v1/admin/analytics/revenue`

**Access**: ADMIN, EMPLOYEE

**Query Parameters**:
- `startDate` (optional): Start date for revenue calculation
- `endDate` (optional): End date for revenue calculation

### 15. Product Analytics
**GET** `/api/v1/admin/analytics/products`

**Access**: ADMIN, EMPLOYEE

**Query Parameters**:
- `limit` (optional): Number of top products to return (default: 10)

### 16. Customer Analytics
**GET** `/api/v1/admin/analytics/customers`

**Access**: ADMIN, EMPLOYEE

### 17. Delivery Performance
**GET** `/api/v1/admin/analytics/delivery`

**Access**: ADMIN, EMPLOYEE

### 18. Revenue Trends
**GET** `/api/v1/admin/analytics/trends`

**Access**: ADMIN, EMPLOYEE

**Query Parameters**:
- `days` (optional): Number of days for trend analysis (default: 30)

## 🔍 Order Search & Filtering

### 19. Search Orders by Order Code
**GET** `/api/v1/admin/orders/search`

**Access**: ADMIN, EMPLOYEE

**Query Parameters**:
- `orderCode`: Order code to search for

### 20. Get Orders by Date Range
**GET** `/api/v1/admin/orders`

**Access**: ADMIN, EMPLOYEE

**Query Parameters**:
- `startDate`: Start date (ISO format)
- `endDate`: End date (ISO format)

## 📈 Order Lifecycle

### Typical Order Flow:
1. **PENDING** - Order created, payment pending
2. **PROCESSING** - Payment confirmed, order being prepared
3. **OUT_FOR_DELIVERY** - Order shipped, in transit
4. **DELIVERED** - Order successfully delivered

### Cancellation Flow:
- Orders can be cancelled from **PENDING** or **PROCESSING** status
- Cancelled orders restore product inventory
- Refunds are processed through Stripe

## 🔧 Error Handling

### Common Error Responses:

**400 Bad Request**:
```json
{
  "success": false,
  "message": "Invalid input data",
  "errors": ["Field 'quantity' must be greater than 0"]
}
```

**401 Unauthorized**:
```json
{
  "success": false,
  "message": "Authentication required"
}
```

**403 Forbidden**:
```json
{
  "success": false,
  "message": "Insufficient permissions"
}
```

**404 Not Found**:
```json
{
  "success": false,
  "message": "Order not found"
}
```

**500 Internal Server Error**:
```json
{
  "success": false,
  "message": "Internal server error"
}
```

## 🧪 Testing

### Integration Test
Run the comprehensive integration test:
```bash
mvn test -Dtest=OrderSystemIntegrationTest
```

### Test Coverage
The integration test covers:
- ✅ Order creation flow
- ✅ Customer order management
- ✅ Admin order management
- ✅ Delivery order management
- ✅ Analytics endpoints
- ✅ Order lifecycle transitions
- ✅ Error handling
- ✅ Authentication & authorization

## 🚀 Getting Started

### 1. Start the Application
```bash
mvn spring-boot:run
```

### 2. Access Swagger UI
```
http://localhost:8095/swagger-ui.html
```

### 3. Authenticate
Use the `/api/v1/auth/login` endpoint to get a JWT token.

### 4. Test Endpoints
Use the token in the Authorization header to test all endpoints.

## 📝 Notes

- All timestamps are in ISO 8601 format
- Monetary amounts are in BigDecimal with 2 decimal places
- Order codes are auto-generated with format: `ORD-YYYY-XXX`
- Product inventory is automatically managed during order creation/cancellation
- Email notifications are sent for major order status changes (TODO: implement actual email sending)

## 🔗 Related Documentation

- [Authentication Guide](../AUTH_INTEGRATION_TESTING_GUIDE.md)
- [Database Schema](../DATABASE_SCHEMA_FIX_GUIDE.md)
- [Role-Based Access Control](../ROLE_BASED_AUTHENTICATION_GUIDE.md)
- [Payment Integration](../PAYMENT_TROUBLESHOOTING_GUIDE.md)
