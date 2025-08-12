# Product Search Testing Guide

## Overview
This guide covers the comprehensive product search functionality that replaces the old individual product retrieval endpoints. The new search system provides a unified, flexible way to filter products using various criteria.

## Endpoint Details
- **URL**: `POST /api/v1/products/search`
- **Method**: POST
- **Content-Type**: `application/json`
- **Authentication**: Required (Bearer token)

## ProductSearchDTO Structure

### Basic Product Identifiers
```json
{
  "productId": "uuid",
  "name": "string",
  "description": "string",
  "sku": "string",
  "barcode": "string",
  "slug": "string",
  "model": "string"
}
```

### Price Filters
```json
{
  "basePriceMin": 10.00,
  "basePriceMax": 100.00,
  "salePriceMin": 5.00,
  "salePriceMax": 80.00,
  "compareAtPriceMin": 15.00,
  "compareAtPriceMax": 120.00
}
```

### Stock Filters
```json
{
  "stockQuantityMin": 5,
  "stockQuantityMax": 50,
  "inStock": true
}
```

### Category and Brand Filters
```json
{
  "categoryId": 1,
  "categoryIds": [1, 2, 3],
  "includeSubcategories": true,
  "brandId": "uuid",
  "brandIds": ["uuid1", "uuid2"]
}
```

### Discount Filters
```json
{
  "discountId": "uuid",
  "discountIds": ["uuid1", "uuid2"],
  "discountPercentageMin": 10.0,
  "discountPercentageMax": 50.0,
  "hasDiscount": true,
  "isOnSale": true
}
```

### Feature Flags
```json
{
  "isFeatured": true,
  "isBestseller": true,
  "isNewArrival": true
}
```

### Rating and Review Filters
```json
{
  "averageRatingMin": 4.0,
  "averageRatingMax": 5.0,
  "reviewCountMin": 10,
  "reviewCountMax": 100
}
```

### Variant Filters
```json
{
  "variantCountMin": 2,
  "variantCountMax": 10,
  "variantAttributes": ["Color:Red", "Size:LG"]
}
```

### Physical Attributes
```json
{
  "weightMin": 0.1,
  "weightMax": 2.0,
  "dimensionsMin": 10.0,
  "dimensionsMax": 50.0
}
```

### Date Filters
```json
{
  "createdAtMin": "2024-01-01T00:00:00",
  "createdAtMax": "2024-12-31T23:59:59",
  "updatedAtMin": "2024-06-01T00:00:00",
  "updatedAtMax": "2024-12-31T23:59:59"
}
```

### Creator Filter
```json
{
  "createdBy": "uuid"
}
```

### Pagination and Sorting
```json
{
  "page": 0,
  "size": 20,
  "sortBy": "price",
  "sortDirection": "asc"
}
```

### Text Search
```json
{
  "searchKeyword": "airpods wireless"
}
```

## Testing Scenarios

### 1. Basic Search by Name
```json
{
  "name": "AirPods"
}
```

### 2. Price Range Search
```json
{
  "basePriceMin": 15.00,
  "basePriceMax": 25.00
}
```

### 3. Category Search with Subcategories
```json
{
  "categoryId": 1,
  "includeSubcategories": true
}
```

### 4. Featured Products
```json
{
  "isFeatured": true
}
```

### 5. Products with Discounts
```json
{
  "hasDiscount": true,
  "discountPercentageMin": 20.0
}
```

### 6. In-Stock Products
```json
{
  "inStock": true,
  "stockQuantityMin": 5
}
```

### 7. New Arrivals
```json
{
  "isNewArrival": true
}
```

### 8. Complex Multi-Criteria Search
```json
{
  "categoryId": 1,
  "basePriceMin": 10.00,
  "basePriceMax": 50.00,
  "isFeatured": true,
  "inStock": true,
  "page": 0,
  "size": 15,
  "sortBy": "price",
  "sortDirection": "asc"
}
```

### 9. Text Search Across Multiple Fields
```json
{
  "searchKeyword": "wireless bluetooth"
}
```

### 10. Brand-Specific Search
```json
{
  "brandId": "uuid",
  "isFeatured": true
}
```

## Postman Setup

