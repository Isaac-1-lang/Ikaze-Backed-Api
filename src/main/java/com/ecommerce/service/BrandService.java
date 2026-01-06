package com.ecommerce.service;

import com.ecommerce.dto.BrandDTO;
import com.ecommerce.dto.BrandSearchDTO;
import com.ecommerce.dto.CreateBrandDTO;
import com.ecommerce.dto.UpdateBrandDTO;
import com.ecommerce.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BrandService {

    /**
     * Create a new brand
     * 
     * @param createBrandDTO the brand creation data
     * @return the created brand
     */
    BrandDTO createBrand(CreateBrandDTO createBrandDTO);

    /**
     * Update an existing brand
     * 
     * @param id             the ID of the brand to update
     * @param updateBrandDTO the updated brand data
     * @return the updated brand
     */
    BrandDTO updateBrand(UUID id, UpdateBrandDTO updateBrandDTO);

    /**
     * Delete a brand
     * 
     * @param id the ID of the brand to delete
     */
    void deleteBrand(UUID id);

    /**
     * Get a brand by ID
     * 
     * @param id the ID of the brand
     * @return the brand
     */
    BrandDTO getBrandById(UUID id);

    /**
     * Get a brand by slug
     * 
     * @param slug the slug of the brand
     * @return the brand
     */
    BrandDTO getBrandBySlug(String slug);

    /**
     * Get all brands with pagination
     * 
     * @param pageable pagination information
     * @param shopId   optional shop ID to filter by
     * @return page of brands
     */
    Page<BrandDTO> getAllBrands(Pageable pageable, UUID shopId);

    /**
     * Get all active brands
     * 
     * @param shopId optional shop ID to filter by
     * @return list of active brands
     */
    List<BrandDTO> getActiveBrands(UUID shopId);

    /**
     * Get all featured brands
     * 
     * @param shopId optional shop ID to filter by
     * @return list of featured brands
     */
    List<BrandDTO> getFeaturedBrands(UUID shopId);

    /**
     * Search brands with multiple criteria
     * 
     * @param searchDTO the search criteria
     * @return page of brands matching the search criteria
     */
    Page<BrandDTO> searchBrands(BrandSearchDTO searchDTO);

    /**
     * Check if brand name already exists
     * 
     * @param brandName the brand name to check
     * @param excludeId the brand ID to exclude from check (for updates)
     * @return true if brand name exists, false otherwise
     */
    boolean existsByBrandName(String brandName, UUID excludeId);

    /**
     * Check if brand slug already exists
     * 
     * @param slug      the slug to check
     * @param excludeId the brand ID to exclude from check (for updates)
     * @return true if slug exists, false otherwise
     */
    boolean existsBySlug(String slug, UUID excludeId);

    /**
     * Convert a Brand entity to a BrandDTO
     * 
     * @param brand the brand entity
     * @return the brand DTO
     */
    BrandDTO convertToDTO(Brand brand);

    /**
     * Convert a CreateBrandDTO to a Brand entity
     * 
     * @param createBrandDTO the create brand DTO
     * @return the brand entity
     */
    Brand convertToEntity(CreateBrandDTO createBrandDTO);
}
