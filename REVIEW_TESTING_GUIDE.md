# Review Service Testing Guide

This guide provides comprehensive testing instructions for all review-related endpoints in the e-commerce application.

## Overview

The review service provides the following functionality:

- Create product reviews (customers)
- Update reviews (customers)
- Delete reviews (customers, employees, admins)
- Read reviews (anyone)
- Search and filter reviews
- Get review statistics
- Vote on reviews (helpful/not helpful)
- Moderate reviews (admins/employees)

## Base URL

```
http://localhost:8095/api/v1/reviews
```

## Authentication

Most endpoints require authentication. Use the JWT token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

## 1. Create Review

**Endpoint:** `POST /create`

**Description:** Create a new product review (requires authentication)

**Headers:**

```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**

```json
{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "rating": 5,
  "title": "Excellent Product!",
  "content": "This product exceeded my expectations. Great quality and fast delivery."
}
```

**Response (201 Created):**

```json
{
  "success": true,
  "message": "Review created successfully",
  "data": {
    "id": 1,
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "userName": "John Doe",
    "userEmail": "john@example.com",
    "productId": "550e8400-e29b-41d4-a716-446655440000",
    "productName": "AirPods Pro",
    "rating": 5,
    "title": "Excellent Product!",
    "content": "This product exceeded my expectations. Great quality and fast delivery.",
    "status": "PENDING",
    "isVerifiedPurchase": false,
    "helpfulVotes": 0,
    "notHelpfulVotes": 0,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00",
    "canEdit": true,
    "canDelete": true
  }
}
```

## 2. Update Review

**Endpoint:** `PUT /update`

**Description:** Update an existing review (only by author or admin/employee)

**Headers:**

```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**

```json
{
  "reviewId": 1,
  "rating": 4,
  "title": "Good Product with Minor Issues",
  "content": "Overall good product, but there are some minor issues with the battery life."
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Review updated successfully",
  "data": {
    "id": 1,
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "userName": "John Doe",
    "userEmail": "john@example.com",
    "productId": "550e8400-e29b-41d4-a716-446655440000",
    "productName": "AirPods Pro",
    "rating": 4,
    "title": "Good Product with Minor Issues",
    "content": "Overall good product, but there are some minor issues with the battery life.",
    "status": "PENDING",
    "isVerifiedPurchase": false,
    "helpfulVotes": 0,
    "notHelpfulVotes": 0,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:00:00",
    "canEdit": true,
    "canDelete": true
  }
}
```

## 3. Delete Review

**Endpoint:** `DELETE /{reviewId}`

**Description:** Delete a review (only by author or admin/employee)

**Headers:**

```
Authorization: Bearer <jwt_token>
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Review deleted successfully"
}
```

## 4. Get Review

**Endpoint:** `GET /{reviewId}`

