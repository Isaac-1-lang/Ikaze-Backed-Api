# GET ALL PRODUCTS TESTING GUIDE

## Overview

This guide covers testing the `getAllProducts` endpoint that returns a paginated list of products in `ManyProductsDto` format, optimized for displaying products in cards.

## Endpoint Details

- **URL**: `GET /api/v1/products`
- **Method**: GET
- **Authentication**: Not required (public endpoint)
- **Response Type**: `Page<ManyProductsDto>`

## Query Parameters

| Parameter       | Type   | Default     | Description                      |
| --------------- | ------ | ----------- | -------------------------------- |
| `page`          | int    | 0           | Page number (0-based)            |
| `size`          | int    | 10          | Number of products per page      |
| `sortBy`        | String | "createdAt" | Field to sort by                 |
| `sortDirection` | String | "desc"      | Sort direction ("asc" or "desc") |

## ManyProductsDto Response Structure

The response contains only essential fields needed for product cards:

```json
{
  "content": [
    {
      "productId": "uuid",
      "productName": "string",
      "shortDescription": "string",
      "price": 0.0,
      "compareAtPrice": 0.0,
      "stockQuantity": 0,
      "category": {
        "categoryId": 0,
        "categoryName": "string",
        "description": "string"
      },
      "brand": {
        "brandId": "uuid",
        "brandName": "string",
        "description": "string"
      },
      "isBestSeller": false,
      "isFeatured": false,
      "discountInfo": {
        "discountId": "uuid",
        "percentage": 0,
        "startDate": "date",
        "endDate": "date",
        "isActive": false
      },
      "primaryImage": {
        "id": 0,
        "imageUrl": "string",
        "altText": "string",
        "isPrimary": false,
        "width": 0,
        "height": 0
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": false,
      "unsorted": true
    }
  },
  "totalElements": 0,
  "totalPages": 0,
  "last": false,
  "first": true,
  "size": 10,
  "number": 0,
  "sort": {
    "sorted": false,
    "unsorted": true
  },
  "numberOfElements": 0
}
```

## Testing Scenarios

### 1. Basic Pagination Test

**Request:**

```
GET /api/v1/products?page=0&size=5
```

**Expected Response:**

- Status: 200 OK
- Content: Array of up to 5 products
- Pagination info with correct page numbers

### 2. Custom Page Size Test

**Request:**

```
GET /api/v1/products?page=0&size=20
```

**Expected Response:**

- Status: 200 OK
- Content: Array of up to 20 products
- Pagination info with size=20

### 3. Sorting Test

**Request:**

```
GET /api/v1/products?sortBy=productName&sortDirection=asc
```

**Expected Response:**

- Status: 200 OK
- Products sorted alphabetically by name (A-Z)

### 4. Price Sorting Test

**Request:**

```
GET /api/v1/products?sortBy=price&sortDirection=desc
```

**Expected Response:**

- Status: 200 OK
- Products sorted by price (highest to lowest)

### 5. Multiple Parameters Test

**Request:**

```
GET /api/v1/products?page=1&size=15&sortBy=createdAt&sortDirection=desc
```

**Expected Response:**

- Status: 200 OK
- Second page (page=1) with 15 products
- Sorted by creation date (newest first)

### 6. Edge Cases Test

**Request:**

```
GET /api/v1/products?page=999&size=100
```

**Expected Response:**

- Status: 200 OK
- Empty content array if page doesn't exist
- Correct pagination metadata

## Postman Setup

### 1. Create New Request

- Method: GET
- URL: `{{baseUrl}}/api/v1/products`

### 2. Add Query Parameters

- **Params Tab:**
  - `page`: 0
  - `size`: 10
  - `sortBy`: createdAt
  - `sortDirection`: desc

### 3. Headers

- `Content-Type`: application/json
- `Accept`: application/json

### 4. Environment Variables

```
baseUrl: http://localhost:8095/api/v1
```

## Validation Rules

### Response Validation

1. **Content Array**: Should contain products matching the requested page and size
2. **Pagination Metadata**:
   - `totalElements`: Total number of products in database
   - `totalPages`: Calculated based on size and total elements
   - `pageNumber`: Should match requested page
   - `pageSize`: Should match requested size
3. **Product Fields**: Each product should have all required fields populated
4. **Primary Image**: Should be the designated primary image or first available image

### Data Integrity

1. **Product Count**: Total elements should match actual product count in database
2. **Page Calculation**: `totalPages = ceil(totalElements / size)`
3. **Page Boundaries**: Page numbers should be 0-based and within valid range
4. **Sorting**: Products should be properly sorted according to parameters

## Database Verification Queries

### Check Total Product Count

```sql
SELECT COUNT(*) FROM products WHERE is_active = true;
```

### Verify Pagination

```sql
-- First page (0), size 10
SELECT * FROM products
WHERE is_active = true
ORDER BY created_at DESC
LIMIT 10 OFFSET 0;

-- Second page (1), size 10
SELECT * FROM products
WHERE is_active = true
ORDER BY created_at DESC
LIMIT 10 OFFSET 10;
```

### Check Product Images

```sql
SELECT p.product_id, p.product_name, pi.image_url, pi.is_primary
FROM products p
LEFT JOIN product_images pi ON p.product_id = pi.product_id
WHERE p.is_active = true
ORDER BY p.created_at DESC;
```

## Error Handling Tests

### 1. Invalid Page Number

**Request:**

```
GET /api/v1/products?page=-1
```

**Expected Response:**

- Status: 400 Bad Request
- Error message about invalid pagination parameters

### 2. Invalid Size

**Request:**

```
GET /api/v1/products?size=0
```

**Expected Response:**

- Status: 400 Bad Request
- Error message about invalid pagination parameters

### 3. Invalid Sort Direction

**Request:**

```
GET /api/v1/products?sortDirection=invalid
```

**Expected Response:**

- Status: 200 OK (should default to "desc")
- Products returned with default sorting

## Performance Considerations

### Expected Response Times

- **Small dataset (< 100 products)**: < 100ms
- **Medium dataset (100-1000 products)**: < 200ms
- **Large dataset (> 1000 products)**: < 500ms

### Database Indexes

Ensure the following indexes exist for optimal performance:

```sql
-- For sorting by creation date
CREATE INDEX idx_products_created_at ON products(created_at);

-- For sorting by product name
CREATE INDEX idx_products_name ON products(product_name);

-- For sorting by price
CREATE INDEX idx_products_price ON products(price);

-- For active products filter
CREATE INDEX idx_products_active ON products(is_active);
```

## Success Criteria

1. ✅ Endpoint returns 200 OK for valid requests
2. ✅ Pagination works correctly with different page sizes
3. ✅ Sorting works for all supported fields
4. ✅ Response contains only essential fields (ManyProductsDto)
5. ✅ Primary images are correctly identified and included
6. ✅ Pagination metadata is accurate
7. ✅ Error handling works for invalid parameters
8. ✅ Performance is acceptable for expected dataset sizes

## Notes

- The `ManyProductsDto` is designed to be lightweight for card display
- Full product details are available via the individual product endpoint (`/api/v1/products/{productId}`)
- This endpoint is public and doesn't require authentication
- Pagination is 0-based (first page is page 0)
- Default sorting is by creation date (newest first)
