package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.CategoryDTO;
import com.ecommerce.dto.CategorySearchDTO;
import com.ecommerce.entity.Category;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = convertToEntity(categoryDTO);
        Category savedCategory = categoryRepository.save(category);
        return convertToDTO(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));

        existingCategory.setName(categoryDTO.getName());
        existingCategory.setDescription(categoryDTO.getDescription());
        existingCategory.setImageUrl(categoryDTO.getImageUrl());
        existingCategory.setSortOrder(categoryDTO.getSortOrder());
        existingCategory.setIsActive(categoryDTO.isActive());
        existingCategory.setIsFeatured(categoryDTO.isFeatured());
        existingCategory.setMetaTitle(categoryDTO.getMetaTitle());
        existingCategory.setMetaDescription(categoryDTO.getMetaDescription());
        existingCategory.setMetaKeywords(categoryDTO.getMetaKeywords());

        // Update parent if changed
        if (categoryDTO.getParentId() != null) {
            Category parent = categoryRepository.findById(categoryDTO.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent category not found with id: " + categoryDTO.getParentId()));
            existingCategory.setParent(parent);
        } else {
            existingCategory.setParent(null);
        }

        // Generate slug if not provided
        if (categoryDTO.getSlug() == null || categoryDTO.getSlug().trim().isEmpty()) {
            // The slug will be generated in the @PreUpdate method of the entity
        } else {
            existingCategory.setSlug(categoryDTO.getSlug());
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        return convertToDTO(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));

        // Check if the category has children
        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with sub-categories. Delete sub-categories first.");
        }

        // Check if the category has products
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with products. Remove products first or reassign them to another category.");
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
        return convertToDTO(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDTO> getAllCategories(Pageable pageable) {
        Page<Category> categories = categoryRepository.findAll(pageable);
        return categories.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getTopLevelCategories() {
        List<Category> topLevelCategories = categoryRepository.findByParentIsNull();
        return topLevelCategories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getSubCategories(Long parentId) {
        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Parent category not found with id: " + parentId));

        return parent.getChildren().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDTO> searchCategories(CategorySearchDTO searchDTO) {
        // Create a specification for dynamic filtering
        Specification<Category> spec = Specification.where(null);
        
        // Add filters based on search criteria
        if (searchDTO.getName() != null && !searchDTO.getName().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("name")), "%" + searchDTO.getName().toLowerCase() + "%"));
        }
        
        if (searchDTO.getDescription() != null && !searchDTO.getDescription().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("description")), "%" + searchDTO.getDescription().toLowerCase() + "%"));
        }
        
        if (searchDTO.getIsActive() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("isActive"), searchDTO.getIsActive()));
        }
        
        if (searchDTO.getIsFeatured() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("isFeatured"), searchDTO.getIsFeatured()));
        }
        
        // Create pageable with sorting
        String sortBy = searchDTO.getSortBy() != null ? searchDTO.getSortBy() : "name";
        String sortDir = searchDTO.getSortDirection() != null ? searchDTO.getSortDirection() : "asc";
        
        // Handle special sorting cases
        if (searchDTO.getSortByChildrenCount() != null && searchDTO.getSortByChildrenCount()) {
            // This requires a custom query with join and count
            // For simplicity, we'll use a workaround by fetching all matching categories and sorting in memory
            List<Category> allMatchingCategories = categoryRepository.findAll(spec);
            
            // Sort by children count
            if ("asc".equalsIgnoreCase(sortDir)) {
                allMatchingCategories.sort(Comparator.comparingInt(c -> c.getChildren().size()));
            } else {
                allMatchingCategories.sort((c1, c2) -> Integer.compare(c2.getChildren().size(), c1.getChildren().size()));
            }
            
            // Create a page manually
            int pageSize = searchDTO.getSize() != null ? searchDTO.getSize() : 10;
            int pageNum = searchDTO.getPage() != null ? searchDTO.getPage() : 0;
            int start = pageNum * pageSize;
            int end = Math.min(start + pageSize, allMatchingCategories.size());
            
            List<Category> pageContent = start < allMatchingCategories.size() ?
                    allMatchingCategories.subList(start, end) : new ArrayList<>();
            
            Page<Category> page = new PageImpl<>(pageContent, 
                    PageRequest.of(pageNum, pageSize), allMatchingCategories.size());
            
            return page.map(this::convertToDTO);
        }
        
        if (searchDTO.getSortByProductCount() != null && searchDTO.getSortByProductCount()) {
            // Similar approach for sorting by product count
            List<Category> allMatchingCategories = categoryRepository.findAll(spec);
            
            // Sort by product count
            if ("asc".equalsIgnoreCase(sortDir)) {
                allMatchingCategories.sort(Comparator.comparingInt(c -> 
                    c.getProducts() != null ? c.getProducts().size() : 0));
            } else {
                allMatchingCategories.sort((c1, c2) -> Integer.compare(
                    c2.getProducts() != null ? c2.getProducts().size() : 0,
                    c1.getProducts() != null ? c1.getProducts().size() : 0));
            }
            
            // Create a page manually
            int pageSize = searchDTO.getSize() != null ? searchDTO.getSize() : 10;
            int pageNum = searchDTO.getPage() != null ? searchDTO.getPage() : 0;
            int start = pageNum * pageSize;
            int end = Math.min(start + pageSize, allMatchingCategories.size());
            
            List<Category> pageContent = start < allMatchingCategories.size() ?
                    allMatchingCategories.subList(start, end) : new ArrayList<>();
            
            Page<Category> page = new PageImpl<>(pageContent, 
                    PageRequest.of(pageNum, pageSize), allMatchingCategories.size());
            
            return page.map(this::convertToDTO);
        }
        
        // Standard sorting for other cases
        Sort sort = "asc".equalsIgnoreCase(sortDir) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(
                searchDTO.getPage() != null ? searchDTO.getPage() : 0,
                searchDTO.getSize() != null ? searchDTO.getSize() : 10,
                sort);
        
        Page<Category> categories = categoryRepository.findAll(spec, pageable);
        return categories.map(this::convertToDTO);
    }

    @Override
    public CategoryDTO convertToDTO(Category category) {
        if (category == null) {
            return null;
        }

        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setImageUrl(category.getImageUrl());
        dto.setSlug(category.getSlug());
        dto.setSortOrder(category.getSortOrder());
        dto.setActive(category.isActive());
        dto.setFeatured(category.isFeatured());
        dto.setMetaTitle(category.getMetaTitle());
        dto.setMetaDescription(category.getMetaDescription());
        dto.setMetaKeywords(category.getMetaKeywords());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        dto.setLevel(category.getLevel());

        // Set parent information if available
        if (category.getParent() != null) {
            dto.setParentId(category.getParent().getId());
            dto.setParentName(category.getParent().getName());
        }

        // Set children recursively
        if (category.getChildren() != null) {
            List<CategoryDTO> childrenDTOs = category.getChildren().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            dto.setChildren(childrenDTOs);
        }

        return dto;
    }

    @Override
    public Category convertToEntity(CategoryDTO categoryDTO) {
        if (categoryDTO == null) {
            return null;
        }

        Category category = new Category();
        
        // Don't set ID for new entities, only for updates
        if (categoryDTO.getId() != null) {
            category.setId(categoryDTO.getId());
        }
        
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setImageUrl(categoryDTO.getImageUrl());
        category.setSlug(categoryDTO.getSlug());
        category.setSortOrder(categoryDTO.getSortOrder());
        category.setIsActive(categoryDTO.isActive());
        category.setIsFeatured(categoryDTO.isFeatured());
        category.setMetaTitle(categoryDTO.getMetaTitle());
        category.setMetaDescription(categoryDTO.getMetaDescription());
        category.setMetaKeywords(categoryDTO.getMetaKeywords());

        // Set parent if parentId is provided
        if (categoryDTO.getParentId() != null) {
            Category parent = categoryRepository.findById(categoryDTO.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent category not found with id: " + categoryDTO.getParentId()));
            category.setParent(parent);
        }

        return category;
    }
}