### 1. Request Configuration
- **Method**: POST
- **URL**: `{{baseUrl}}/api/v1/products/search`
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`

### 2. Environment Variables
```json
{
  "baseUrl": "http://localhost:8095/api/v1",
  "token": "your_jwt_token_here"
}
```

### 3. Test Cases

#### Test Case 1: Valid Search
```json
{
  "name": "AirPods",
  "page": 0,
  "size": 10
}
```
**Expected Response**: HTTP 200 with paginated results

#### Test Case 2: No Filters Provided
```json
{}
```
**Expected Response**: HTTP 400 with error message "At least one filter criterion must be provided"

#### Test Case 3: Empty Results
```json
{
  "name": "NonExistentProduct"
}
```
**Expected Response**: HTTP 200 with empty page

#### Test Case 4: Invalid Price Range
```json
{
  "basePriceMin": 100.00,
  "basePriceMax": 50.00
}
```
**Expected Response**: HTTP 200 with empty results (no validation error, just no matches)

## Validation Rules

### Required Fields
- At least one filter criterion must be provided
- All fields are optional individually

### Field Validation
- **UUIDs**: Must be valid UUID format
- **Numbers**: Must be positive numbers
- **Dates**: Must be valid ISO 8601 format
- **Strings**: Trimmed and validated for empty strings

### Pagination Defaults
- **page**: Defaults to 0
- **size**: Defaults to 10
- **sortBy**: Defaults to "createdAt"
- **sortDirection**: Defaults to "desc"

## Response Format

### Success Response (HTTP 200)
```json
{
  "content": [
    {
      "productId": "uuid",
      "productName": "AirPods Pro",
      "shortDescription": "Wireless earbuds with active noise cancellation",
      "price": 249.99,
      "compareAtPrice": 299.99,
      "stockQuantity": 25,
      "category": {
        "id": 1,
        "name": "Electronics"
      },
      "brand": {
        "brandId": "uuid",
        "brandName": "Apple"
      },
      "isBestSeller": true,
      "isFeatured": true,
      "discountInfo": {
        "discountId": "uuid",
        "percentage": 16.67
      },
      "primaryImage": {
        "imageId": "uuid",
        "imageUrl": "https://example.com/image.jpg",
        "altText": "AirPods Pro"
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "empty": false
}
```

### Error Response (HTTP 400)
```json
{
  "errorCode": "INVALID_SEARCH_CRITERIA",
  "message": "At least one filter criterion must be provided",
  "timestamp": 1640995200000
}
```

### Error Response (HTTP 500)
```json
{
  "errorCode": "INTERNAL_ERROR",
  "message": "Failed to search products",
  "timestamp": 1640995200000
}
```

## Database Verification Queries

### 1. Check Products with Specific Category
```sql
SELECT p.product_id, p.product_name, c.name as category_name
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.id = 1;
```

### 2. Check Products with Discounts
```sql
SELECT p.product_id, p.product_name, d.percentage
FROM products p
JOIN discounts d ON p.discount_id = d.discount_id
WHERE d.is_active = true;
```

### 3. Check Featured Products
```sql
SELECT product_id, product_name, featured, bestseller
FROM products
WHERE featured = true OR bestseller = true;
```

### 4. Check Products by Price Range
```sql
SELECT product_id, product_name, price, sale_price
FROM products
WHERE price BETWEEN 10.00 AND 100.00;
```

### 5. Check Products by Stock Level
```sql
SELECT product_id, product_name, stock_quantity
FROM products
WHERE stock_quantity > 0;
```

## Performance Considerations

### 1. Indexing
Ensure the following database indexes exist:
- `products(category_id)`
- `products(brand_id)`
- `products(discount_id)`
- `products(price)`
- `products(stock_quantity)`
- `products(featured)`
- `products(bestseller)`
- `products(created_at)`
- `products(updated_at)`

### 2. Query Optimization
- Use pagination to limit result sets
- Avoid very large page sizes
- Use specific filters rather than broad text searches when possible

### 3. Caching
- Consider caching frequently used search results
- Implement Redis caching for popular search combinations

## Success Criteria

### Functional Requirements
- ✅ All filter criteria work correctly
- ✅ Pagination functions properly
- ✅ Sorting works for all sortable fields
- ✅ Empty results are handled gracefully
- ✅ Error handling is robust
- ✅ Response format is consistent

### Performance Requirements
- ✅ Search response time < 500ms for typical queries
- ✅ Handles large result sets efficiently
- ✅ Database queries are optimized

### Security Requirements
- ✅ Authentication is required
- ✅ Input validation prevents injection attacks
- ✅ Sensitive data is not exposed

## Troubleshooting

### Common Issues

#### 1. No Results Returned
- Check if filters are too restrictive
- Verify database has data matching criteria
- Check if category/brand IDs exist

#### 2. Slow Response Times
- Verify database indexes exist
- Check if page size is too large
- Monitor database query performance

#### 3. Authentication Errors
- Verify JWT token is valid
- Check if token has expired
- Ensure proper Authorization header format

#### 4. Validation Errors
- Check required field validation
- Verify data types match expected format
- Ensure at least one filter is provided

## Testing Checklist

- [ ] Basic search by name
- [ ] Price range filtering
- [ ] Category filtering
- [ ] Brand filtering
- [ ] Discount filtering
- [ ] Feature flag filtering
- [ ] Stock level filtering
- [ ] Date range filtering
- [ ] Pagination
- [ ] Sorting
- [ ] Empty results handling
- [ ] Error handling
- [ ] Authentication
- [ ] Input validation
- [ ] Response format consistency
- [ ] Performance under load
- [ ] Database query optimization
