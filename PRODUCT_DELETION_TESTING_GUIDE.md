# Product Deletion Testing Guide

## Overview
This guide covers the comprehensive product deletion functionality that safely removes products and all their associated data while ensuring business rules are followed.

## Endpoint Details
- **URL**: `DELETE /api/v1/products/{productId}`
- **Method**: DELETE
- **Authentication**: Required (ADMIN or EMPLOYEE role only)
- **Content-Type**: `application/json`

## Functionality Overview

### What Gets Deleted
1. **Product Variants** - All variants of the product
2. **Variant Images** - From both database and Cloudinary
3. **Variant Attributes** - All attribute associations
4. **Main Product Images** - From both database and Cloudinary
5. **Main Product Videos** - From both database and Cloudinary
6. **Product Detail** - Extended product information
7. **Product Entity** - The main product record

### What Gets Cleaned Up
1. **Cart Items** - Removes product variants from all user carts
2. **Wishlist Items** - Removes product variants from all user wishlists

### What Gets Preserved
1. **Discounts** - Discount entities are not deleted
2. **Orders** - Order history is preserved (but prevents deletion if pending)

## Business Rules

### Deletion Prevention
A product **CANNOT** be deleted if:
- Any of its variants have pending orders that are NOT in these statuses:
  - `DELIVERED`
  - `CANCELLED`
  - `REFUNDED`
  - `RETURNED`

### Allowed Order Statuses for Deletion
- `PENDING` - ❌ **BLOCKS DELETION**
- `PROCESSING` - ❌ **BLOCKS DELETION**
- `SHIPPED` - ❌ **BLOCKS DELETION**
- `DELIVERED` - ✅ **ALLOWS DELETION**
- `CANCELLED` - ✅ **ALLOWS DELETION**
- `REFUNDED` - ✅ **ALLOWS DELETION**
- `RETURNED` - ✅ **ALLOWS DELETION**

## Testing Scenarios

### 1. Successful Product Deletion
**Prerequisites**: Product exists with no pending orders
**Expected Result**: HTTP 200 with success message

```json
{
  "success": true,
  "message": "Product deleted successfully",
  "productId": "uuid",
  "timestamp": 1640995200000
}
```

### 2. Deletion Blocked by Pending Orders
**Prerequisites**: Product has variants with pending orders
**Expected Result**: HTTP 400 with detailed error message

```json
{
  "success": false,
  "errorCode": "DELETION_BLOCKED",
  "message": "Cannot delete product 'AirPods Pro' because variant 'AIRPODS-BLUE-LG' has pending orders that are not yet delivered. Please ensure all orders are delivered, cancelled, refunded, or returned before deleting the product.",
  "timestamp": 1640995200000
}
```

### 3. Product Not Found
**Prerequisites**: Invalid product ID
**Expected Result**: HTTP 404 with error message

```json
{
  "success": false,
  "errorCode": "PRODUCT_NOT_FOUND",
  "message": "Product not found with ID: invalid-uuid",
  "timestamp": 1640995200000
}
```

### 4. Unauthorized Access
**Prerequisites**: User without ADMIN or EMPLOYEE role
**Expected Result**: HTTP 403 Forbidden

### 5. Unauthenticated Access
**Prerequisites**: No authentication token
**Expected Result**: HTTP 401 Unauthorized

## Postman Setup

