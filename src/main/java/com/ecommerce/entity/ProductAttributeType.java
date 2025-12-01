package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_attribute_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"attributeValues"})
@ToString(exclude = {"attributeValues"})
public class ProductAttributeType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attributeTypeId;

    @NotBlank(message = "Attribute type name is required")
    @Size(min = 2, max = 50, message = "Attribute type name must be between 2 and 50 characters")
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "is_required")
    private boolean isRequired = false;

    @OneToMany(mappedBy = "attributeType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductAttributeValue> attributeValues = new ArrayList<>();
}