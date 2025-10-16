# System Reset API Endpoints - Quick Reference

## Base URL
```
/api/v1/admin/system-reset
```

## Authentication
All endpoints require:
- **Bearer Token Authentication**
- **ADMIN Role**

## Endpoints

### 1. Comprehensive System Reset
**POST** `/reset`

Performs selective deletion based on checkbox selections.

**Request:**
```json
{
  "deleteProducts": true,
  "deleteDiscounts": true,
  "deleteOrders": true,
  "deleteRewardSystems": true,
  "deleteShippingCosts": true,
  "deleteMoneyFlows": true,
  "deleteCategories": true,
  "deleteBrands": true,
  "deleteWarehouses": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "System reset completed. Total entities deleted: 5432",
  "timestamp": "2024-10-16T15:20:30",
  "stats": {
    "productsDeleted": 1200,
    "discountsDeleted": 45,
    "ordersDeleted": 3500,
    "rewardSystemsDeleted": 2,
    "shippingCostsDeleted": 5,
    "moneyFlowsDeleted": 450,
    "categoriesDeleted": 80,
    "brandsDeleted": 100,
    "warehousesDeleted": 50,
    "totalDeleted": 5432,
    "executionTimeMs": 12345
  },
  "errors": []
}
```

---

### 2. Delete All Products
**DELETE** `/products`

Deletes all products with cascading relationships.

**Response:**
```json
{
  "success": true,
  "message": "All products deleted successfully",
  "deletedCount": 1200
}
```

---

### 3. Delete All Discounts
**DELETE** `/discounts`

Deletes all discounts with cascading relationships.

**Response:**
```json
{
  "success": true,
  "message": "All discounts deleted successfully",
  "deletedCount": 45
}
```

---

### 4. Delete All Orders
**DELETE** `/orders`

Deletes all orders with cascading relationships.

**Response:**
```json
{
  "success": true,
  "message": "All orders deleted successfully",
  "deletedCount": 3500
}
```

---

### 5. Delete All Reward Systems
**DELETE** `/reward-systems`

Deletes all reward systems with cascading relationships.

**Response:**
```json
{
  "success": true,
  "message": "All reward systems deleted successfully",
  "deletedCount": 2
}
```

---

### 6. Delete All Shipping Costs
**DELETE** `/shipping-costs`

Deletes all shipping cost configurations.

**Response:**
```json
{
  "success": true,
  "message": "All shipping costs deleted successfully",
  "deletedCount": 5
}
```

---

### 7. Delete All Money Flows
**DELETE** `/money-flows`

Deletes all money flow transaction records.

**Response:**
```json
{
  "success": true,
  "message": "All money flow records deleted successfully",
  "deletedCount": 450
}
```

---

### 8. Delete All Categories
**DELETE** `/categories`

Deletes all categories with cascading relationships.

**Response:**
```json
{
  "success": true,
  "message": "All categories deleted successfully",
  "deletedCount": 80
}
```

---

### 9. Delete All Brands
**DELETE** `/brands`

Deletes all brands with cascading relationships.

**Response:**
```json
{
  "success": true,
  "message": "All brands deleted successfully",
  "deletedCount": 100
}
```

---

### 10. Delete All Warehouses
**DELETE** `/warehouses`

Deletes all warehouses with cascading relationships.

**Response:**
```json
{
  "success": true,
  "message": "All warehouses deleted successfully",
  "deletedCount": 50
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "errorCode": "INVALID_REQUEST",
  "message": "At least one deletion option must be selected",
  "timestamp": 1697467230000
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "errorCode": "UNAUTHORIZED",
  "message": "Authentication required",
  "timestamp": 1697467230000
}
```

### 403 Forbidden
```json
{
  "success": false,
  "errorCode": "FORBIDDEN",
  "message": "Admin role required",
  "timestamp": 1697467230000
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "errorCode": "DELETION_ERROR",
  "message": "Failed to delete products: Connection timeout",
  "timestamp": 1697467230000
}
```

