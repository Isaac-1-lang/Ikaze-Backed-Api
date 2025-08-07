package com.ecommerce.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        File convertedFile = convertMultiPartToFile(file);
        try {
            Map params = ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "public_id", generateUniquePublicId(file),
                    "overwrite", true
            );

            Map uploadResult = cloudinary.uploader().upload(convertedFile, params);
            return mapToStringMap(uploadResult);
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
        File convertedFile = File.createTempFile("upload", fileName != null ? fileName.substring(fileName.lastIndexOf(".")) : ".tmp");
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
}