# Wishlist Service Testing Guide

This guide provides comprehensive testing instructions for all wishlist-related endpoints in the e-commerce application.

## Overview

The wishlist service provides the following functionality:

- Add products to wishlist
- Update wishlist product notes and priority
- Remove products from wishlist
- View wishlist with pagination
- Clear entire wishlist
- Get specific wishlist products
- Check if wishlist has products
- Move products from wishlist to cart

## Base URL

```
http://localhost:8095/api/v1/wishlist
```

## Authentication

All endpoints require authentication. Use the JWT token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

## 1. Add Product to Wishlist

**Endpoint:** `POST /add`

**Description:** Add a product variant to the user's wishlist

**Request Parameters:**

- `userId` (UUID, required): The ID of the user adding the product

**Request Body:**

```json
{
  "variantId": 1,
  "notes": "Want to buy this for birthday",
  "priority": 2
}
```

**Response (Success - 201):**

```json
{
  "success": true,
  "message": "Product added to wishlist successfully",
  "data": {
    "id": 1,
    "variantId": 1,
    "variantSku": "AIRPODS-BLUE-LG",
    "productName": "AirPods Pro",
    "productImage": "https://example.com/image.jpg",
    "notes": "Want to buy this for birthday",
    "priority": 2,
    "addedAt": "2025-08-11T15:30:00",
    "inStock": true,
    "availableStock": 15,
    "price": 16.0
  }
}
```

**Error Responses:**

- **400 Bad Request:** Inactive variant
- **404 Not Found:** User or product variant not found
- **500 Internal Server Error:** Server-side errors

## 2. Update Wishlist Product

**Endpoint:** `PUT /update`

**Description:** Update the notes or priority of a wishlist product

**Request Parameters:**

- `userId` (UUID, required): The ID of the user updating the product

**Request Body:**

```json
{
  "wishlistProductId": 1,
  "notes": "Updated notes for this product",
  "priority": 1
}
```

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Wishlist product updated successfully",
  "data": {
    "id": 1,
    "variantId": 1,
    "variantSku": "AIRPODS-BLUE-LG",
    "productName": "AirPods Pro",
    "notes": "Updated notes for this product",
    "priority": 1,
    "addedAt": "2025-08-11T15:30:00",
    "inStock": true,
    "availableStock": 15,
    "price": 16.0
  }
}
```

**Error Responses:**

- **400 Bad Request:** Invalid wishlist product ID
- **404 Not Found:** User, wishlist, or wishlist product not found
- **500 Internal Server Error:** Server-side errors

## 3. Remove Product from Wishlist

**Endpoint:** `DELETE /remove/{wishlistProductId}`

**Description:** Remove a specific product from the user's wishlist

**Request Parameters:**

- `userId` (UUID, required): The ID of the user removing the product
- `wishlistProductId` (Long, path variable): The ID of the wishlist product to remove

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Product removed from wishlist successfully"
}
```

**Error Responses:**

- **404 Not Found:** User, wishlist, or wishlist product not found
- **500 Internal Server Error:** Server-side errors

## 4. View Wishlist

**Endpoint:** `GET /view`

**Description:** View the user's wishlist with pagination and sorting

**Request Parameters:**

- `userId` (UUID, required): The ID of the user viewing the wishlist
- `page` (int, optional, default: 0): Page number (0-based)
- `size` (int, optional, default: 10): Number of products per page
- `sortBy` (String, optional, default: "addedAt"): Field to sort by
- `sortDirection` (String, optional, default: "desc"): Sort direction ("asc" or "desc")

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Wishlist retrieved successfully",
  "data": {
    "wishlistId": 1,
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "products": [
      {
        "id": 1,
        "variantId": 1,
        "variantSku": "AIRPODS-BLUE-LG",
        "productName": "AirPods Pro",
        "productImage": "https://example.com/image.jpg",
        "notes": "Want to buy this for birthday",
        "priority": 2,
        "addedAt": "2025-08-11T15:30:00",
        "inStock": true,
        "availableStock": 15,
        "price": 16.0
      }
    ],
    "totalProducts": 1,
    "createdAt": "2025-08-11T15:30:00",
    "updatedAt": "2025-08-11T15:30:00",
    "isEmpty": false
  },
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

**Error Responses:**

- **404 Not Found:** User or wishlist not found
- **500 Internal Server Error:** Server-side errors

## 5. Clear Wishlist

