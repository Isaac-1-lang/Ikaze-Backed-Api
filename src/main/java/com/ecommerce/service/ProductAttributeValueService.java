package com.ecommerce.service;

import com.ecommerce.dto.ProductAttributeValueDTO;
import com.ecommerce.dto.ProductAttributeValueRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductAttributeValueService {

    /**
     * Create a new product attribute value
     * 
     * @param requestDTO the attribute value data to create
     * @return the created attribute value
     */
    ProductAttributeValueDTO createAttributeValue(ProductAttributeValueRequestDTO requestDTO);

    /**
     * Update an existing product attribute value
     * 
     * @param id         the ID of the attribute value to update
     * @param requestDTO the updated attribute value data
     * @return the updated attribute value
     */
    ProductAttributeValueDTO updateAttributeValue(Long id, ProductAttributeValueRequestDTO requestDTO);

    /**
     * Get a product attribute value by its ID
     * 
     * @param id the ID of the attribute value
     * @return the attribute value
     */
    ProductAttributeValueDTO getAttributeValueById(Long id);

    /**
     * Get all product attribute values for a given attribute type
     * 
     * @param attributeTypeId the ID of the attribute type
     * @return a list of attribute values for the given type
     */
    List<ProductAttributeValueDTO> getAttributeValuesByTypeId(Long attributeTypeId);

    /**
     * Get all product attribute values for a given attribute type with pagination
     * 
     * @param attributeTypeId the ID of the attribute type
     * @param pageable        pagination information
     * @return a page of attribute values for the given type
     */
    Page<ProductAttributeValueDTO> getAttributeValuesByTypeId(Long attributeTypeId, Pageable pageable);

    /**
     * Get all product attribute values
     * 
     * @return a list of all attribute values
     */
    List<ProductAttributeValueDTO> getAllAttributeValues();

    /**
     * Get all product attribute values with pagination
     * 
     * @param pageable pagination information
     * @return a page of attribute values
     */
    Page<ProductAttributeValueDTO> getAllAttributeValues(Pageable pageable);

    /**
     * Search for product attribute values by value
     * 
     * @param value    the value to search for
     * @param pageable pagination information
     * @return a page of attribute values matching the search criteria
     */
    Page<ProductAttributeValueDTO> searchAttributeValuesByValue(String value, Pageable pageable);

    /**
     * Search for product attribute values by value and attribute type
     * 
     * @param value           the value to search for
     * @param attributeTypeId the ID of the attribute type
     * @param pageable        pagination information
     * @return a page of attribute values matching the search criteria
     */
    Page<ProductAttributeValueDTO> searchAttributeValuesByValueAndTypeId(String value, Long attributeTypeId,
            Pageable pageable);

    /**
     * Delete a product attribute value by its ID
     * 
     * @param id the ID of the attribute value to delete
     * @return true if the attribute value was deleted, false otherwise
     */
    boolean deleteAttributeValue(Long id);

    /**
     * Check if a product attribute value with the given value and attribute type
     * exists
     * 
     * @param value           the value of the attribute
     * @param attributeTypeId the ID of the attribute type
     * @return true if the attribute value exists, false otherwise
     */
    boolean attributeValueExists(String value, Long attributeTypeId);

    /**
     * Check if a product attribute value is in use
     * 
     * @param id the ID of the attribute value
     * @return true if the attribute value is in use, false otherwise
     */
    boolean isAttributeValueInUse(Long id);

    /**
     * Get a product attribute value by its value and attribute type ID
     * 
     * @param value           the value of the attribute
     * @param attributeTypeId the ID of the attribute type
     * @return the attribute value if found
     */
    ProductAttributeValueDTO getAttributeValueByValueAndTypeId(String value, Long attributeTypeId);
}