# Product Variant Deletion Testing Guide

This guide provides comprehensive testing instructions for the new product variant deletion functionality in the e-commerce backend service.

## Overview

The product variant deletion functionality allows administrators and employees to remove specific variants from products. When a variant is deleted, all associated data is removed:

- Variant images (deleted from both database and Cloudinary)
- Variant attributes
- The variant entity itself

## API Endpoint

**DELETE** `/api/products/{productId}/variants/{variantId}`

### Path Parameters

- `productId` (UUID): The ID of the product containing the variant
- `variantId` (Long): The ID of the variant to delete

### Authentication

- **Required**: JWT Bearer token
- **Roles**: ADMIN or EMPLOYEE

### Headers

```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

## Testing Scenarios

### 1. Successful Variant Deletion

**Request:**

```http
DELETE /api/products/550e8400-e29b-41d4-a716-446655440000/variants/1
Authorization: Bearer <jwt_token>
```

**Expected Response:**

```json
{
  "success": true,
  "message": "Product variant deleted successfully",
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "variantId": 1,
  "timestamp": 1691748000000
}
```

**What Happens:**

- Variant is verified to exist and belong to the specified product
- All variant images are deleted from Cloudinary
- All variant images are removed from the database
- All variant attributes are removed from the database
- The variant entity is deleted
- Success response is returned

### 2. Product Not Found

**Request:**

```http
DELETE /api/products/550e8400-e29b-41d4-a716-446655440000/variants/1
Authorization: Bearer <jwt_token>
```

**Expected Response:**

```json
{
  "success": false,
  "errorCode": "ENTITY_NOT_FOUND",
  "message": "Product not found with ID: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1691748000000
}
```

**HTTP Status:** 404 Not Found

### 3. Variant Not Found

**Request:**

```http
DELETE /api/products/550e8400-e29b-41d4-a716-446655440000/variants/999
Authorization: Bearer <jwt_token>
```

**Expected Response:**

```json
{
  "success": false,
  "errorCode": "ENTITY_NOT_FOUND",
  "message": "Product variant not found with ID: 999",
  "timestamp": 1691748000000
}
```

**HTTP Status:** 404 Not Found

### 4. Variant Does Not Belong to Product

**Request:**

```http
DELETE /api/products/550e8400-e29b-41d4-a716-446655440000/variants/2
Authorization: Bearer <jwt_token>
```

**Expected Response:**

```json
{
  "success": false,
  "errorCode": "INVALID_ARGUMENT",
  "message": "Variant does not belong to the specified product",
  "timestamp": 1691748000000
}
```

**HTTP Status:** 400 Bad Request

### 5. Unauthorized Access

**Request:**

```http
DELETE /api/products/550e8400-e29b-41d4-a716-446655440000/variants/1
```

**Expected Response:**

```json
{
  "timestamp": "2023-08-11T07:56:55.123+00:00",
  "status": 401,
  "error": "Unauthorized",
  "path": "/api/products/550e8400-e29b-41d4-a716-446655440000/variants/1"
}
```

**HTTP Status:** 401 Unauthorized

### 6. Insufficient Permissions

**Request:**

```http
DELETE /api/products/550e8400-e29b-41d4-a716-446655440000/variants/1
Authorization: Bearer <jwt_token_with_user_role>
```

**Expected Response:**

```json
{
  "timestamp": "2023-08-11T07:56:55.123+00:00",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/products/550e8400-e29b-41d4-a716-446655440000/variants/1"
}
```

**HTTP Status:** 403 Forbidden

### 7. Cloudinary Deletion Failure

**Scenario:** One or more variant images fail to delete from Cloudinary

**Expected Behavior:**

- Individual image deletion failures are logged as warnings
- Process continues with other images
- Database cleanup proceeds normally
- Variant is still deleted successfully
- Success response is returned

**Logs:**

```
WARN - Failed to delete image from Cloudinary: https://res.cloudinary.com/... Error: Network timeout
INFO - Successfully deleted 2 variant images from database for variant ID: 1
```

## Postman Testing Setup

### 1. Environment Variables

Set up these variables in your Postman environment:

```
base_url: http://localhost:8095
jwt_token: <your_jwt_token>
product_id: <existing_product_uuid>
variant_id: <existing_variant_long_id>
```

### 2. Collection Setup

Create a new collection called "Product Variant Deletion" with the following request:

**Request Name:** Delete Product Variant
**Method:** DELETE
**URL:** `{{base_url}}/api/products/{{product_id}}/variants/{{variant_id}}`
**Headers:**

```
Authorization: Bearer {{jwt_token}}
Content-Type: application/json
```

### 3. Pre-request Script

```javascript
// Verify that required variables are set
if (!pm.environment.get("jwt_token")) {
  throw new Error("JWT token not set in environment");
}

