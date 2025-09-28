package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appeal_media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"returnAppeal"})
public class AppealMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Appeal ID is required")
    @Column(name = "appeal_id", nullable = false)
    private Long appealId;

    @NotBlank(message = "File URL is required")
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @NotNull(message = "Upload date is required")
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appeal_id", insertable = false, updatable = false)
    private ReturnAppeal returnAppeal;

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

    /**
     * Helper method to get media type for display purposes
     */
    public String getDisplayType() {
        return fileType.name().toLowerCase();
    }
}
