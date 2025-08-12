package com.ecommerce.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface CloudinaryService {

    /**
     * Upload a single image to Cloudinary
     * 
     * @param file The image file to upload
     * @return Map containing upload results including URL
     * @throws IOException If an error occurs during upload
     */
    Map<String, String> uploadImage(MultipartFile file) throws IOException;

    /**
     * Upload multiple images to Cloudinary concurrently
     * 
     * @param files List of image files to upload
     * @return List of maps containing upload results
     */
    List<Map<String, String>> uploadMultipleImages(List<MultipartFile> files);

    /**
     * Upload a single video to Cloudinary
     * 
     * @param file The video file to upload
     * @return Map containing upload results including URL
     * @throws IOException If an error occurs during upload
     */
    Map<String, String> uploadVideo(MultipartFile file) throws IOException;

    /**
     * Upload multiple videos to Cloudinary concurrently
     * 
     * @param files List of video files to upload
     * @return List of maps containing upload results
     */
    List<Map<String, String>> uploadMultipleVideos(List<MultipartFile> files);

    /**
     * Delete a file from Cloudinary by its public ID
     * 
     * @param publicId The public ID of the file to delete
     * @return Map containing deletion results
     * @throws IOException If an error occurs during deletion
     */
    Map<String, String> deleteFile(String publicId) throws IOException;

    /**
     * Get a signed URL for a file with an expiration time
     * 
     * @param publicId                The public ID of the file
     * @param expirationTimeInSeconds Time in seconds until URL expires
     * @return The signed URL
     */
    String getSignedUrl(String publicId, int expirationTimeInSeconds);

    /**
     * Create a thumbnail of an image
     * 
     * @param publicId The public ID of the image
     * @param width    The width of the thumbnail
     * @param height   The height of the thumbnail
     * @return URL of the generated thumbnail
     */
    String createThumbnail(String publicId, int width, int height);

    /**
     * Delete an image from Cloudinary by its URL
     * 
     * @param imageUrl The URL of the image to delete
     * @return Map containing deletion results
     * @throws IOException If an error occurs during deletion
     */
    Map<String, String> deleteImage(String imageUrl) throws IOException;
}