**Endpoint:** `DELETE /clear`

**Description:** Remove all products from the user's wishlist

**Request Parameters:**

- `userId` (UUID, required): The ID of the user clearing the wishlist

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Wishlist cleared successfully"
}
```

**Error Responses:**

- **404 Not Found:** User or wishlist not found
- **500 Internal Server Error:** Server-side errors

## 6. Get Wishlist Product

**Endpoint:** `GET /product/{wishlistProductId}`

**Description:** Get details of a specific wishlist product

**Request Parameters:**

- `userId` (UUID, required): The ID of the user requesting the product
- `wishlistProductId` (Long, path variable): The ID of the wishlist product

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Wishlist product retrieved successfully",
  "data": {
    "id": 1,
    "variantId": 1,
    "variantSku": "AIRPODS-BLUE-LG",
    "productName": "AirPods Pro",
    "productImage": "https://example.com/image.jpg",
    "notes": "Want to buy this for birthday",
    "priority": 2,
    "addedAt": "2025-08-11T15:30:00",
    "inStock": true,
    "availableStock": 15,
    "price": 16.0
  }
}
```

**Error Responses:**

- **404 Not Found:** User, wishlist, or wishlist product not found
- **500 Internal Server Error:** Server-side errors

## 7. Check if Wishlist Has Products

**Endpoint:** `GET /has-products`

**Description:** Check if the user's wishlist contains any products

**Request Parameters:**

- `userId` (UUID, required): The ID of the user checking the wishlist

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Wishlist status checked successfully",
  "data": {
    "hasProducts": true
  }
}
```

**Error Responses:**

- **404 Not Found:** User or wishlist not found
- **500 Internal Server Error:** Server-side errors

## 8. Move Product to Cart

**Endpoint:** `POST /move-to-cart/{wishlistProductId}`

**Description:** Move a product from wishlist to cart and remove it from wishlist

**Request Parameters:**

- `userId` (UUID, required): The ID of the user moving the product
- `wishlistProductId` (Long, path variable): The ID of the wishlist product
- `quantity` (int, optional, default: 1): Quantity to add to cart

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Product moved to cart successfully"
}
```

**Error Responses:**

- **404 Not Found:** User, wishlist, or wishlist product not found
- **500 Internal Server Error:** Server-side errors

## Postman Testing Setup

### 1. Environment Variables

Set up these environment variables in Postman:

- `base_url`: `http://localhost:8095`
- `jwt_token`: Your JWT authentication token
- `user_id`: A valid user UUID for testing

### 2. Collection Setup

Create a new collection called "Wishlist Management" with the following structure:

#### Add Product to Wishlist

- **Method:** POST
- **URL:** `{{base_url}}/api/v1/wishlist/add?userId={{user_id}}`
- **Headers:**
  - `Authorization`: `Bearer {{jwt_token}}`
  - `Content-Type`: `application/json`
- **Body:** Raw JSON with variantId, notes, and priority

#### Update Wishlist Product

- **Method:** PUT
- **URL:** `{{base_url}}/api/v1/wishlist/update?userId={{user_id}}`
- **Headers:** Same as above
- **Body:** Raw JSON with wishlistProductId, notes, and priority

#### Remove Wishlist Product

- **Method:** DELETE
- **URL:** `{{base_url}}/api/v1/wishlist/remove/{{wishlist_product_id}}?userId={{user_id}}`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

#### View Wishlist

- **Method:** GET
- **URL:** `{{base_url}}/api/v1/wishlist/view?userId={{user_id}}&page=0&size=10&sortBy=addedAt&sortDirection=desc`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

#### Clear Wishlist

- **Method:** DELETE
- **URL:** `{{base_url}}/api/v1/wishlist/clear?userId={{user_id}}`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

#### Get Wishlist Product

- **Method:** GET
- **URL:** `{{base_url}}/api/v1/wishlist/product/{{wishlist_product_id}}?userId={{user_id}}`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

#### Check Wishlist Products

- **Method:** GET
- **URL:** `{{base_url}}/api/v1/wishlist/has-products?userId={{user_id}}`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

#### Move to Cart

- **Method:** POST
- **URL:** `{{base_url}}/api/v1/wishlist/move-to-cart/{{wishlist_product_id}}?userId={{user_id}}&quantity=2`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

## Testing Scenarios

### 1. Happy Path Testing

