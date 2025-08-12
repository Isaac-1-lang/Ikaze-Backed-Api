package com.ecommerce.repository;

import com.ecommerce.entity.ProductVariantImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantImageRepository extends JpaRepository<ProductVariantImage, Long> {

    List<ProductVariantImage> findByProductVariantId(Long variantId);

    void deleteByProductVariantId(Long variantId);
}
