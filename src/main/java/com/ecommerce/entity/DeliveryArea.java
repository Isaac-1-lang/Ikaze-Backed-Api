package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_areas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_area_id")
    private Long deliveryAreaId;

    @NotBlank(message = "Delivery area name is required")
    @Size(min = 2, max = 100, message = "Delivery area name must be between 2 and 100 characters")
    @Column(name = "delivery_area_name", nullable = false, unique = true)
    private String deliveryAreaName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DeliveryArea parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DeliveryArea> children = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Checks if this delivery area is a top-level area (has no parent)
     * 
     * @return true if this is a top-level area, false otherwise
     */
    public boolean isTopLevel() {
        return parent == null;
    }

    /**
     * Checks if this delivery area is a leaf area (has no children)
     * 
     * @return true if this is a leaf area, false otherwise
     */
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    /**
     * Gets the level of this delivery area in the hierarchy
     * 
     * @return 0 for top-level areas, 1 for their children, etc.
     */
    public int getLevel() {
        if (isTopLevel()) {
            return 0;
        }
        return parent.getLevel() + 1;
    }

    /**
     * Gets all children of this delivery area, including children of children
     * 
     * @return a list of all descendant delivery areas
     */
    public List<DeliveryArea> getAllChildren() {
        List<DeliveryArea> allChildren = new ArrayList<>();
        for (DeliveryArea child : children) {
            allChildren.add(child);
            allChildren.addAll(child.getAllChildren());
        }
        return allChildren;
    }
}