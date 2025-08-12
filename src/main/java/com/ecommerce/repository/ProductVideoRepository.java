package com.ecommerce.repository;

import com.ecommerce.entity.ProductVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVideoRepository extends JpaRepository<ProductVideo, Long> {

    List<ProductVideo> findByProductProductId(UUID productId);

    void deleteByProductProductId(UUID productId);
}
