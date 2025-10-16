# System Reset Implementation Documentation

## Overview
This document describes the comprehensive system reset functionality implemented for the ShopSphere e-commerce platform. The system allows administrators to selectively delete various entities with full cascading relationship handling.

## Architecture Summary

### Components Created

1. **DTOs (Data Transfer Objects)**
   - `SystemResetRequest.java` - Request DTO with boolean flags for each deletable entity type
   - `SystemResetResponse.java` - Response DTO with statistics and error reporting

2. **Service Layer**
   - `SystemResetService.java` - Service interface defining all deletion operations
   - `SystemResetServiceImpl.java` - Implementation with multithreading support

3. **Controller Layer**
   - `SystemResetController.java` - REST API endpoints (admin-only access)

## Entity Count
Based on the entity analysis, the system has **48 entities** in total:
- Address, AdminInvitation, AppealMedia, Brand, Cart, CartItem, Category
- DeliveryAgentLocation, DeliveryAssignment, Discount, MoneyFlow, Order
- OrderAddress, OrderCustomerInfo, OrderDeliveryNote, OrderInfo, OrderItem
- OrderItemBatch, OrderTrackingToken, OrderTransaction, Product
- ProductAttributeType, ProductAttributeValue, ProductDetail, ProductImage
- ProductVariant, ProductVariantImage, ProductVideo, ReadyForDeliveryGroup
- ResetToken, ReturnAppeal, ReturnItem, ReturnMedia, ReturnRequest
- Review, RewardRange, RewardSystem, ShippingCost, Stock, StockBatch
- StockBatchLock, User, UserPoints, VariantAttributeValue, Warehouse
- WarehouseImage, Wishlist, WishlistProduct

## Deletable Entities (9 Categories)

### 1. Products
**Cascading Deletes:**
- Product variants
- Product images
- Product videos
- Stocks and stock batches
- Cart items
- Wishlist items
- Reviews
- Product details

**Endpoint:** `DELETE /api/v1/admin/system-reset/products`

### 2. Discounts
**Cascading Deletes:**
- Removes discount associations from products
- Removes discount associations from product variants

**Endpoint:** `DELETE /api/v1/admin/system-reset/discounts`

### 3. Orders
**Cascading Deletes:**
- Order items
- Order transactions
- Order addresses
- Order customer info
- Order info
- Order tracking tokens
- Order delivery notes
- Order item batches

**Endpoint:** `DELETE /api/v1/admin/system-reset/orders`

### 4. Reward Systems
**Cascading Deletes:**
- Reward ranges
- User points

**Endpoint:** `DELETE /api/v1/admin/system-reset/reward-systems`

### 5. Shipping Costs
**Cascading Deletes:** None (standalone entity)

**Endpoint:** `DELETE /api/v1/admin/system-reset/shipping-costs`

### 6. Money Flows
**Cascading Deletes:** None (standalone entity)

**Endpoint:** `DELETE /api/v1/admin/system-reset/money-flows`

### 7. Categories
**Cascading Deletes:**
- Removes category associations from products
- Handles parent-child category relationships

**Endpoint:** `DELETE /api/v1/admin/system-reset/categories`

### 8. Brands
**Cascading Deletes:**
- Removes brand associations from products

**Endpoint:** `DELETE /api/v1/admin/system-reset/brands`

### 9. Warehouses
**Cascading Deletes:**
- Stocks
- Stock batches
- Warehouse images

**Endpoint:** `DELETE /api/v1/admin/system-reset/warehouses`

## API Endpoints

### Main System Reset Endpoint
```
POST /api/v1/admin/system-reset/reset
```

**Request Body:**
```json
{
  "deleteProducts": true,
  "deleteDiscounts": false,
  "deleteOrders": true,
  "deleteRewardSystems": false,
  "deleteShippingCosts": false,
  "deleteMoneyFlows": false,
  "deleteCategories": false,
  "deleteBrands": false,
  "deleteWarehouses": false
}
```

**Response:**
```json
{
  "success": true,
  "message": "System reset completed. Total entities deleted: 1523",
  "timestamp": "2024-10-16T15:20:30",
  "stats": {
    "productsDeleted": 1200,
    "discountsDeleted": 0,
    "ordersDeleted": 323,
    "rewardSystemsDeleted": 0,
    "shippingCostsDeleted": 0,
    "moneyFlowsDeleted": 0,
    "categoriesDeleted": 0,
    "brandsDeleted": 0,
    "warehousesDeleted": 0,
    "totalDeleted": 1523,
    "executionTimeMs": 4567
  },
  "errors": []
}
```

### Individual Entity Deletion Endpoints
All endpoints require **ADMIN role** and use **DELETE** method:

1. `/api/v1/admin/system-reset/products`
2. `/api/v1/admin/system-reset/discounts`
3. `/api/v1/admin/system-reset/orders`
4. `/api/v1/admin/system-reset/reward-systems`
5. `/api/v1/admin/system-reset/shipping-costs`
6. `/api/v1/admin/system-reset/money-flows`
7. `/api/v1/admin/system-reset/categories`
8. `/api/v1/admin/system-reset/brands`
9. `/api/v1/admin/system-reset/warehouses`

## Key Features

### 1. Multithreading
- Uses `ExecutorService` with configurable thread pool
- Thread pool size: `min(9, available_processors)`
- Each deletion task runs in parallel for maximum efficiency
- Timeout protection: 5 minutes per task, 6 minutes total

### 2. Error Handling
- **Continue on Error:** If one entity type fails, others continue
- **Error Collection:** All errors are collected and reported
- **Detailed Logging:** Comprehensive logging at INFO and ERROR levels
- **Transaction Management:** Each deletion operation is transactional

