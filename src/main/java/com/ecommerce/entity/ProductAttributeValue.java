package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product_attribute_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "variantAttributeValues", "attributeType" })
@ToString(exclude = { "variantAttributeValues", "attributeType" })
public class ProductAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attributeValueId;

    @NotBlank(message = "Value is required")
    @Column(name = "value", nullable = false)
    private String value;

    @NotNull(message = "Attribute type is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_type_id", nullable = false)
    private ProductAttributeType attributeType;

    @OneToMany(mappedBy = "attributeValue")
    private Set<VariantAttributeValue> variantAttributeValues = new HashSet<>();

    /**
     * Gets the value
     * 
     * @return The value
     */
    public String getValue() {
        return value;
    }
}