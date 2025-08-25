# Complete Postman Testing Guide for Product Creation API

## Prerequisites

1. **Start the Application**: Ensure your Spring Boot application is running on `http://localhost:8095`
2. **Database Setup**: Make sure PostgreSQL is running and the database schema is created
3. **Authentication**: You'll need to authenticate first to get a JWT token (assuming you have admin/employee role)

## Step 1: Authentication (Get JWT Token)

### Login Request

```
POST http://localhost:8081/api/v1/auth/users/login
Content-Type: application/json

{
    "email": "admin@example.com",
    "password": "your_password"
}
```

**Save the JWT token from the response for subsequent requests.**

## Step 2: Create Prerequisites Data

### Create a Category

```
POST http://localhost:8081/api/v1/categories
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
    "name": "Electronics",
    "description": "Electronic devices and accessories",
    "isActive": true,
    "isFeatured": false
}
```

### Create a Brand (Optional)

```
POST http://localhost:8095/api/v1/brands
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
    "brandName": "Apple",
    "description": "Premium electronics brand",
    "isActive": true,
    "isFeatured": true
}
```

### Create Product Attribute Types and Values

```
POST http://localhost:8095/api/v1/product-attributes/types
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
    "name": "Color",
    "isRequired": false
}
```

```
POST http://localhost:8095/api/v1/product-attributes/values
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
    "value": "Blue",
    "attributeTypeId": 1
}
```

**Repeat for Red, Black, and Size attributes (LG, XSM, SM, MD, XLG)**

## Step 3: Create Product with Variants and Images

### Main Product Creation Request

**URL**: `POST http://localhost:8095/api/v1/products`

**Headers**:

- `Authorization: Bearer YOUR_JWT_TOKEN`
- `Content-Type: multipart/form-data`

### Postman Configuration

1. **Open Postman**
2. **Create a new POST request**
3. **Set the URL**: `http://localhost:8095/api/v1/products`
4. **Go to Headers tab**:
   - Add `Authorization: Bearer YOUR_JWT_TOKEN`
5. **Go to Body tab**:
   - Select `form-data`
   - Configure fields as shown below:

### Form Data Fields Configuration

#### Basic Product Information

| Key             | Type | Value                                                     |
| --------------- | ---- | --------------------------------------------------------- |
| `name`          | Text | `AirPods Pro`                                             |
| `description`   | Text | `Premium wireless earbuds with active noise cancellation` |
| `sku`           | Text | `AIRPODS-PRO-001`                                         |
| `basePrice`     | Text | `17.00`                                                   |
| `stockQuantity` | Text | `30`                                                      |
| `categoryId`    | Text | `1`                                                       |
| `brandId`       | Text | `YOUR_BRAND_UUID`                                         |
| `isActive`      | Text | `true`                                                    |
| `isFeatured`    | Text | `true`                                                    |
| `isNewArrival`  | Text | `true`                                                    |

#### Product Detail Information

| Key               | Type | Value                                                                                                                             |
| ----------------- | ---- | --------------------------------------------------------------------------------------------------------------------------------- |
| `fullDescription` | Text | `Experience premium sound quality with these state-of-the-art wireless earbuds featuring advanced noise cancellation technology.` |
| `metaTitle`       | Text | `AirPods Pro - Premium Wireless Earbuds`                                                                                          |
| `metaDescription` | Text | `Buy AirPods Pro with noise cancellation. Available in multiple colors and sizes.`                                                |
| `metaKeywords`    | Text | `airpods, wireless, earbuds, noise cancellation, apple`                                                                           |
| `dimensionsCm`    | Text | `5.4 x 4.5 x 2.1`                                                                                                                 |
| `weightKg`        | Text | `0.056`                                                                                                                           |

#### Product Images

| Key             | Type | Value                                    |
| --------------- | ---- | ---------------------------------------- |
| `productImages` | File | `[Select your main product image files]` |

#### Image Metadata (JSON Arrays as Text)

| Key             | Type | Value                                                                                                                                                 |
| --------------- | ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `imageMetadata` | Text | `[{"altText": "AirPods Pro main view", "isPrimary": true, "sortOrder": 0}, {"altText": "AirPods Pro side view", "isPrimary": false, "sortOrder": 1}]` |