1. Add product to wishlist
2. Verify product appears in wishlist view
3. Update product notes/priority
4. Verify changes are applied
5. Move product to cart
6. Verify product is removed from wishlist

### 2. Edge Cases

1. **Add inactive product to wishlist**
   - Expected: 400 Bad Request with inactive error
2. **Add duplicate product to wishlist**
   - Expected: Product is updated with new notes/priority
3. **Access wishlist with invalid user ID**
   - Expected: 404 Not Found
4. **Access wishlist product that doesn't belong to user**
   - Expected: 400 Bad Request with ownership error

### 3. Pagination Testing

1. Add multiple products to wishlist
2. Test different page sizes (5, 10, 20)
3. Test sorting by different fields
4. Verify pagination metadata

### 4. Priority and Notes Testing

1. Add products with different priorities
2. Update notes for existing products
3. Verify priority sorting works correctly
4. Test empty notes and priority values

## Database Verification

### Check Wishlist Products

```sql
SELECT wp.id, wp.notes, wp.priority, wp.added_at,
       pv.variant_sku, p.product_name, u.email
FROM wishlist_products wp
JOIN product_variants pv ON wp.variant_id = pv.id
JOIN products p ON pv.product_id = p.id
JOIN wishlists w ON wp.wishlist_id = w.id
JOIN users u ON w.user_id = u.id
WHERE u.id = 'your-user-uuid'
ORDER BY wp.priority DESC, wp.added_at DESC;
```

### Check Wishlist Summary

```sql
SELECT w.id, u.email, COUNT(wp.id) as product_count,
       AVG(wp.priority) as avg_priority
FROM wishlists w
JOIN users u ON w.user_id = u.id
LEFT JOIN wishlist_products wp ON w.id = wp.wishlist_id
WHERE u.id = 'your-user-uuid'
GROUP BY w.id, u.email;
```

## Performance Considerations

1. **Pagination:** Always use pagination for wishlist views to handle large wishlists
2. **Indexing:** Ensure database indexes on frequently queried fields (priority, added_at)
3. **Caching:** Consider caching wishlist data for frequently accessed wishlists
4. **Batch Operations:** Use batch operations for clearing wishlists

## Security Notes

1. **User Isolation:** Users can only access their own wishlist
2. **Input Validation:** All inputs are validated for type and business rules
3. **Authentication:** All endpoints require valid JWT tokens
4. **Authorization:** Role-based access control is implemented

## Integration with Cart Service

The wishlist service includes a `moveToCart` method that:

1. Validates the wishlist product belongs to the user
2. Adds the product to the cart (TODO: implement cart service integration)
3. Removes the product from the wishlist

To complete this integration, you would need to:

1. Inject the `CartService` into `WishlistServiceImpl`
2. Call `cartService.addToCart()` before removing from wishlist
3. Handle cart-related errors appropriately

## Troubleshooting

### Common Issues

1. **401 Unauthorized**

   - Check JWT token validity
   - Verify token expiration
   - Ensure proper Authorization header format

2. **404 Not Found**

   - Verify user ID exists
   - Check if wishlist exists for user
   - Ensure wishlist product ID is valid

3. **400 Bad Request**

   - Check request body format
   - Verify variant is active
   - Ensure wishlist product ownership

4. **500 Internal Server Error**
   - Check server logs for detailed error messages
   - Verify database connectivity
   - Check entity relationships

### Debug Mode

Enable debug logging in `application.properties`:

```properties
logging.level.com.ecommerce.service=DEBUG
logging.level.com.ecommerce.controller=DEBUG
```

This will provide detailed information about wishlist operations and help identify issues.

## Differences from Cart Service

While the wishlist service follows the same pattern as the cart service, there are key differences:

1. **No Quantity Management:** Wishlist products don't have quantities
2. **Notes and Priority:** Wishlist products support notes and priority levels
3. **Duplicate Handling:** Adding the same product updates existing entry instead of creating new one
4. **Move to Cart:** Special functionality to transfer products from wishlist to cart
5. **No Stock Validation:** Wishlist doesn't check stock availability (only cart does)

## Future Enhancements

1. **Wishlist Sharing:** Allow users to share wishlists with others
2. **Wishlist Categories:** Organize wishlist products by categories
3. **Price Tracking:** Track price changes for wishlist products
4. **Stock Notifications:** Notify users when wishlist products come back in stock
5. **Bulk Operations:** Add/remove multiple products at once





