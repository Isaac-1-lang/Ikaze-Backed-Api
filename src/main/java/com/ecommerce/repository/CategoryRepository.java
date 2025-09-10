package com.ecommerce.repository;

import com.ecommerce.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    /**
     * Find all top-level categories (categories with no parent)
     * 
     * @return list of top-level categories
     */
    List<Category> findByParentIsNull();

    /**
     * Find all sub-categories of a given parent category
     * 
     * @param parentId the ID of the parent category
     * @return list of child categories
     */
    List<Category> findByParentId(Long parentId);

    /**
     * Find a category by its name
     * 
     * @param name the name of the category
     * @return the category if found
     */
    Optional<Category> findByName(String name);

    /**
     * Find a category by its slug
     * 
     * @param slug the slug of the category
     * @return the category if found
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Check if a category has any children
     * 
     * @param categoryId the ID of the category
     * @return true if the category has children, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE c.parent.id = :categoryId")
    boolean hasChildren(@Param("categoryId") Long categoryId);

    /**
     * Find all categories with pagination
     * 
     * @param pageable pagination information
     * @return a page of categories
     */
    Page<Category> findAll(Pageable pageable);

    /**
     * Find categories by name containing the given string (case insensitive)
     * 
     * @param name     the name to search for
     * @param pageable pagination information
     * @return a page of categories matching the search criteria
     */
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find categories that are active
     * 
     * @param pageable pagination information
     * @return a page of active categories
     */
    Page<Category> findByIsActiveTrue(Pageable pageable);

    /**
     * Find categories that are featured
     * 
     * @param pageable pagination information
     * @return a page of featured categories
     */
    Page<Category> findByIsFeaturedTrue(Pageable pageable);

    // Search suggestion method
    List<Category> findTop5ByNameContainingIgnoreCase(String name);
}