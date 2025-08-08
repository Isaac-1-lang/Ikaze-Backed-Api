# Cloudinary Service Implementation

## Overview
The Cloudinary Service provides functionality for uploading and managing media files (images and videos) using the Cloudinary cloud service. It supports both single and concurrent multiple file uploads, optimized for performance.

## Features

- **Single Image Upload**: Upload individual image files to Cloudinary
- **Concurrent Multiple Image Upload**: Upload multiple images simultaneously using Java's CompletableFuture for improved performance
- **Single Video Upload**: Upload individual video files to Cloudinary
- **Concurrent Multiple Video Upload**: Upload multiple videos simultaneously using Java's CompletableFuture
- **File Deletion**: Remove files from Cloudinary storage
- **Thumbnail Generation**: Create thumbnails of images with specified dimensions
- **Signed URL Generation**: Generate secure, time-limited URLs for accessing media

## Configuration

The service requires the following configuration in `application.properties`:

```properties
# Cloudinary Configuration
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret

# File Upload Configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

## API Endpoints

### Image Upload
- `POST /api/v1/media/upload/image`: Upload a single image
- `POST /api/v1/media/upload/images`: Upload multiple images concurrently

### Video Upload
- `POST /api/v1/media/upload/video`: Upload a single video
- `POST /api/v1/media/upload/videos`: Upload multiple videos concurrently

### File Management
- `DELETE /api/v1/media/delete/{publicId}`: Delete a file from Cloudinary
- `GET /api/v1/media/thumbnail/{publicId}`: Generate a thumbnail with specified dimensions

## Security

All endpoints are secured with role-based access control. Only users with `ADMIN` or `EMPLOYEE` roles can upload or delete files.

## Implementation Details

### Concurrency
The service uses Java's CompletableFuture and a thread pool to handle concurrent uploads, significantly improving performance when uploading multiple files.

### Error Handling
Comprehensive error handling is implemented with custom exceptions and appropriate HTTP status codes.

### File Validation
Files are validated for type (image/video) and size before upload.

## Usage Example

```java
// Inject the service
@Autowired
private CloudinaryService cloudinaryService;

// Upload a single image
Map<String, String> result = cloudinaryService.uploadImage(imageFile);
String imageUrl = result.get("secure_url");

// Upload multiple images concurrently
List<Map<String, String>> results = cloudinaryService.uploadMultipleImages(imageFiles);
```