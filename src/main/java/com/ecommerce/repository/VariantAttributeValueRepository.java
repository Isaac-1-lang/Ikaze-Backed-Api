package com.ecommerce.repository;

import com.ecommerce.entity.VariantAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VariantAttributeValueRepository
        extends JpaRepository<VariantAttributeValue, VariantAttributeValue.VariantAttributeValueId> {

    List<VariantAttributeValue> findByProductVariantId(Long variantId);

    void deleteByProductVariantId(Long variantId);
}