if (!pm.environment.get("product_id")) {
  throw new Error("Product ID not set in environment");
}

if (!pm.environment.get("variant_id")) {
  throw new Error("Variant ID not set in environment");
}
```

### 4. Tests Script

```javascript
// Test successful deletion
pm.test("Status code is 200", function () {
  pm.response.to.have.status(200);
});

pm.test("Response has success flag", function () {
  const response = pm.response.json();
  pm.expect(response.success).to.be.true;
});

pm.test("Response contains correct product ID", function () {
  const response = pm.response.json();
  pm.expect(response.productId).to.eql(pm.environment.get("product_id"));
});

pm.test("Response contains correct variant ID", function () {
  const response = pm.response.json();
  pm.expect(response.variantId).to.eql(
    parseInt(pm.environment.get("variant_id"))
  );
});

pm.test("Response contains timestamp", function () {
  const response = pm.response.json();
  pm.expect(response.timestamp).to.be.a("number");
});
```

## Database Verification

After successful deletion, verify the following:

### 1. Check Variant Deletion

```sql
SELECT * FROM product_variants WHERE id = <variant_id>;
-- Should return no rows
```

### 2. Check Variant Images Deletion

```sql
SELECT * FROM product_variant_images WHERE product_variant_id = <variant_id>;
-- Should return no rows
```

### 3. Check Variant Attributes Deletion

```sql
SELECT * FROM variant_attribute_values WHERE product_variant_id = <variant_id>;
-- Should return no rows
```

### 4. Verify Product Still Exists

```sql
SELECT * FROM products WHERE product_id = '<product_uuid>';
-- Should return the product
```

## Error Handling Verification

### 1. Transaction Rollback

If any part of the deletion process fails, verify that:

- No partial deletions occur
- Database remains in a consistent state
- All operations are rolled back

### 2. Logging Verification

Check application logs for:

- Detailed error messages
- Proper error categorization
- Stack traces for debugging

## Performance Considerations

### 1. Large Variants

For variants with many images:

- Image deletion from Cloudinary happens sequentially to avoid overwhelming the service
- Database operations use batch deletion for efficiency

### 2. Concurrent Deletions

- Multiple variant deletions can happen concurrently
- Each deletion is wrapped in its own transaction
- No blocking between different variant deletions

## Security Considerations

### 1. Authorization

- Only ADMIN and EMPLOYEE roles can delete variants
- JWT token validation is enforced
- Role-based access control is implemented

### 2. Input Validation

- Product ID and variant ID are validated for format
- Existence checks prevent unauthorized deletions
- Belonging relationship is verified

## Monitoring and Alerting

### 1. Success Metrics

- Track successful variant deletions
- Monitor deletion response times
- Count variants deleted per time period

### 2. Error Metrics

- Track deletion failures by error type
- Monitor Cloudinary API response times
- Alert on high failure rates

## Troubleshooting

### Common Issues

1. **Cloudinary API Errors**

   - Check network connectivity
   - Verify Cloudinary credentials
   - Check API rate limits

2. **Database Constraint Violations**

   - Ensure no foreign key references exist
   - Check for active orders containing the variant
   - Verify cart items are properly cleaned up

3. **Transaction Timeouts**
   - Check database connection pool settings
   - Monitor long-running transactions
   - Consider breaking large deletions into smaller batches

### Debug Mode

Enable debug logging to see detailed operation flow:

```properties
logging.level.com.ecommerce.service=DEBUG
logging.level.com.ecommerce.service.impl.ProductServiceImpl=DEBUG
```

## Conclusion

The product variant deletion functionality provides a robust, secure, and efficient way to remove product variants while maintaining data integrity. The implementation includes comprehensive error handling, proper cleanup of associated resources, and detailed logging for monitoring and debugging purposes.

For any issues or questions, refer to the application logs and this testing guide for troubleshooting steps.
