package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shops")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "shop_id", updatable = false, nullable = false)
    private UUID shopId;

    @NotBlank(message = "Shop name is required")
    @Size(min = 2, max = 100, message = "Shop name must be between 2 and 100 characters")
    @Column(name = "shop_name", nullable = false)
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "slug", unique = true, nullable = false)
    private String slug;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShopStatus status = ShopStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonBackReference
    private User owner;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlug(name);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    public boolean isActive() {
        return isActive != null ? isActive : true;
    }

    public enum ShopStatus {
        PENDING, ACTIVE, SUSPENDED, INACTIVE
    }
}

