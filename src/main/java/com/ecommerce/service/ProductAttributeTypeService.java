package com.ecommerce.service;

import com.ecommerce.dto.ProductAttributeTypeDTO;
import com.ecommerce.dto.ProductAttributeTypeRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductAttributeTypeService {

    /**
     * Create a new product attribute type
     * 
     * @param requestDTO the attribute type data to create
     * @return the created attribute type
     */
    ProductAttributeTypeDTO createAttributeType(ProductAttributeTypeRequestDTO requestDTO);

    /**
     * Update an existing product attribute type
     * 
     * @param id         the ID of the attribute type to update
     * @param requestDTO the updated attribute type data
     * @return the updated attribute type
     */
    ProductAttributeTypeDTO updateAttributeType(Long id, ProductAttributeTypeRequestDTO requestDTO);

    /**
     * Get a product attribute type by its ID
     * 
     * @param id the ID of the attribute type
     * @return the attribute type
     */
    ProductAttributeTypeDTO getAttributeTypeById(Long id);

    /**
     * Get a product attribute type by its name
     * 
     * @param name the name of the attribute type
     * @return the attribute type
     */
    ProductAttributeTypeDTO getAttributeTypeByName(String name);

    /**
     * Get all product attribute types
     * 
     * @return a list of all attribute types
     */
    List<ProductAttributeTypeDTO> getAllAttributeTypes();

    /**
     * Get all product attribute types with pagination
     * 
     * @param pageable pagination information
     * @return a page of attribute types
     */
    Page<ProductAttributeTypeDTO> getAllAttributeTypes(Pageable pageable);

    /**
     * Search for product attribute types by name
     * 
     * @param name     the name to search for
     * @param pageable pagination information
     * @return a page of attribute types matching the search criteria
     */
    Page<ProductAttributeTypeDTO> searchAttributeTypesByName(String name, Pageable pageable);

    /**
     * Delete a product attribute type by its ID
     * 
     * @param id the ID of the attribute type to delete
     * @return true if the attribute type was deleted, false otherwise
     */
    boolean deleteAttributeType(Long id);

    /**
     * Check if a product attribute type with the given name exists
     * 
     * @param name the name of the attribute type
     * @return true if the attribute type exists, false otherwise
     */
    boolean attributeTypeExists(String name);

    /**
     * Check if a product attribute type is in use
     * 
     * @param id the ID of the attribute type
     * @return true if the attribute type is in use, false otherwise
     */
    boolean isAttributeTypeInUse(Long id);
}