### 3. Security
- **Admin-Only Access:** `@PreAuthorize("hasRole('ADMIN')")`
- **Bearer Token Authentication:** Required for all endpoints
- **Validation:** Request validation ensures at least one option is selected

### 4. Response Structure
- **Statistics:** Detailed count of deleted entities per type
- **Execution Time:** Performance metrics in milliseconds
- **Error Details:** Entity type, error message, and exception details
- **Success Flag:** Overall operation success indicator

## Usage Example

### Frontend Integration (Checkbox-based UI)

```typescript
// Example React/Next.js component
const SystemResetPanel = () => {
  const [selections, setSelections] = useState({
    deleteProducts: false,
    deleteDiscounts: false,
    deleteOrders: false,
    deleteRewardSystems: false,
    deleteShippingCosts: false,
    deleteMoneyFlows: false,
    deleteCategories: false,
    deleteBrands: false,
    deleteWarehouses: false,
  });

  const handleReset = async () => {
    const response = await fetch('/api/v1/admin/system-reset/reset', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(selections)
    });
    
    const result = await response.json();
    // Handle response...
  };

  return (
    <div>
      <h2>System Reset</h2>
      <label>
        <input type="checkbox" 
               checked={selections.deleteProducts}
               onChange={(e) => setSelections({...selections, deleteProducts: e.target.checked})} />
        Delete All Products
      </label>
      {/* More checkboxes... */}
      <button onClick={handleReset}>Reset Selected</button>
    </div>
  );
};
```

## Performance Considerations

### Optimization Strategies
1. **Batch Operations:** Uses JPA's `deleteAll()` for bulk operations
2. **Parallel Execution:** Multiple entity types deleted simultaneously
3. **Lazy Loading:** Entities loaded only when needed
4. **Transaction Boundaries:** Each entity type has its own transaction

### Expected Performance
- **Small Dataset** (< 1000 entities): 1-5 seconds
- **Medium Dataset** (1000-10000 entities): 5-30 seconds
- **Large Dataset** (> 10000 entities): 30-300 seconds

## Testing Recommendations

### Unit Tests
```java
@Test
void testDeleteAllProducts() {
    long count = systemResetService.deleteAllProducts();
    assertTrue(count >= 0);
    assertEquals(0, productRepository.count());
}
```

### Integration Tests
```java
@Test
void testSystemResetWithMultipleSelections() {
    SystemResetRequest request = new SystemResetRequest();
    request.setDeleteProducts(true);
    request.setDeleteOrders(true);
    
    SystemResetResponse response = systemResetService.performSystemReset(request);
    
    assertTrue(response.isSuccess());
    assertTrue(response.getStats().getTotalDeleted() > 0);
}
```

### API Tests (Postman/REST Client)
```bash
curl -X POST http://localhost:8080/api/v1/admin/system-reset/reset \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "deleteProducts": true,
    "deleteDiscounts": false,
    "deleteOrders": true,
    "deleteRewardSystems": false,
    "deleteShippingCosts": false,
    "deleteMoneyFlows": false,
    "deleteCategories": false,
    "deleteBrands": false,
    "deleteWarehouses": false
  }'
```

## Database Considerations

### Backup Recommendations
⚠️ **CRITICAL:** Always backup your database before performing system reset operations!

```bash
# PostgreSQL backup
pg_dump -U username -d database_name > backup_$(date +%Y%m%d_%H%M%S).sql

# MySQL backup
mysqldump -u username -p database_name > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Cascade Configuration
The implementation relies on:
1. **JPA Cascade Types:** `CascadeType.ALL` and `orphanRemoval = true`
2. **Database Constraints:** Foreign key constraints with `ON DELETE CASCADE`
3. **Manual Cleanup:** For relationships not handled by JPA cascading

## Monitoring and Logging

### Log Levels
- **INFO:** Operation start/completion, entity counts
- **DEBUG:** Request details, intermediate steps
- **ERROR:** Exceptions, failures, error details

### Log Examples
```
INFO  - Starting system reset operation
INFO  - Starting deletion of Products
INFO  - Deleted all cart items
INFO  - Deleted all reviews
INFO  - Deleted 1200 products
INFO  - System reset completed in 4567ms. Total deleted: 1523
```

## Security Considerations

1. **Authentication Required:** All endpoints require valid JWT token
2. **Authorization:** Only ADMIN role can access these endpoints
3. **Audit Logging:** All operations are logged with admin identifier
4. **Rate Limiting:** Consider implementing rate limiting for these endpoints
5. **Confirmation:** Frontend should implement confirmation dialogs

## Future Enhancements

### Potential Improvements
1. **Soft Delete Option:** Add flag to soft-delete instead of hard-delete
2. **Scheduled Resets:** Allow scheduling reset operations
3. **Selective Deletion:** Delete entities based on date ranges or filters
4. **Backup Integration:** Automatic backup before reset
5. **Progress Tracking:** WebSocket-based progress updates
6. **Rollback Support:** Transaction-based rollback capability
7. **Export Before Delete:** Export data before deletion

## Troubleshooting

### Common Issues

**Issue:** Timeout errors
**Solution:** Increase timeout values or reduce dataset size

**Issue:** Foreign key constraint violations
**Solution:** Ensure cascading is properly configured in entity relationships

**Issue:** Out of memory errors
**Solution:** Implement batch processing with smaller chunks

**Issue:** Transaction deadlocks
**Solution:** Adjust transaction isolation levels or execution order

## Support

For issues or questions:
1. Check application logs for detailed error messages
2. Verify database constraints and relationships
3. Ensure proper admin authentication
4. Review entity relationship mappings

---

**Version:** 1.0  
**Last Updated:** October 16, 2024  
**Author:** ShopSphere Development Team