#### Product Videos (Optional)

| Key             | Type | Value                                                     |
| --------------- | ---- | --------------------------------------------------------- |
| `productVideos` | File | `[Select your product video files - max 30 seconds each]` |

#### Video Metadata (JSON Arrays as Text)

| Key             | Type | Value                                                                                                                  |
| --------------- | ---- | ---------------------------------------------------------------------------------------------------------------------- |
| `videoMetadata` | Text | `[{"title": "AirPods Pro Demo", "description": "Product demonstration video", "sortOrder": 0, "durationSeconds": 25}]` |

#### Product Variants (JSON Array as Text)

| Key        | Type | Value                   |
| ---------- | ---- | ----------------------- |
| `variants` | Text | See detailed JSON below |

### Detailed Variants JSON Structure

```json
[
  {
    "variantSku": "AIRPODS-PRO-BLUE-LG",
    "price": 16.0,
    "stockQuantity": 14,
    "isActive": true,
    "sortOrder": 0,
    "attributes": [
      { "attributeValueId": 1 }, // Blue color
      { "attributeValueId": 5 } // Large size
    ],
    "imageMetadata": [
      {
        "altText": "Blue AirPods Pro - Large",
        "isPrimary": true,
        "sortOrder": 0
      }
    ]
  },
  {
    "variantSku": "AIRPODS-PRO-BLUE-SM",
    "price": 16.0,
    "stockQuantity": 5,
    "isActive": true,
    "sortOrder": 1,
    "attributes": [
      { "attributeValueId": 1 }, // Blue color
      { "attributeValueId": 6 } // Small size
    ],
    "imageMetadata": [
      {
        "altText": "Blue AirPods Pro - Small",
        "isPrimary": true,
        "sortOrder": 0
      }
    ]
  },
  {
    "variantSku": "AIRPODS-PRO-RED-LG",
    "price": 16.0,
    "stockQuantity": 15,
    "isActive": true,
    "sortOrder": 2,
    "attributes": [
      { "attributeValueId": 2 }, // Red color
      { "attributeValueId": 5 } // Large size
    ],
    "imageMetadata": [
      {
        "altText": "Red AirPods Pro - Large",
        "isPrimary": true,
        "sortOrder": 0
      }
    ]
  },
  {
    "variantSku": "AIRPODS-PRO-BLACK-MD",
    "price": 17.0,
    "stockQuantity": 1,
    "isActive": true,
    "sortOrder": 3,
    "attributes": [
      { "attributeValueId": 3 }, // Black color
      { "attributeValueId": 7 } // Medium size
    ],
    "imageMetadata": [
      {
        "altText": "Black AirPods Pro - Medium",
        "isPrimary": true,
        "sortOrder": 0
      }
    ]
  }
]
```

### Variant Images Upload

**Important**: For each variant, you need to upload specific images. Add these fields for variant images:

| Key                         | Type | Value                         |
| --------------------------- | ---- | ----------------------------- |
| `variants[0].variantImages` | File | `[Blue AirPods images]`       |
| `variants[1].variantImages` | File | `[Blue AirPods small images]` |
| `variants[2].variantImages` | File | `[Red AirPods images]`        |
| `variants[3].variantImages` | File | `[Black AirPods images]`      |

### Complete Postman Form-Data Setup

Here's the step-by-step process in Postman:

1. **Set Request Type**: POST
2. **Enter URL**: `http://localhost:8095/api/v1/products`
3. **Add Authorization Header**:
   - Go to "Headers" tab
   - Add: `Authorization: Bearer YOUR_JWT_TOKEN`
4. **Configure Body**:
   - Go to "Body" tab
   - Select "form-data"
   - Add all the fields from the tables above

### File Upload Guidelines

#### For Images:

- **Supported formats**: JPG, PNG, GIF, WebP
- **Maximum size**: 10MB per image
- **Recommended resolution**: 1024x1024 or higher
- **Image metadata will be automatically extracted**: width, height, file size, MIME type

#### For Videos:

