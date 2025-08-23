# Cart Service Testing Guide

This guide provides comprehensive testing instructions for all cart-related endpoints in the e-commerce application.

## Overview

The cart service provides the following functionality:

- Add items to cart
- Update cart item quantities
- Remove items from cart
- View cart with pagination
- Clear entire cart
- Get specific cart items
- Check if cart has items

## Base URL

```
http://localhost:8081/api/v1/cart
```

## Authentication

All endpoints require authentication. Use the JWT token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

## 1. Add Item to Cart

**Endpoint:** `POST /add`

**Description:** Add a product variant to the user's shopping cart

**Request Parameters:**

- `userId` (UUID, required): The ID of the user adding the item

**Request Body:**

```json
{
  "variantId": 1,
  "quantity": 2
}
```

**Response (Success - 201):**

```json
{
  "success": true,
  "message": "Item added to cart successfully",
  "data": {
    "id": 1,
    "variantId": 1,
    "variantSku": "AIRPODS-BLUE-LG",
    "productName": "AirPods Pro",
    "productImage": "https://example.com/image.jpg",
    "quantity": 2,
    "price": 16.0,
    "totalPrice": 32.0,
    "addedAt": "2025-08-11T15:30:00",
    "inStock": true,
    "availableStock": 15
  }
}
```

**Error Responses:**

- **400 Bad Request:** Invalid quantity, insufficient stock, inactive variant
- **404 Not Found:** User or product variant not found
- **500 Internal Server Error:** Server-side errors

## 2. Update Cart Item Quantity

**Endpoint:** `PUT /update`

**Description:** Update the quantity of an existing item in the cart

**Request Parameters:**

- `userId` (UUID, required): The ID of the user updating the item

**Request Body:**

```json
{
  "cartItemId": 1,
  "quantity": 3
}
```

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Cart item updated successfully",
  "data": {
    "id": 1,
    "variantId": 1,
    "variantSku": "AIRPODS-BLUE-LG",
    "productName": "AirPods Pro",
    "quantity": 3,
    "price": 16.0,
    "totalPrice": 48.0,
    "addedAt": "2025-08-11T15:30:00",
    "inStock": true,
    "availableStock": 15
  }
}
```

**Error Responses:**

- **400 Bad Request:** Quantity exceeds available stock, invalid cart item ID
- **404 Not Found:** User, cart, or cart item not found
- **500 Internal Server Error:** Server-side errors

## 3. Remove Item from Cart

**Endpoint:** `DELETE /remove/{cartItemId}`

**Description:** Remove a specific item from the user's cart

**Request Parameters:**

- `userId` (UUID, required): The ID of the user removing the item
- `cartItemId` (Long, path variable): The ID of the cart item to remove

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Item removed from cart successfully"
}
```

**Error Responses:**

- **404 Not Found:** User, cart, or cart item not found
- **500 Internal Server Error:** Server-side errors

## 4. View Cart

**Endpoint:** `GET /view`

**Description:** View the user's cart with pagination and sorting

**Request Parameters:**

- `userId` (UUID, required): The ID of the user viewing the cart
- `page` (int, optional, default: 0): Page number (0-based)
- `size` (int, optional, default: 10): Number of items per page
- `sortBy` (String, optional, default: "addedAt"): Field to sort by
- `sortDirection` (String, optional, default: "desc"): Sort direction ("asc" or "desc")

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Cart retrieved successfully",
  "data": {
    "cartId": 1,
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "items": [
      {
        "id": 1,
        "variantId": 1,
        "variantSku": "AIRPODS-BLUE-LG",
        "productName": "AirPods Pro",
        "productImage": "https://example.com/image.jpg",
        "quantity": 2,
        "price": 16.0,
        "totalPrice": 32.0,
        "addedAt": "2025-08-11T15:30:00",
        "inStock": true,
        "availableStock": 15
      }
    ],
    "totalItems": 1,
    "subtotal": 32.0,
    "total": 32.0,
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

- **404 Not Found:** User or cart not found
- **500 Internal Server Error:** Server-side errors

## 5. Clear Cart

**Endpoint:** `DELETE /clear`

**Description:** Remove all items from the user's cart

**Request Parameters:**

- `userId` (UUID, required): The ID of the user clearing the cart

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Cart cleared successfully"
}
```

**Error Responses:**

- **404 Not Found:** User or cart not found
- **500 Internal Server Error:** Server-side errors

## 6. Get Cart Item

**Endpoint:** `GET /item/{cartItemId}`

**Description:** Get details of a specific cart item

**Request Parameters:**

- `userId` (UUID, required): The ID of the user requesting the item
- `cartItemId` (Long, path variable): The ID of the cart item

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Cart item retrieved successfully",
  "data": {
    "id": 1,
    "variantId": 1,
    "variantSku": "AIRPODS-BLUE-LG",
    "productName": "AirPods Pro",
    "productImage": "https://example.com/image.jpg",
    "quantity": 2,
    "price": 16.0,
    "totalPrice": 32.0,
    "addedAt": "2025-08-11T15:30:00",
    "inStock": true,
    "availableStock": 15
  }
}
```

**Error Responses:**

- **404 Not Found:** User, cart, or cart item not found
- **500 Internal Server Error:** Server-side errors

