package com.ecommerce.repository;

import com.ecommerce.entity.ProductAttributeType;
import com.ecommerce.entity.ProductAttributeValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {

        /**
         * Find a product attribute value by its value
         * 
         * @param value the value of the attribute
         * @return the attribute value if found
         */
        Optional<ProductAttributeValue> findByValue(String value);

        /**
         * Find a product attribute value by its value and attribute type
         * 
         * @param value         the value of the attribute
         * @param attributeType the attribute type
         * @return the attribute value if found
         */
        Optional<ProductAttributeValue> findByValueIgnoreCaseAndAttributeType(String value,
                        ProductAttributeType attributeType);

        /**
         * Check if a product attribute value with the given value and attribute type
         * exists
         * 
         * @param value         the value of the attribute
         * @param attributeType the attribute type
         * @return true if the attribute value exists, false otherwise
         */
        boolean existsByValueIgnoreCaseAndAttributeType(String value, ProductAttributeType attributeType);

        /**
         * Find all product attribute values for a given attribute type
         * 
         * @param attributeType the attribute type
         * @return a list of attribute values for the given type
         */
        List<ProductAttributeValue> findByAttributeType(ProductAttributeType attributeType);

        /**
         * Find all product attribute values for a given attribute type with pagination
         * 
         * @param attributeType the attribute type
         * @param pageable      pagination information
         * @return a page of attribute values for the given type
         */
        Page<ProductAttributeValue> findByAttributeType(ProductAttributeType attributeType, Pageable pageable);

        /**
         * Find product attribute values by value containing the given string (case
         * insensitive)
         * 
         * @param value    the value to search for
         * @param pageable pagination information
         * @return a page of attribute values matching the search criteria
         */
        Page<ProductAttributeValue> findByValueContainingIgnoreCase(String value, Pageable pageable);

        /**
         * Find product attribute values by value containing the given string and
         * attribute type (case insensitive)
         * 
         * @param value         the value to search for
         * @param attributeType the attribute type
         * @param pageable      pagination information
         * @return a page of attribute values matching the search criteria
         */
        Page<ProductAttributeValue> findByValueContainingIgnoreCaseAndAttributeType(String value,
                        ProductAttributeType attributeType, Pageable pageable);

        /**
         * Check if an attribute value is used by any product variant
         * 
         * @param attributeValueId the ID of the attribute value
         * @return true if the attribute value is used, false otherwise
         */
        @Query("SELECT CASE WHEN COUNT(vav) > 0 THEN true ELSE false END FROM VariantAttributeValue vav " +
                        "WHERE vav.attributeValue.attributeValueId = :attributeValueId")
        boolean isAttributeValueInUse(@Param("attributeValueId") Long attributeValueId);
}