# Product Update API Testing Guide

## Overview

This guide covers testing the new product update functionality implemented in the e-commerce backend. The update API allows partial updates to existing products and supports adding new variants with images.

## API Endpoint

```
PUT /api/v1/products/{productId}
Content-Type: multipart/form-data
Authorization: Bearer {JWT_TOKEN}
```

## Authentication

- **Required Role**: ADMIN or EMPLOYEE
- **Header**: `Authorization: Bearer {JWT_TOKEN}`
- **Login**: Use the seeded admin user:
  - Email: `abayohirwajovin@gmail.com`
  - Password: `JOVIN19`

## Testing Scenarios

### 1. Basic Product Field Updates

#### Update Product Name and Description

```json
{
  "name": "AirPods Pro 2nd Generation",
  "description": "Updated description for the latest AirPods Pro model"
}
```

#### Update Pricing Information

```json
{
  "basePrice": "18.50",
  "salePrice": "22.00",
  "costPrice": "13.50"
}
```

#### Update Product Status

```json
{
  "isFeatured": true,
  "isOnSale": true,
  "salePercentage": 20
}
```

### 2. Product Detail Updates

#### Update Meta Information

```json
{
  "fullDescription": "Comprehensive description with technical specifications",
  "metaTitle": "AirPods Pro - Premium Wireless Earbuds | Updated Title",
  "metaDescription": "Updated meta description for better SEO",
  "metaKeywords": "airpods, wireless, earbuds, noise cancellation, apple, premium",
  "searchKeywords": "airpods pro wireless earbuds apple noise cancellation premium"
}
```

#### Update Physical Properties

```json
{
  "dimensionsCm": "5.6 x 4.7 x 2.3",
  "weightKg": "0.058"
}
```

### 3. Adding New Variants

#### Add New Color Variant

```json
{
  "newVariants": [
    {
      "variantSku": "AIRPODS-PRO-WHITE-LG",
      "price": "18.00",
      "stockQuantity": 12,
      "isActive": true,
      "attributes": [
        {
          "attributeValueId": 6
        },
        {
          "attributeValueId": 4
        }
      ]
    }
  ]
}
```

**Note**: `attributeValueId: 6` should be "White" color, `attributeValueId: 4` should be "LG" size.

#### Add Multiple Variants

```json
{
  "newVariants": [
    {
      "variantSku": "AIRPODS-PRO-SILVER-MD",
      "price": "17.50",
      "stockQuantity": 8,
      "isActive": true,
      "attributes": [
        {
          "attributeValueId": 5
        },
        {
          "attributeValueId": 3
        }
      ]
    },
    {
      "variantSku": "AIRPODS-PRO-GOLD-SM",
      "price": "18.50",
      "stockQuantity": 5,
      "isActive": true,
      "attributes": [
        {
          "attributeValueId": 6
        },
        {
          "attributeValueId": 2
        }
      ]
    }
  ]
}
```

### 4. Adding Variants with Images

#### Prepare Multipart Form Data

- **newVariantImages**: Upload image files
- **newVariantImageMetadata**: JSON array with image metadata

```json
{
  "newVariants": [
    {
      "variantSku": "AIRPODS-PRO-WHITE-LG",
      "price": "18.00",
      "stockQuantity": 12,
      "isActive": true,
      "attributes": [
        {
          "attributeValueId": 6
        },
        {
          "attributeValueId": 4
        }
      ]
    }
  ],
  "newVariantImageMetadata": [
    {
      "altText": "AirPods Pro White Large - Front View",
      "isPrimary": true,
      "sortOrder": 0,
      "variantIndex": 0
    },
    {
      "altText": "AirPods Pro White Large - Side View",
      "isPrimary": false,
      "sortOrder": 1,
      "variantIndex": 0
    }
  ]
}
```

**File Uploads**:

- Attach `newVariantImages` as multipart files
- Ensure `variantIndex` matches the variant position in the array

### 5. Complete Update Example

#### Full Product Update with New Variants

```json
{
  "name": "AirPods Pro 2nd Generation",
  "description": "Enhanced wireless earbuds with improved noise cancellation",
  "basePrice": "18.50",
  "isFeatured": true,
  "fullDescription": "Experience premium sound quality with these state-of-the-art wireless earbuds featuring advanced noise cancellation technology and enhanced battery life.",
  "metaTitle": "AirPods Pro 2nd Gen - Premium Wireless Earbuds",
  "newVariants": [
    {
      "variantSku": "AIRPODS-PRO-WHITE-LG",
      "price": "18.00",
      "stockQuantity": 12,
      "isActive": true,
      "attributes": [
        {
          "attributeValueId": 6
        },
        {
          "attributeValueId": 4
        }
      ]
    }
  ],
  "newVariantImageMetadata": [
    {
      "altText": "AirPods Pro White Large - Front View",
      "isPrimary": true,
      "sortOrder": 0,
      "variantIndex": 0
    }
  ]
}
```

## Error Handling Scenarios