---

## cURL Examples

### Comprehensive Reset
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

### Delete All Products
```bash
curl -X DELETE http://localhost:8080/api/v1/admin/system-reset/products \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

### Delete All Orders
```bash
curl -X DELETE http://localhost:8080/api/v1/admin/system-reset/orders \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

---

## Postman Collection

Import this JSON into Postman:

```json
{
  "info": {
    "name": "System Reset API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Comprehensive System Reset",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{admin_token}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"deleteProducts\": true,\n  \"deleteDiscounts\": true,\n  \"deleteOrders\": true,\n  \"deleteRewardSystems\": true,\n  \"deleteShippingCosts\": true,\n  \"deleteMoneyFlows\": true,\n  \"deleteCategories\": true,\n  \"deleteBrands\": true,\n  \"deleteWarehouses\": true\n}"
        },
        "url": {
          "raw": "{{base_url}}/api/v1/admin/system-reset/reset",
          "host": ["{{base_url}}"],
          "path": ["api", "v1", "admin", "system-reset", "reset"]
        }
      }
    },
    {
      "name": "Delete All Products",
      "request": {
        "method": "DELETE",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{admin_token}}"
          }
        ],
        "url": {
          "raw": "{{base_url}}/api/v1/admin/system-reset/products",
          "host": ["{{base_url}}"],
          "path": ["api", "v1", "admin", "system-reset", "products"]
        }
      }
    }
  ]
}
```

---

## Frontend Integration Example

### React/Next.js Component
```typescript
import { useState } from 'react';

interface SystemResetRequest {
  deleteProducts: boolean;
  deleteDiscounts: boolean;
  deleteOrders: boolean;
  deleteRewardSystems: boolean;
  deleteShippingCosts: boolean;
  deleteMoneyFlows: boolean;
  deleteCategories: boolean;
  deleteBrands: boolean;
  deleteWarehouses: boolean;
}

export const SystemResetPanel = () => {
  const [loading, setLoading] = useState(false);
  const [selections, setSelections] = useState<SystemResetRequest>({
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

  const handleCheckboxChange = (key: keyof SystemResetRequest) => {
    setSelections(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const handleReset = async () => {
    if (!Object.values(selections).some(v => v)) {
      alert('Please select at least one option');
      return;
    }

    if (!confirm('Are you sure you want to reset the selected items? This action cannot be undone!')) {
      return;
    }

    setLoading(true);
    try {
      const response = await fetch('/api/v1/admin/system-reset/reset', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('adminToken')}`
        },
        body: JSON.stringify(selections)
      });

      const result = await response.json();
      
      if (result.success) {
        alert(`Reset completed! Total deleted: ${result.stats.totalDeleted}`);
      } else {
        alert(`Reset failed: ${result.message}`);
      }
    } catch (error) {
      alert('Error performing reset');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-6 bg-white rounded-lg shadow">
      <h2 className="text-2xl font-bold mb-4">System Reset</h2>
      <div className="space-y-2 mb-4">
        {Object.keys(selections).map(key => (
          <label key={key} className="flex items-center space-x-2">
            <input
              type="checkbox"
              checked={selections[key as keyof SystemResetRequest]}
              onChange={() => handleCheckboxChange(key as keyof SystemResetRequest)}
              className="w-4 h-4"
            />
            <span>{key.replace(/([A-Z])/g, ' $1').trim()}</span>
          </label>
        ))}
      </div>
      <button
        onClick={handleReset}
        disabled={loading}
        className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50"
      >
        {loading ? 'Resetting...' : 'Reset Selected Items'}
      </button>
    </div>
  );
};
```

---

## Notes

- ⚠️ **Always backup your database before using these endpoints**
- All operations are **irreversible**
- Operations use **multithreading** for efficiency
- Errors in one entity type **don't stop** others from being deleted
- All operations are **logged** for audit purposes
