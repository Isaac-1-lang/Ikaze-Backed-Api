# 🚀 Quick Start Testing Guide - Order System Integration

## ⚡ Get Started in 5 Minutes

### 1. 🏃‍♂️ Start the Backend
```bash
cd Backend-service
mvn spring-boot:run
```

### 2. 🌐 Access Swagger UI
Open your browser and go to:
```
http://localhost:8095/swagger-ui.html
```

### 3. 🔑 Get Authentication Token
Use the login endpoint to get a JWT token:
```bash
curl -X POST "http://localhost:8095/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "password123"
  }'
```

### 4. 🧪 Test the Integration

#### Test Order Creation
```bash
curl -X POST "http://localhost:8095/api/v1/orders" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productVariantId": 1,
        "quantity": 2
      }
    ],
    "address": {
      "street": "123 Test St",
      "city": "Test City",
      "state": "Test State",
      "zipCode": "12345",
      "country": "Test Country",
      "phone": "123-456-7890"
    },
    "stripePaymentIntentId": "pi_test_123",
    "stripeSessionId": "cs_test_123"
  }'
```

#### Test Analytics
```bash
curl -X GET "http://localhost:8095/api/v1/admin/analytics/dashboard" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Test Customer Orders
```bash
curl -X GET "http://localhost:8095/api/v1/customer/orders" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 📱 Postman Collection

1. Import the `ORDER_SYSTEM_POSTMAN_COLLECTION.json` file into Postman
2. Set the `jwt_token` variable with your authentication token
3. Test all endpoints systematically

## 🧪 Run Integration Tests

```bash
# Run all tests
mvn test

# Run only order system tests
mvn test -Dtest=OrderSystemIntegrationTest

# Run with detailed output
mvn test -Dtest=OrderSystemIntegrationTest -X
```

## 🔍 Test Scenarios

### Customer Flow
1. ✅ Create order
2. ✅ View order details
3. ✅ Cancel order (if eligible)

### Admin Flow
1. ✅ View all orders
2. ✅ Update order status
3. ✅ Update tracking information
4. ✅ View analytics

### Delivery Flow
1. ✅ View delivery orders
2. ✅ Update delivery status
3. ✅ Update tracking

### Analytics Flow
1. ✅ Dashboard metrics
2. ✅ Revenue analysis
3. ✅ Product performance
4. ✅ Customer insights

## 🚨 Common Issues & Solutions

### Issue: "Unable to locate Attribute with the given name [status]"
**Solution**: The field is called `orderStatus`, not `status`. Use `findByOrderStatusIn` instead of `findByStatusIn`.

### Issue: "Cannot find symbol variable log"
**Solution**: Ensure `@Slf4j` annotation is present on the service class.

### Issue: "Incompatible types: UUID vs Long"
**Solution**: Product IDs are UUIDs, not Longs. Update your Map types accordingly.

### Issue: "Method not found: getId()"
**Solution**: Use `getProductId()` for Product entities and `getProductName()` for product names.

## 📊 Expected Results

### Order Creation
- Returns 201 status with order details
- Order status should be "PENDING"
- Inventory should be reduced
- Order code should be auto-generated

### Analytics
- Dashboard should return total orders, revenue, and AOV
- Revenue calculations should only include "DELIVERED" orders
- Customer stats should show total customers and order distribution

### Order Management
- Status updates should follow the correct flow
- Cancelled orders should restore inventory
- Tracking updates should be persisted

## 🎯 Next Steps

1. **Test with Real Data**: Create actual products and users
2. **Frontend Integration**: Connect your React frontend to these APIs
3. **Email Notifications**: Implement actual email sending logic
4. **Payment Integration**: Test with real Stripe test keys
5. **Performance Testing**: Load test with multiple concurrent users

## 📞 Support

If you encounter issues:
1. Check the application logs for detailed error messages
2. Verify your database connection and schema
3. Ensure all required dependencies are present
4. Check that JWT tokens are valid and not expired

## 🎉 Success Indicators

You'll know the integration is working when:
- ✅ All endpoints return 200/201 status codes
- ✅ Order creation reduces inventory
- ✅ Analytics return meaningful data
- ✅ Role-based access control works
- ✅ Order status transitions are smooth
- ✅ Integration tests pass

---

**Happy Testing! 🚀**