### 1. Product Not Found

```http
PUT /api/v1/products/00000000-0000-0000-0000-000000000000
```

**Expected Response**: 404 Not Found

```json
{
  "success": false,
  "errorCode": "ENTITY_NOT_FOUND",
  "message": "Product not found with ID: 00000000-0000-0000-0000-000000000000",
  "timestamp": 1234567890
}
```

### 2. Invalid Category ID

```json
{
  "categoryId": 999
}
```

**Expected Response**: 400 Bad Request

```json
{
  "success": false,
  "errorCode": "ENTITY_NOT_FOUND",
  "message": "Category not found with ID: 999",
  "timestamp": 1234567890
}
```

### 3. Duplicate Variant SKU

```json
{
  "newVariants": [
    {
      "variantSku": "AIRPODS-PRO-BLUE-LG",
      "price": "16.00",
      "stockQuantity": 10
    }
  ]
}
```

**Expected Response**: 400 Bad Request

```json
{
  "success": false,
  "errorCode": "INVALID_ARGUMENT",
  "message": "Variant with SKU AIRPODS-PRO-BLUE-LG already exists",
  "timestamp": 1234567890
}
```

### 4. Invalid Brand ID

```json
{
  "brandId": "00000000-0000-0000-0000-000000000000"
}
```

**Expected Response**: 400 Bad Request

```json
{
  "success": false,
  "errorCode": "ENTITY_NOT_FOUND",
  "message": "Brand not found with ID: 00000000-0000-0000-0000-000000000000",
  "timestamp": 1234567890
}
```

## Postman Testing Setup

### 1. Environment Variables

```
BASE_URL: http://localhost:8095
JWT_TOKEN: {your_jwt_token_after_login}
```

### 2. Authentication Request

```
POST {{BASE_URL}}/api/v1/auth/login
Content-Type: application/json

{
  "email": "abayohirwajovin@gmail.com",
  "password": "JOVIN19"
}
```

### 3. Update Product Request

```
PUT {{BASE_URL}}/api/v1/products/{productId}
Authorization: Bearer {{JWT_TOKEN}}
Content-Type: multipart/form-data

Body (form-data):
- name: "AirPods Pro 2nd Generation"
- description: "Enhanced wireless earbuds"
- basePrice: "18.50"
- newVariants: [{"variantSku":"AIRPODS-PRO-WHITE-LG","price":"18.00","stockQuantity":12,"isActive":true,"attributes":[{"attributeValueId":6},{"attributeValueId":4}]}]
- newVariantImageMetadata: [{"altText":"White Large Front View","isPrimary":true,"sortOrder":0,"variantIndex":0}]
- newVariantImages: [file1.jpg, file2.jpg]
```

## Validation Rules

### 1. Required Fields

- None (all fields are optional for updates)

### 2. Field Constraints

- **name**: 2-255 characters (if provided)
- **basePrice**: Must be positive (if provided)
- **stockQuantity**: Must be non-negative (if provided)
- **SKU**: Must be unique across all variants

### 3. Business Rules

- Existing product images and videos remain unchanged
- New variants must have unique SKUs
- Attribute values must exist in the database
- Category, brand, and discount must be active if updated

## Success Response Format

```json
{
  "productId": "uuid-here",
  "name": "AirPods Pro 2nd Generation",
  "description": "Enhanced wireless earbuds with improved noise cancellation",
  "sku": "AIRPODS-PRO-001",
  "basePrice": "18.50",
  "stockQuantity": 30,
  "categoryId": 2,
  "categoryName": "Audio",
  "brandId": "brand-uuid-here",
  "brandName": "Apple",
  "isActive": true,
  "isFeatured": true,
  "variants": [
    {
      "variantId": "variant-uuid-here",
      "variantSku": "AIRPODS-PRO-WHITE-LG",
      "price": "18.00",
      "stockQuantity": 12,
      "isActive": true,
      "images": [
        {
          "imageId": "image-uuid-here",
          "url": "cloudinary-url-here",
          "altText": "White Large Front View",
          "isPrimary": true,
          "sortOrder": 0
        }
      ]
    }
  ]
}
```

## Testing Checklist

- [ ] Basic field updates (name, description, price)
- [ ] Product detail updates (meta fields, dimensions, weight)
- [ ] Category/brand/discount updates
- [ ] Adding single new variant
- [ ] Adding multiple new variants
- [ ] Adding variants with images
- [ ] Error handling for invalid IDs
- [ ] Error handling for duplicate SKUs
- [ ] Authentication and authorization
- [ ] Partial updates (only provided fields)
- [ ] Transaction rollback on errors

## Notes

1. **Partial Updates**: Only fields provided in the DTO will be updated
2. **Image Handling**: Existing product images remain unchanged
3. **Variant Images**: Only new variants can have images uploaded
4. **Transaction Safety**: All updates are wrapped in transactions
5. **Validation**: All foreign key references are validated before updates
6. **Performance**: Uses existing concurrent processing for image uploads