- **Supported formats**: MP4, AVI, MOV, WebM
- **Maximum size**: 50MB per video
- **Maximum duration**: 30 seconds
- **The system will validate duration if provided in metadata**

### Sample Response

```json
{
  "productId": "123e4567-e89b-12d3-a456-426614174000",
  "name": "AirPods Pro",
  "description": "Premium wireless earbuds with active noise cancellation",
  "sku": "AIRPODS-PRO-001",
  "basePrice": 17.0,
  "salePrice": null,
  "discountedPrice": 17.0,
  "stockQuantity": 30,
  "categoryId": 1,
  "categoryName": "Electronics",
  "brandId": "brand-uuid-here",
  "brandName": "Apple",
  "slug": "airpods-pro",
  "isActive": true,
  "isFeatured": true,
  "isNewArrival": true,
  "averageRating": 0.0,
  "reviewCount": 0,
  "images": [
    {
      "imageId": 1,
      "url": "https://res.cloudinary.com/your-cloud/image/upload/v1234567890/product-image.jpg",
      "altText": "AirPods Pro main view",
      "isPrimary": true,
      "sortOrder": 0
    }
  ],
  "videos": [
    {
      "videoId": 1,
      "url": "https://res.cloudinary.com/your-cloud/video/upload/v1234567890/product-video.mp4",
      "title": "AirPods Pro Demo",
      "description": "Product demonstration video",
      "sortOrder": 0
    }
  ],
  "variants": [
    {
      "variantId": 1,
      "variantSku": "AIRPODS-PRO-BLUE-LG",
      "variantName": "Blue - Large",
      "price": 16.0,
      "stockQuantity": 14,
      "isActive": true,
      "isInStock": true,
      "isLowStock": false,
      "images": [
        {
          "imageId": 2,
          "url": "https://res.cloudinary.com/your-cloud/image/upload/v1234567890/variant-image.jpg",
          "altText": "Blue AirPods Pro - Large",
          "isPrimary": true,
          "sortOrder": 0
        }
      ],
      "attributes": [
        {
          "attributeValueId": 1,
          "attributeValue": "Blue",
          "attributeTypeId": 1,
          "attributeType": "Color"
        },
        {
          "attributeValueId": 5,
          "attributeValue": "Large",
          "attributeTypeId": 2,
          "attributeType": "Size"
        }
      ]
    }
  ],
  "createdAt": "2025-08-08T12:00:00",
  "updatedAt": "2025-08-08T12:00:00"
}
```

## Error Handling

### Common Errors and Solutions

1. **401 Unauthorized**: Check your JWT token and ensure it's valid
2. **403 Forbidden**: Ensure your user has ADMIN or EMPLOYEE role
3. **400 Bad Request**: Check required fields and data validation
4. **413 Payload Too Large**: Reduce file sizes (images < 10MB, videos < 50MB)
5. **Video duration error**: Ensure videos are <= 30 seconds
6. **SKU already exists**: Use unique SKU values for products and variants

### File Upload Validation Errors

- **"Image file size exceeds maximum allowed (10MB)"**: Compress your images
- **"Video file size exceeds maximum allowed (50MB)"**: Compress your videos
- **"Video duration exceeds maximum allowed (30 seconds)"**: Trim your videos
- **"File must be a video format"**: Ensure correct MIME type for videos

## Testing Tips

1. **Start Small**: Test with 1-2 variants first, then expand
2. **Use Small Files**: For initial testing, use smaller image/video files
3. **Check Logs**: Monitor application logs for detailed error messages
4. **Test Incrementally**: Test basic product creation first, then add variants and media
5. **Validate Attribute IDs**: Ensure attribute value IDs exist in your database

## Advanced Testing Scenarios

### Test Case 1: Product with Multiple Variants and Images

- Upload a product with 3 color variants
- Each variant has 2-3 images
- Include product-level images and videos

### Test Case 2: Large Product with All Features

- Include all optional fields
- Upload maximum number of images/videos
- Test with different file formats

### Test Case 3: Error Scenarios

- Try uploading oversized files
- Try uploading invalid file formats
- Test with missing required fields
- Test with invalid attribute IDs

This comprehensive guide should help you test the complete product creation functionality with all the enhancements we've implemented!
