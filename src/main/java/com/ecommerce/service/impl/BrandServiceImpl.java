package com.ecommerce.service.impl;

import com.ecommerce.dto.BrandDTO;
import com.ecommerce.dto.BrandSearchDTO;
import com.ecommerce.dto.CreateBrandDTO;
import com.ecommerce.dto.UpdateBrandDTO;
import com.ecommerce.entity.Brand;
import com.ecommerce.entity.Shop;
import com.ecommerce.repository.BrandRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.service.BrandService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ShopRepository shopRepository;

    @Autowired
    public BrandServiceImpl(BrandRepository brandRepository, ShopRepository shopRepository) {
        this.brandRepository = brandRepository;
        this.shopRepository = shopRepository;
    }

    @Override
    public BrandDTO createBrand(CreateBrandDTO createBrandDTO) {
        log.info("Creating new brand: {}", createBrandDTO.getBrandName());
        
        // Validate brand name uniqueness
        if (existsByBrandName(createBrandDTO.getBrandName(), null)) {
            throw new IllegalArgumentException("Brand name already exists: " + createBrandDTO.getBrandName());
        }
        
        // Convert DTO to entity
        Brand brand = convertToEntity(createBrandDTO);
        
        // Set shop if shopId is provided
        if (createBrandDTO.getShopId() != null) {
            Shop shop = shopRepository.findById(createBrandDTO.getShopId())
                    .orElseThrow(() -> new EntityNotFoundException("Shop not found with id: " + createBrandDTO.getShopId()));
            brand.setShop(shop);
        }
        
        // Set default values
        if (brand.getMetaTitle() == null || brand.getMetaTitle().trim().isEmpty()) {
            brand.setMetaTitle(brand.getBrandName());
        }
        if (brand.getMetaDescription() == null || brand.getMetaDescription().trim().isEmpty()) {
            brand.setMetaDescription(brand.getDescription());
        }
        
        // Save brand
        Brand savedBrand = brandRepository.save(brand);
        log.info("Brand created successfully with ID: {}", savedBrand.getBrandId());
        
        return convertToDTO(savedBrand);
    }

    @Override
    public BrandDTO updateBrand(UUID id, UpdateBrandDTO updateBrandDTO) {
        log.info("Updating brand with ID: {}", id);
        
        // Find existing brand
        Brand existingBrand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found with ID: " + id));
        
        // Validate brand name uniqueness if name is being changed
        if (StringUtils.hasText(updateBrandDTO.getBrandName()) && 
            !updateBrandDTO.getBrandName().equals(existingBrand.getBrandName())) {
            if (existsByBrandName(updateBrandDTO.getBrandName(), id)) {
                throw new IllegalArgumentException("Brand name already exists: " + updateBrandDTO.getBrandName());
            }
        }
        
        // Update fields
        if (StringUtils.hasText(updateBrandDTO.getBrandName())) {
            existingBrand.setBrandName(updateBrandDTO.getBrandName());
        }
        if (updateBrandDTO.getDescription() != null) {
            existingBrand.setDescription(updateBrandDTO.getDescription());
        }
        if (updateBrandDTO.getLogoUrl() != null) {
            existingBrand.setLogoUrl(updateBrandDTO.getLogoUrl());
        }
        if (updateBrandDTO.getWebsiteUrl() != null) {
            existingBrand.setWebsiteUrl(updateBrandDTO.getWebsiteUrl());
        }
        if (updateBrandDTO.getIsActive() != null) {
            existingBrand.setActive(updateBrandDTO.getIsActive());
        }
        if (updateBrandDTO.getIsFeatured() != null) {
            existingBrand.setFeatured(updateBrandDTO.getIsFeatured());
        }
        if (updateBrandDTO.getSortOrder() != null) {
            existingBrand.setSortOrder(updateBrandDTO.getSortOrder());
        }
        if (updateBrandDTO.getMetaTitle() != null) {
            existingBrand.setMetaTitle(updateBrandDTO.getMetaTitle());
        }
        if (updateBrandDTO.getMetaDescription() != null) {
            existingBrand.setMetaDescription(updateBrandDTO.getMetaDescription());
        }
        if (updateBrandDTO.getMetaKeywords() != null) {
            existingBrand.setMetaKeywords(updateBrandDTO.getMetaKeywords());
        }
        
        // Update timestamp
        existingBrand.setUpdatedAt(LocalDateTime.now());
        
        // Save updated brand
        Brand updatedBrand = brandRepository.save(existingBrand);
        log.info("Brand updated successfully with ID: {}", updatedBrand.getBrandId());
        
        return convertToDTO(updatedBrand);
    }

    @Override
    public void deleteBrand(UUID id) {
        log.info("Deleting brand with ID: {}", id);
        
        // Check if brand exists
        if (!brandRepository.existsById(id)) {
            throw new EntityNotFoundException("Brand not found with ID: " + id);
        }
        
        // Check if brand has associated products
        Brand brand = brandRepository.findById(id).orElse(null);
        if (brand != null && !brand.getProducts().isEmpty()) {
            throw new IllegalStateException("Cannot delete brand with associated products. Please remove or reassign products first.");
        }
        
        brandRepository.deleteById(id);
        log.info("Brand deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandDTO getBrandById(UUID id) {
        log.debug("Fetching brand by ID: {}", id);
        
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found with ID: " + id));
        
        return convertToDTO(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandDTO getBrandBySlug(String slug) {
        log.debug("Fetching brand by slug: {}", slug);
        
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found with slug: " + slug));
        
        return convertToDTO(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BrandDTO> getAllBrands(Pageable pageable, UUID shopId) {
        log.debug("Fetching all brands with pagination: {}, shopId: {}", pageable, shopId);
        
        Specification<Brand> spec = Specification.where(null);
        
        // Filter by shopId if provided
        if (shopId != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("shop").get("shopId"), shopId));
        }
        
        Page<Brand> brands = brandRepository.findAll(spec, pageable);
        return brands.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandDTO> getActiveBrands() {
        log.debug("Fetching all active brands");
        
        List<Brand> activeBrands = brandRepository.findByIsActiveTrue();
        return activeBrands.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandDTO> getFeaturedBrands() {
        log.debug("Fetching all featured brands");
        
        List<Brand> featuredBrands = brandRepository.findByIsFeaturedTrue();
        return featuredBrands.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BrandDTO> searchBrands(BrandSearchDTO searchDTO) {
        log.debug("Searching brands with criteria: {}", searchDTO);
        
        // Create specification for search
        Specification<Brand> spec = createSearchSpecification(searchDTO);
        
        // Create pageable
        Sort sort = Sort.by(
            searchDTO.getSortDir().equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
            searchDTO.getSortBy()
        );
        Pageable pageable = PageRequest.of(searchDTO.getPage(), searchDTO.getSize(), sort);
        
        // Execute search
        Page<Brand> brands = brandRepository.findAll(spec, pageable);
        return brands.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBrandName(String brandName, UUID excludeId) {
        if (excludeId == null) {
            return brandRepository.findByBrandName(brandName).isPresent();
        } else {
            return brandRepository.findByBrandName(brandName)
                    .map(brand -> !brand.getBrandId().equals(excludeId))
                    .orElse(false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug, UUID excludeId) {
        if (excludeId == null) {
            return brandRepository.findBySlug(slug).isPresent();
        } else {
            return brandRepository.findBySlug(slug)
                    .map(brand -> !brand.getBrandId().equals(excludeId))
                    .orElse(false);
        }
    }

    @Override
    public BrandDTO convertToDTO(Brand brand) {
        if (brand == null) {
            return null;
        }
        
        BrandDTO dto = new BrandDTO();
        dto.setBrandId(brand.getBrandId());
        dto.setBrandName(brand.getBrandName());
        dto.setDescription(brand.getDescription());
        dto.setLogoUrl(brand.getLogoUrl());
        dto.setWebsiteUrl(brand.getWebsiteUrl());
        dto.setSlug(brand.getSlug());
        dto.setActive(brand.isActive());
        dto.setFeatured(brand.isFeatured());
        dto.setSortOrder(brand.getSortOrder());
        dto.setMetaTitle(brand.getMetaTitle());
        dto.setMetaDescription(brand.getMetaDescription());
        dto.setMetaKeywords(brand.getMetaKeywords());
        dto.setCreatedAt(brand.getCreatedAt());
        dto.setUpdatedAt(brand.getUpdatedAt());
        
        // Set product count
        if (brand.getProducts() != null) {
            dto.setProductCount((long) brand.getProducts().size());
        } else {
            dto.setProductCount(0L);
        }
        
        // Set shop information if available
        if (brand.getShop() != null) {
            dto.setShopId(brand.getShop().getShopId());
            dto.setShopName(brand.getShop().getName());
        }
        
        return dto;
    }

    @Override
    public Brand convertToEntity(CreateBrandDTO createBrandDTO) {
        if (createBrandDTO == null) {
            return null;
        }
        
        Brand brand = new Brand();
        brand.setBrandName(createBrandDTO.getBrandName());
        brand.setDescription(createBrandDTO.getDescription());
        brand.setLogoUrl(createBrandDTO.getLogoUrl());
        brand.setWebsiteUrl(createBrandDTO.getWebsiteUrl());
        brand.setActive(createBrandDTO.isActive());
        brand.setFeatured(createBrandDTO.isFeatured());
        brand.setSortOrder(createBrandDTO.getSortOrder());
        brand.setMetaTitle(createBrandDTO.getMetaTitle());
        brand.setMetaDescription(createBrandDTO.getMetaDescription());
        brand.setMetaKeywords(createBrandDTO.getMetaKeywords());
        
        return brand;
    }

    /**
     * Create search specification for brands
     */
    private Specification<Brand> createSearchSpecification(BrandSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            
            if (StringUtils.hasText(searchDTO.getBrandName())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("brandName")),
                    "%" + searchDTO.getBrandName().toLowerCase() + "%"
                ));
            }
            
            if (StringUtils.hasText(searchDTO.getDescription())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")),
                    "%" + searchDTO.getDescription().toLowerCase() + "%"
                ));
            }
            
            if (searchDTO.getIsActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), searchDTO.getIsActive()));
            }
            
            if (searchDTO.getIsFeatured() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isFeatured"), searchDTO.getIsFeatured()));
            }
            
            if (StringUtils.hasText(searchDTO.getSlug())) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("slug")),
                    "%" + searchDTO.getSlug().toLowerCase() + "%"
                ));
            }
            
            // Filter by shopId if provided
            if (searchDTO.getShopId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("shop").get("shopId"), searchDTO.getShopId()));
            }
            
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
