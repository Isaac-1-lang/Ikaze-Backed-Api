package com.ecommerce.service;

import com.ecommerce.dto.CategoryDTO;
import com.ecommerce.dto.CategorySearchDTO;
import com.ecommerce.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {

    /**
     * Create a new category
     * 
     * @param categoryDTO the category data
     * @return the created category
     */
    CategoryDTO createCategory(CategoryDTO categoryDTO);

    /**
     * Update an existing category
     * 
     * @param id          the ID of the category to update
     * @param categoryDTO the updated category data
     * @return the updated category
     */
    CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO);

    /**
     * Delete a category
     * 
     * @param id the ID of the category to delete
     */
    void deleteCategory(Long id);

    /**
     * Get a category by ID
     * 
     * @param id the ID of the category
     * @return the category
     */
    CategoryDTO getCategoryById(Long id);

    /**
     * Get all categories with pagination
     * 
     * @param pageable pagination information
     * @param shopId   optional shop ID to filter by
     * @return page of categories
     */
    Page<CategoryDTO> getAllCategories(Pageable pageable, java.util.UUID shopId);

    /**
     * Get all top-level categories
     * 
     * @param shopId optional shop ID to filter by
     * @return list of top-level categories
     */
    List<CategoryDTO> getTopLevelCategories(java.util.UUID shopId);

    /**
     * Get all sub-categories of a given parent category
     * 
     * @param parentId the ID of the parent category
     * @return list of child categories
     */
    List<CategoryDTO> getSubCategories(Long parentId);

    /**
     * Search categories with multiple criteria
     * 
     * @param searchDTO the search criteria
     * @return page of categories matching the search criteria
     */
    Page<CategoryDTO> searchCategories(CategorySearchDTO searchDTO);

    /**
     * Convert a Category entity to a CategoryDTO
     * 
     * @param category the category entity
     * @return the category DTO
     */
    CategoryDTO convertToDTO(Category category);

    /**
     * Convert a CategoryDTO to a Category entity
     * 
     * @param categoryDTO the category DTO
     * @return the category entity
     */
    Category convertToEntity(CategoryDTO categoryDTO);
}