**Description:** Get a specific review by ID (public endpoint)

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Review retrieved successfully",
  "data": {
    "id": 1,
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "userName": "John Doe",
    "userEmail": "john@example.com",
    "productId": "550e8400-e29b-41d4-a716-446655440000",
    "productName": "AirPods Pro",
    "rating": 4,
    "title": "Good Product with Minor Issues",
    "content": "Overall good product, but there are some minor issues with the battery life.",
    "status": "APPROVED",
    "isVerifiedPurchase": false,
    "helpfulVotes": 5,
    "notHelpfulVotes": 1,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:00:00",
    "canEdit": false,
    "canDelete": false
  }
}
```

## 5. Get Product Reviews

**Endpoint:** `GET /product/{productId}`

**Description:** Get all approved reviews for a specific product (public endpoint)

**Query Parameters:**

- `page` (default: 0) - Page number
- `size` (default: 10) - Page size
- `sortBy` (default: "createdAt") - Sort field (rating, createdAt, helpfulVotes)
- `sortDirection` (default: "desc") - Sort direction (asc, desc)

**Example:**

```
GET /product/550e8400-e29b-41d4-a716-446655440000?page=0&size=5&sortBy=rating&sortDirection=desc
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Product reviews retrieved successfully",
  "data": [
    {
      "id": 1,
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "userName": "John Doe",
      "userEmail": "john@example.com",
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "productName": "AirPods Pro",
      "rating": 5,
      "title": "Excellent Product!",
      "content": "This product exceeded my expectations.",
      "status": "APPROVED",
      "isVerifiedPurchase": true,
      "helpfulVotes": 10,
      "notHelpfulVotes": 0,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00",
      "canEdit": false,
      "canDelete": false
    }
  ],
  "pagination": {
    "page": 0,
    "size": 5,
    "totalElements": 25,
    "totalPages": 5
  }
}
```

## 6. Get User Reviews

**Endpoint:** `GET /user`

**Description:** Get all reviews by the authenticated user

**Headers:**

```
Authorization: Bearer <jwt_token>
```

**Query Parameters:**

- `page` (default: 0) - Page number
- `size` (default: 10) - Page size

**Response (200 OK):**

```json
{
  "success": true,
  "message": "User reviews retrieved successfully",
  "data": [
    {
      "id": 1,
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "userName": "John Doe",
      "userEmail": "john@example.com",
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "productName": "AirPods Pro",
      "rating": 5,
      "title": "Excellent Product!",
      "content": "This product exceeded my expectations.",
      "status": "APPROVED",
      "isVerifiedPurchase": true,
      "helpfulVotes": 10,
      "notHelpfulVotes": 0,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00",
      "canEdit": true,
      "canDelete": true
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

## 7. Search Reviews

**Endpoint:** `POST /search`

**Description:** Search and filter reviews (public endpoint)

**Request Body:**

```json
{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "minRating": 4,
  "maxRating": 5,
  "status": "APPROVED",
  "isVerifiedPurchase": true,
  "keyword": "excellent",
  "sortBy": "helpfulVotes",
  "sortDirection": "desc",
  "page": 0,
  "size": 10
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Reviews search completed successfully",
  "data": [
    {
      "id": 1,
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "userName": "John Doe",
      "userEmail": "john@example.com",
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "productName": "AirPods Pro",
      "rating": 5,
      "title": "Excellent Product!",
      "content": "This product exceeded my expectations.",
      "status": "APPROVED",
      "isVerifiedPurchase": true,
      "helpfulVotes": 15,
      "notHelpfulVotes": 0,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00",
      "canEdit": false,
      "canDelete": false
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 8,
    "totalPages": 1
  }
}
```

## 8. Get Product Review Stats

**Endpoint:** `GET /product/{productId}/stats`

**Description:** Get review statistics for a product (public endpoint)

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Product review stats retrieved successfully",
  "data": {
    "averageRating": 4.2,
    "reviewCount": 25,
    "ratingDistribution": [
      [5, 12],
      [4, 8],
      [3, 3],
      [2, 1],
      [1, 1]
    ]
  }
}
```

## 9. Vote on Review

**Endpoint:** `POST /{reviewId}/vote`

**Description:** Vote helpful or not helpful on a review (requires authentication)

**Headers:**

```
Authorization: Bearer <jwt_token>
```

**Query Parameters:**

- `isHelpful` (required) - true for helpful, false for not helpful

**Example:**

```
POST /1/vote?isHelpful=true
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Vote recorded successfully"
}
```

## 10. Moderate Review

**Endpoint:** `POST /{reviewId}/moderate`

**Description:** Moderate a review (admin/employee only)

**Headers:**

```
Authorization: Bearer <jwt_token>
```

**Query Parameters:**

- `status` (required) - PENDING, APPROVED, REJECTED
- `moderatorNotes` (optional) - Notes from moderator

**Example:**

```
POST /1/moderate?status=APPROVED&moderatorNotes=Great review, approved
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Review moderated successfully",
  "data": {
    "id": 1,
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "userName": "John Doe",
    "userEmail": "john@example.com",
    "productId": "550e8400-e29b-41d4-a716-446655440000",
    "productName": "AirPods Pro",
    "rating": 5,
    "title": "Excellent Product!",
    "content": "This product exceeded my expectations.",
    "status": "APPROVED",
    "isVerifiedPurchase": false,
    "helpfulVotes": 0,
    "notHelpfulVotes": 0,
    "moderatorNotes": "Great review, approved",
    "moderatedBy": "admin@example.com",
    "moderatedAt": "2024-01-15T12:00:00",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T12:00:00",
    "canEdit": false,
    "canDelete": false
  }
}
```

## Error Responses

### Validation Error (400 Bad Request)

```json
{
  "success": false,
  "message": "Rating must be at least 1",
  "error": "VALIDATION_ERROR"
}
```

### Not Found Error (404 Not Found)

```json
{
  "success": false,
  "message": "Review not found",
  "error": "NOT_FOUND"
}
```

### Authorization Error (400 Bad Request)

```json
{
  "success": false,
  "message": "You are not authorized to edit this review",
  "error": "VALIDATION_ERROR"
}
```

### Internal Server Error (500 Internal Server Error)

```json
{
  "success": false,
  "message": "Failed to create review",
  "error": "INTERNAL_ERROR"
}
```

## Testing Scenarios

### 1. Customer Review Flow

1. Login as a customer
2. Create a review for a product
3. Update the review
4. Vote on other reviews
5. View your own reviews

### 2. Admin/Employee Moderation Flow

1. Login as admin/employee
2. View pending reviews
3. Moderate reviews (approve/reject)
4. Add moderator notes

### 3. Public Review Browsing

1. View product reviews without authentication
2. Search and filter reviews
3. View review statistics
4. Sort reviews by different criteria

### 4. Error Handling

1. Try to create duplicate reviews
2. Try to edit/delete reviews you don't own
3. Try to moderate without proper permissions
4. Test with invalid data

## Notes

- Reviews are created with "PENDING" status by default
- Only approved reviews are shown in public endpoints
- Users can only edit/delete their own reviews (unless admin/employee)
- Verified purchase status is currently set to false (TODO: implement order history check)
- Review moderation is required for content approval
- Voting system allows users to mark reviews as helpful/not helpful
