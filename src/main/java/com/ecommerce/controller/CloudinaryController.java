package com.ecommerce.controller;

import com.ecommerce.dto.FileUploadResponseDTO;
import com.ecommerce.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Upload API", description = "API for uploading and managing media files")
@SecurityRequirement(name = "bearerAuth")
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
            summary = "Upload a single image",
            description = "Upload a single image file to Cloudinary",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Image uploaded successfully",
                            content = @Content(schema = @Schema(implementation = FileUploadResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid file format or size"),
                    @ApiResponse(responseCode = "500", description = "Server error during upload")
            }
    )
    public ResponseEntity<FileUploadResponseDTO> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            validateImageFile(file);
            Map<String, String> result = cloudinaryService.uploadImage(file);
            return ResponseEntity.ok(mapToResponseDTO(result));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    FileUploadResponseDTO.builder()
                            .error("Failed to upload image: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping(value = "/upload/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
            summary = "Upload multiple images",
            description = "Upload multiple image files to Cloudinary concurrently",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Images uploaded successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid file format or size"),
                    @ApiResponse(responseCode = "500", description = "Server error during upload")
            }
    )
    public ResponseEntity<List<FileUploadResponseDTO>> uploadMultipleImages(@RequestParam("files") List<MultipartFile> files) {
        for (MultipartFile file : files) {
            try {
                validateImageFile(file);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                        List.of(FileUploadResponseDTO.builder()
                                .error(e.getMessage())
                                .build())
                );
            }
        }

        List<Map<String, String>> results = cloudinaryService.uploadMultipleImages(files);
        List<FileUploadResponseDTO> responseDTOs = results.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDTOs);
    }

    @PostMapping(value = "/upload/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
            summary = "Upload a single video",
            description = "Upload a single video file to Cloudinary",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Video uploaded successfully",
                            content = @Content(schema = @Schema(implementation = FileUploadResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid file format or size"),
                    @ApiResponse(responseCode = "500", description = "Server error during upload")
            }
    )
    public ResponseEntity<FileUploadResponseDTO> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            validateVideoFile(file);
            Map<String, String> result = cloudinaryService.uploadVideo(file);
            return ResponseEntity.ok(mapToResponseDTO(result));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    FileUploadResponseDTO.builder()
                            .error("Failed to upload video: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping(value = "/upload/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
            summary = "Upload multiple videos",
            description = "Upload multiple video files to Cloudinary concurrently",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Videos uploaded successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid file format or size"),
                    @ApiResponse(responseCode = "500", description = "Server error during upload")
            }
    )
    public ResponseEntity<List<FileUploadResponseDTO>> uploadMultipleVideos(@RequestParam("files") List<MultipartFile> files) {
        for (MultipartFile file : files) {
            try {
                validateVideoFile(file);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                        List.of(FileUploadResponseDTO.builder()
                                .error(e.getMessage())
                                .build())
                );
            }
        }

        List<Map<String, String>> results = cloudinaryService.uploadMultipleVideos(files);
        List<FileUploadResponseDTO> responseDTOs = results.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDTOs);
    }

    @DeleteMapping("/delete/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
            summary = "Delete a file",
            description = "Delete a file from Cloudinary by its public ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid public ID"),
                    @ApiResponse(responseCode = "500", description = "Server error during deletion")
            }
    )
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String publicId) {
        try {
            Map<String, String> result = cloudinaryService.deleteFile(publicId);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            Map<String, String> errorResponse = Map.of(
                    "error", "Failed to delete file: " + e.getMessage(),
                    "publicId", publicId
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/thumbnail/{publicId}")
    @Operation(
            summary = "Create a thumbnail",
            description = "Create a thumbnail of an image with specified dimensions",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Thumbnail URL generated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid parameters")
            }
    )
    public ResponseEntity<Map<String, String>> createThumbnail(
            @PathVariable String publicId,
            @RequestParam(defaultValue = "200") int width,
            @RequestParam(defaultValue = "200") int height) {

        String thumbnailUrl = cloudinaryService.createThumbnail(publicId, width, height);
        return ResponseEntity.ok(Map.of("url", thumbnailUrl));
    }

    private FileUploadResponseDTO mapToResponseDTO(Map<String, String> result) {
        if (result.containsKey("error")) {
            return FileUploadResponseDTO.builder()
                    .error(result.get("error"))
                    .errorMessage(result.get("errorMessage"))
                    .build();
        }

        return FileUploadResponseDTO.builder()
                .publicId(result.get("public_id"))
                .url(result.get("url"))
                .secureUrl(result.get("secure_url"))
                .format(result.get("format"))
                .resourceType(result.get("resource_type"))
                .size(result.get("bytes") != null ? Long.parseLong(result.get("bytes")) : 0)
                .build();
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
    }

    private void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("File must be a video");
        }
    }
}