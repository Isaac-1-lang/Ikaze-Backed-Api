package com.ecommerce.repository;

import com.ecommerce.entity.ProductAttributeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductAttributeTypeRepository extends JpaRepository<ProductAttributeType, Long> {

        /**
         * Find a product attribute type by its name
         * 
         * @param name the name of the attribute type
         * @return the attribute type if found
         */
        Optional<ProductAttributeType> findByNameIgnoreCase(String name);

        /**
         * Check if a product attribute type with the given name exists
         * 
         * @param name the name of the attribute type
         * @return true if the attribute type exists, false otherwise
         */
        boolean existsByNameIgnoreCase(String name);

        /**
         * Find product attribute types by name containing the given string (case
         * insensitive)
         * 
         * @param name     the name to search for
         * @param pageable pagination information
         * @return a page of attribute types matching the search criteria
         */
        Page<ProductAttributeType> findByNameContainingIgnoreCase(String name, Pageable pageable);

        /**
         * Check if an attribute type is used by any product variant
         * 
         * @param attributeTypeId the ID of the attribute type
         * @return true if the attribute type is used, false otherwise
         */
        @Query("SELECT CASE WHEN COUNT(vav) > 0 THEN true ELSE false END FROM VariantAttributeValue vav " +
                        "JOIN vav.attributeValue av WHERE av.attributeType.attributeTypeId = :attributeTypeId")
        boolean isAttributeTypeInUse(@Param("attributeTypeId") Long attributeTypeId);
}