### 1. Request Configuration
- **Method**: DELETE
- **URL**: `{{baseUrl}}/api/v1/products/{{productId}}`
- **Headers**:
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`

### 2. Environment Variables
```json
{
  "baseUrl": "http://localhost:8095/api/v1",
  "token": "your_jwt_token_here",
  "productId": "uuid_of_product_to_delete"
}
```

### 3. Test Cases

#### Test Case 1: Valid Product Deletion
1. Set `productId` to a valid product UUID
2. Ensure the product has no pending orders
3. Send DELETE request
4. **Expected**: HTTP 200 with success response

#### Test Case 2: Deletion with Pending Orders
1. Set `productId` to a product with pending orders
2. Send DELETE request
3. **Expected**: HTTP 400 with deletion blocked message

#### Test Case 3: Invalid Product ID
1. Set `productId` to an invalid UUID
2. Send DELETE request
3. **Expected**: HTTP 404 with product not found message

#### Test Case 4: Unauthorized Role
1. Use a token with USER role only
2. Send DELETE request
3. **Expected**: HTTP 403 Forbidden

## Database Verification Queries

### 1. Check Product Existence
```sql
SELECT product_id, product_name, created_at 
FROM products 
WHERE product_id = 'your-product-uuid';
```

### 2. Check Product Variants
```sql
SELECT id, variant_sku, price, stock_quantity 
FROM product_variants 
WHERE product_id = 'your-product-uuid';
```

### 3. Check Pending Orders
```sql
SELECT o.order_id, o.order_status, oi.variant_id, pv.variant_sku
FROM orders o
JOIN order_items oi ON o.order_id = oi.order_id
JOIN product_variants pv ON oi.variant_id = pv.id
WHERE pv.product_id = 'your-product-uuid'
AND o.order_status NOT IN ('DELIVERED', 'CANCELLED', 'REFUNDED', 'RETURNED');
```

### 4. Check Cart Items
```sql
SELECT ci.id, ci.cart_id, pv.variant_sku
FROM cart_items ci
JOIN product_variants pv ON ci.variant_id = pv.id
WHERE pv.product_id = 'your-product-uuid';
```

### 5. Check Wishlist Items
```sql
SELECT wp.id, wp.wishlist_id, pv.variant_sku
FROM wishlist_products wp
JOIN product_variants pv ON wp.variant_id = pv.id
WHERE pv.product_id = 'your-product-uuid';
```

### 6. Check Product Images
```sql
SELECT image_id, image_url, alt_text
FROM product_images
WHERE product_id = 'your-product-uuid';
```

### 7. Check Product Videos
```sql
SELECT video_id, url, title
FROM product_videos
WHERE product_id = 'your-product-uuid';
```

## Pre-Deletion Checklist

### 1. Verify Product Status
- [ ] Product exists in database
- [ ] Product has no active variants with pending orders
- [ ] All order statuses are in allowed states

### 2. Check Dependencies
- [ ] No cart items reference product variants
- [ ] No wishlist items reference product variants
- [ ] No pending orders for product variants

### 3. Backup Considerations
- [ ] Consider backing up product data before deletion
- [ ] Ensure no critical business data is lost

## Post-Deletion Verification

### 1. Database Cleanup Verification
- [ ] Product record is deleted
- [ ] All product variants are deleted
- [ ] All product images are deleted
- [ ] All product videos are deleted
- [ ] All variant images are deleted
- [ ] All variant attributes are deleted
- [ ] Product detail is deleted

### 2. External Service Cleanup
- [ ] Images deleted from Cloudinary
- [ ] Videos deleted from Cloudinary
- [ ] No orphaned files remain

### 3. Cart and Wishlist Cleanup
- [ ] No cart items reference deleted variants
- [ ] No wishlist items reference deleted variants

## Error Handling

### 1. ProductDeletionException
**When**: Pending orders prevent deletion
**Action**: Resolve pending orders before retrying

### 2. EntityNotFoundException
**When**: Product ID doesn't exist
**Action**: Verify product ID is correct

### 3. RuntimeException
**When**: Unexpected error during deletion
**Action**: Check logs for detailed error information

## Performance Considerations

### 1. Large Product Deletion
- For products with many variants/images, deletion may take time
- Consider using batch operations for very large products
- Monitor database performance during deletion

### 2. Cloudinary Cleanup
- Image/video deletion from Cloudinary is done sequentially
- Consider implementing retry logic for failed deletions
- Monitor Cloudinary API rate limits

## Security Considerations

### 1. Role-Based Access Control
- Only ADMIN and EMPLOYEE roles can delete products
- Ensure proper authentication and authorization

### 2. Audit Trail
- Consider logging all deletion attempts
- Track who deleted what and when

### 3. Soft Delete Option
- Consider implementing soft delete for critical products
- Maintain data history for compliance

## Troubleshooting

### Common Issues

#### 1. Deletion Blocked by Orders
**Problem**: Product deletion fails with "DELETION_BLOCKED" error
**Solution**: 
- Check order statuses for product variants
- Update orders to DELIVERED, CANCELLED, REFUNDED, or RETURNED
- Retry deletion

#### 2. Cloudinary Deletion Failures
**Problem**: Images/videos not deleted from Cloudinary
**Solution**:
- Check Cloudinary API credentials
- Verify image/video URLs are valid
- Check Cloudinary API rate limits

#### 3. Database Constraint Violations
**Problem**: Foreign key constraint errors during deletion
**Solution**:
- Ensure proper cascade settings
- Check for circular references
- Verify all related entities are properly mapped

#### 4. Partial Deletion
**Problem**: Only some parts of the product are deleted
**Solution**:
- Check transaction rollback on errors
- Verify all deletion methods are called
- Check for exception handling issues

## Success Criteria

### Functional Requirements
- [ ] Product and all variants are completely deleted
- [ ] All associated media is removed from Cloudinary
- [ ] Cart and wishlist items are cleaned up
- [ ] Pending orders prevent deletion
- [ ] Proper error messages are returned
- [ ] Role-based access control is enforced

### Performance Requirements
- [ ] Deletion completes within reasonable time
- [ ] No memory leaks during large deletions
- [ ] Database operations are optimized

### Security Requirements
- [ ] Only authorized roles can delete
- [ ] Proper authentication is required
- [ ] No sensitive data is exposed

## Testing Checklist

- [ ] Successful deletion with no pending orders
- [ ] Deletion blocked by pending orders
- [ ] Invalid product ID handling
- [ ] Unauthorized role access
- [ ] Unauthenticated access
- [ ] Database cleanup verification
- [ ] Cloudinary cleanup verification
- [ ] Cart and wishlist cleanup
- [ ] Error handling for all scenarios
- [ ] Performance under normal load
- [ ] Performance with large products
- [ ] Security and authorization
- [ ] Transaction rollback on errors
