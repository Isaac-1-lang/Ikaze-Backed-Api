package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "variant_attribute_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantAttributeValue {

    @EmbeddedId
    private VariantAttributeValueId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("variantId")
    @JoinColumn(name = "variant_id")
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attributeValueId")
    @JoinColumn(name = "attribute_value_id")
    private ProductAttributeValue attributeValue;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantAttributeValueId implements Serializable {

        @Column(name = "variant_id")
        private Long variantId;

        @Column(name = "attribute_value_id")
        private Long attributeValueId;
    }
}