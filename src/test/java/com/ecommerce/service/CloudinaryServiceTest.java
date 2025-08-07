package com.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.ecommerce.service.impl.CloudinaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    private MultipartFile mockImageFile;
    private MultipartFile mockVideoFile;

    @BeforeEach
    void setUp() {
        mockImageFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockVideoFile = new MockMultipartFile(
                "video",
                "test-video.mp4",
                "video/mp4",
                "test video content".getBytes()
        );

        when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void uploadImage_shouldReturnUploadResult() throws IOException {
        // Arrange
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("public_id", "test_public_id");
        mockResult.put("secure_url", "https://test-url.com/image.jpg");
        mockResult.put("resource_type", "image");

        when(uploader.upload(any(), anyMap())).thenReturn(mockResult);

        // Act
        Map<String, String> result = cloudinaryService.uploadImage(mockImageFile);

        // Assert
        assertNotNull(result);
        assertEquals("test_public_id", result.get("public_id"));
        assertEquals("https://test-url.com/image.jpg", result.get("secure_url"));
        assertEquals("image", result.get("resource_type"));
    }

    @Test
    void uploadVideo_shouldReturnUploadResult() throws IOException {
        // Arrange
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("public_id", "test_video_id");
        mockResult.put("secure_url", "https://test-url.com/video.mp4");
        mockResult.put("resource_type", "video");

        when(uploader.upload(any(), anyMap())).thenReturn(mockResult);

        // Act
        Map<String, String> result = cloudinaryService.uploadVideo(mockVideoFile);

        // Assert
        assertNotNull(result);
        assertEquals("test_video_id", result.get("public_id"));
        assertEquals("https://test-url.com/video.mp4", result.get("secure_url"));
        assertEquals("video", result.get("resource_type"));
    }

    @Test
    void uploadMultipleImages_shouldReturnListOfResults() throws IOException {
        // Arrange
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("public_id", "test_public_id");
        mockResult.put("secure_url", "https://test-url.com/image.jpg");
        mockResult.put("resource_type", "image");

        when(uploader.upload(any(), anyMap())).thenReturn(mockResult);

        // Act
        List<Map<String, String>> results = cloudinaryService.uploadMultipleImages(List.of(mockImageFile, mockImageFile));

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("test_public_id", results.get(0).get("public_id"));
        assertEquals("https://test-url.com/image.jpg", results.get(0).get("secure_url"));
    }

    @Test
    void deleteFile_shouldReturnDeletionResult() throws IOException {
        // Arrange
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("result", "ok");

        when(uploader.destroy(any(), anyMap())).thenReturn(mockResult);

        // Act
        Map<String, String> result = cloudinaryService.deleteFile("test_public_id");

        // Assert
        assertNotNull(result);
        assertEquals("test_public_id", result.get("publicId"));
        assertEquals("ok", result.get("result"));
    }
}