## 7. Check if Cart Has Items

**Endpoint:** `GET /has-items`

**Description:** Check if the user's cart contains any items

**Request Parameters:**

- `userId` (UUID, required): The ID of the user checking the cart

**Response (Success - 200):**

```json
{
  "success": true,
  "message": "Cart status checked successfully",
  "data": {
    "hasItems": true
  }
}
```

**Error Responses:**

- **404 Not Found:** User or cart not found
- **500 Internal Server Error:** Server-side errors

## Postman Testing Setup

### 1. Environment Variables

Set up these environment variables in Postman:

- `base_url`: `http://localhost:8081`
- `jwt_token`: Your JWT authentication token
- `user_id`: A valid user UUID for testing

### 2. Collection Setup

Create a new collection called "Cart Management" with the following structure:

#### Add Item to Cart

- **Method:** POST
- **URL:** `{{base_url}}/api/v1/cart/add?userId={{user_id}}`
- **Headers:**
  - `Authorization`: `Bearer {{jwt_token}}`
  - `Content-Type`: `application/json`
- **Body:** Raw JSON with variantId and quantity

#### Update Cart Item

- **Method:** PUT
- **URL:** `{{base_url}}/api/v1/cart/update?userId={{user_id}}`
- **Headers:** Same as above
- **Body:** Raw JSON with cartItemId and quantity

#### Remove Cart Item

- **Method:** DELETE
- **URL:** `{{base_url}}/api/v1/cart/remove/{{cart_item_id}}?userId={{user_id}}`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

#### View Cart

- **Method:** GET
- **URL:** `{{base_url}}/api/v1/cart/view?userId={{user_id}}&page=0&size=10&sortBy=addedAt&sortDirection=desc`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

#### Clear Cart

- **Method:** DELETE
- **URL:** `{{base_url}}/api/v1/cart/clear?userId={{user_id}}`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

#### Get Cart Item

- **Method:** GET
- **URL:** `{{base_url}}/api/v1/cart/item/{{cart_item_id}}?userId={{user_id}}`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

#### Check Cart Items

- **Method:** GET
- **URL:** `{{base_url}}/api/v1/cart/has-items?userId={{user_id}}`
- **Headers:** `Authorization`: `Bearer {{jwt_token}}`

## Testing Scenarios

### 1. Happy Path Testing

1. Add item to cart
2. Verify item appears in cart view
3. Update item quantity
4. Verify quantity change
5. Remove item from cart
6. Verify item is removed

### 2. Edge Cases

1. **Add item with quantity exceeding stock**
   - Expected: 400 Bad Request with stock error
2. **Add item to inactive variant**
   - Expected: 400 Bad Request with inactive error
3. **Update quantity to exceed stock**
   - Expected: 400 Bad Request with stock error
4. **Access cart with invalid user ID**
   - Expected: 404 Not Found
5. **Access cart item that doesn't belong to user**
   - Expected: 400 Bad Request with ownership error

### 3. Pagination Testing

1. Add multiple items to cart
2. Test different page sizes (5, 10, 20)
3. Test sorting by different fields
4. Verify pagination metadata

### 4. Concurrency Testing

1. Open multiple browser tabs
2. Add/update items simultaneously
3. Verify data consistency

## Database Verification

### Check Cart Items

```sql
SELECT ci.id, ci.quantity, ci.added_at,
       pv.variant_sku, p.product_name, u.email
FROM cart_items ci
JOIN product_variants pv ON ci.variant_id = pv.id
JOIN products p ON pv.product_id = p.id
JOIN carts c ON ci.cart_id = c.id
JOIN users u ON c.user_id = u.id
WHERE u.id = 'your-user-uuid'
ORDER BY ci.added_at DESC;
```

### Check Cart Summary

```sql
SELECT c.id, u.email, COUNT(ci.id) as item_count,
       SUM(ci.quantity * pv.price) as total_value
FROM carts c
JOIN users u ON c.user_id = u.id
LEFT JOIN cart_items ci ON c.id = ci.cart_id
LEFT JOIN product_variants pv ON ci.variant_id = pv.id
WHERE u.id = 'your-user-uuid'
GROUP BY c.id, u.email;
```

## Performance Considerations

1. **Pagination:** Always use pagination for cart views to handle large carts
2. **Indexing:** Ensure database indexes on frequently queried fields
3. **Caching:** Consider caching cart data for frequently accessed carts
4. **Batch Operations:** Use batch operations for clearing carts

## Security Notes

1. **User Isolation:** Users can only access their own cart
2. **Input Validation:** All inputs are validated for type and business rules
3. **Authentication:** All endpoints require valid JWT tokens
4. **Authorization:** Role-based access control is implemented

## Troubleshooting

### Common Issues

1. **401 Unauthorized**

   - Check JWT token validity
   - Verify token expiration
   - Ensure proper Authorization header format

2. **404 Not Found**

   - Verify user ID exists
   - Check if cart exists for user
   - Ensure cart item ID is valid

3. **400 Bad Request**

   - Check request body format
   - Verify quantity constraints
   - Ensure variant is active and in stock

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

This will provide detailed information about cart operations and help identify issues.
