package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "return_media")
@Data
@ToString(exclude = "returnRequest")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"returnRequest"})
public class ReturnMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Return request ID is required")
    @Column(name = "return_request_id", nullable = false)
    private Long returnRequestId;

    @NotBlank(message = "File URL is required")
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "public_id", length = 255)
    private String publicId; // Cloudinary public ID for file management

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize; // File size in bytes

    @Column(name = "width")
    private Integer width; // Image width in pixels

    @Column(name = "height")
    private Integer height; // Image height in pixels

    @NotNull(message = "Upload date is required")
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", insertable = false, updatable = false)
    private ReturnRequest returnRequest;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Enum for file types
     */
    public enum FileType {
        IMAGE,
        VIDEO
    }

    /**
     * Helper method to check if the media is an image
     */
    public boolean isImage() {
        return fileType == FileType.IMAGE;
    }

    /**
     * Helper method to check if the media is a video
     */
    public boolean isVideo() {
        return fileType == FileType.VIDEO;
    }

    /**
     * Helper method to get file extension from URL
     */
    public String getFileExtension() {
        if (fileUrl != null && fileUrl.contains(".")) {
            return fileUrl.substring(fileUrl.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
}
