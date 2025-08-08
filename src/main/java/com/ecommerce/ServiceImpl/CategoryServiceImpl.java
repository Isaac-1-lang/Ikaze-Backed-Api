package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.CategoryDTO;
import com.ecommerce.entity.Category;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        existingCategory.setActive(categoryDTO.isActive());
        existingCategory.setFeatured(categoryDTO.isFeatured());
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
    public Page<CategoryDTO> searchCategoriesByName(String name, Pageable pageable) {
        Page<Category> categories = categoryRepository.findByNameContainingIgnoreCase(name, pageable);
        return categories.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDTO> getActiveCategories(Pageable pageable) {
        Page<Category> activeCategories = categoryRepository.findByIsActiveTrue(pageable);
        return activeCategories.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDTO> getFeaturedCategories(Pageable pageable) {
        Page<Category> featuredCategories = categoryRepository.findByIsFeaturedTrue(pageable);
        return featuredCategories.map(this::convertToDTO);
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
        category.setActive(categoryDTO.isActive());
        category.setFeatured(categoryDTO.isFeatured());
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