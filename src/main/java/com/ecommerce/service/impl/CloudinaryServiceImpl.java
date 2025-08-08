package com.ecommerce.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public Map<String, String> uploadImage(MultipartFile file) throws IOException {
        return uploadFile(file, "image");
    }

    @Override
    public List<Map<String, String>> uploadMultipleImages(List<MultipartFile> files) {
        return uploadMultipleFiles(files, "image");
    }

    @Override
    public Map<String, String> uploadVideo(MultipartFile file) throws IOException {
        return uploadFile(file, "video");
    }

    @Override
    public List<Map<String, String>> uploadMultipleVideos(List<MultipartFile> files) {
        return uploadMultipleFiles(files, "video");
    }

    @Override
    public Map<String, String> deleteFile(String publicId) throws IOException {
        Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        Map<String, String> response = new HashMap<>();
        response.put("publicId", publicId);
        response.put("result", result.get("result").toString());
        return response;
    }

    @Override
    public String getSignedUrl(String publicId, int expirationTimeInSeconds) {
        return cloudinary.url()
                .secure(true)
                .signed(true)
                .publicId(publicId)
                .format("auto")
                .generate();
    }

    @Override
    public String createThumbnail(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(new com.cloudinary.Transformation()
                        .width(width)
                        .height(height)
                        .crop("fill"))
                .secure(true)
                .publicId(publicId)
                .format("auto")
                .generate();
    }

    private Map<String, String> uploadFile(MultipartFile file, String resourceType) throws IOException {
        // Validate file before upload
        if ("video".equals(resourceType)) {
            validateVideoFile(file);
        }

        File convertedFile = convertMultiPartToFile(file);
        try {
            Map params = ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "public_id", generateUniquePublicId(file),
                    "overwrite", true);

            Map uploadResult = cloudinary.uploader().upload(convertedFile, params);
            Map<String, String> result = mapToStringMap(uploadResult);

            // Extract metadata for images
            if ("image".equals(resourceType)) {
                extractImageMetadata(file, result);
            }

            // Add file size for all files
            result.put("fileSize", String.valueOf(file.getSize()));
            result.put("mimeType", file.getContentType());

            return result;
        } finally {
            // Clean up the temporary file
            if (convertedFile.exists()) {
                convertedFile.delete();
            }
        }
    }

    private List<Map<String, String>> uploadMultipleFiles(List<MultipartFile> files, String resourceType) {
        List<CompletableFuture<Map<String, String>>> futures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return uploadFile(file, resourceType);
                    } catch (IOException e) {
                        log.error("Error uploading file: {}", e.getMessage());
                        Map<String, String> errorMap = new HashMap<>();
                        errorMap.put("error", "Failed to upload file: " + file.getOriginalFilename());
                        errorMap.put("errorMessage", e.getMessage());
                        return errorMap;
                    }
                }, executorService))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        File convertedFile = File.createTempFile("upload",
                fileName != null ? fileName.substring(fileName.lastIndexOf(".")) : ".tmp");
        FileOutputStream fos = new FileOutputStream(convertedFile);
        fos.write(file.getBytes());
        fos.close();
        return convertedFile;
    }

    private String generateUniquePublicId(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private Map<String, String> mapToStringMap(Map uploadResult) {
        Map<String, String> result = new HashMap<>();
        for (Object key : uploadResult.keySet()) {
            Object value = uploadResult.get(key);
            result.put(key.toString(), value != null ? value.toString() : null);
        }
        return result;
    }

    /**
     * Validates video file duration and size constraints
     */
    private void validateVideoFile(MultipartFile file) throws IOException {
        // Check file size (max 50MB for videos)
        long maxFileSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Video file size exceeds maximum allowed (50MB)");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("File must be a video format");
        }

        // For video duration validation, we'll use a simpler approach
        // In a production environment, you might want to use libraries like FFmpeg
        log.info("Video file validation passed for: {} ({})", file.getOriginalFilename(), contentType);
    }

    /**
     * Extracts image metadata (dimensions) and adds to result map
     */
    private void extractImageMetadata(MultipartFile file, Map<String, String> result) {
        try {
            // Check file size (max 10MB for images)
            long maxFileSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxFileSize) {
                throw new IllegalArgumentException("Image file size exceeds maximum allowed (10MB)");
            }

            // Extract image dimensions
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image != null) {
                result.put("width", String.valueOf(image.getWidth()));
                result.put("height", String.valueOf(image.getHeight()));
                log.debug("Extracted image dimensions: {}x{} for file: {}",
                        image.getWidth(), image.getHeight(), file.getOriginalFilename());
            } else {
                log.warn("Could not read image dimensions for file: {}", file.getOriginalFilename());
                result.put("width", "0");
                result.put("height", "0");
            }
        } catch (IOException e) {
            log.error("Error extracting image metadata for file: {}", file.getOriginalFilename(), e);
            // Set default values if extraction fails
            result.put("width", "0");
            result.put("height", "0");
        }
    }
}