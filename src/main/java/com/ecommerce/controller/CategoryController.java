package com.ecommerce.controller;

import com.ecommerce.dto.CategoryDTO;
import com.ecommerce.dto.CategorySearchDTO;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ShopAuthorizationService;
import com.ecommerce.ServiceImpl.CustomUserDetails;
import com.ecommerce.Enum.UserRole;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Category Management", description = "Endpoints for managing product categories")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;
    private final ShopAuthorizationService shopAuthorizationService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Autowired
    public CategoryController(CategoryService categoryService,
            ShopAuthorizationService shopAuthorizationService,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.categoryService = categoryService;
        this.shopAuthorizationService = shopAuthorizationService;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
    @Operation(summary = "Create a new category", description = "Create a new product category (Admin/Employee/Vendor only)")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        log.info("Creating new category: {}", categoryDTO.getName());
        log.info("Category creation request - shopId: {}, categoryName: {}", categoryDTO.getShopId(),
                categoryDTO.getName());

        UUID currentUserId = getCurrentUserId();
        UserRole userRole = getCurrentUserRole();

        log.info("Current user - userId: {}, role: {}", currentUserId, userRole);

        // For VENDOR and EMPLOYEE, shopId is required and must validate access
        if (userRole == UserRole.VENDOR || userRole == UserRole.EMPLOYEE) {
            if (categoryDTO.getShopId() == null) {
                log.error("shopId is null for VENDOR/EMPLOYEE user - userId: {}, role: {}", currentUserId, userRole);
                throw new IllegalArgumentException("shopId is required for VENDOR and EMPLOYEE roles");
            }
            log.info("Validating shop access - userId: {}, shopId: {}", currentUserId, categoryDTO.getShopId());
            shopAuthorizationService.assertCanManageShop(currentUserId, categoryDTO.getShopId());
            log.info("Shop access validated successfully");
        }

        CategoryDTO createdCategory = categoryService.createCategory(categoryDTO);
        log.info("Category created successfully - categoryId: {}", createdCategory.getId());
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
    @Operation(summary = "Update a category", description = "Update an existing product category (Admin/Employee/Vendor only)")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDTO categoryDTO) {
        UUID currentUserId = getCurrentUserId();
        UserRole userRole = getCurrentUserRole();

        // Validate shop access for VENDOR and EMPLOYEE
        if (userRole == UserRole.VENDOR || userRole == UserRole.EMPLOYEE) {
            com.ecommerce.entity.Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Category not found"));
            if (category.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, category.getShop().getShopId());
            }
        }

        CategoryDTO updatedCategory = categoryService.updateCategory(id, categoryDTO);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
    @Operation(summary = "Delete a category", description = "Delete a product category (Admin/Employee/Vendor only)")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        UUID currentUserId = getCurrentUserId();
        UserRole userRole = getCurrentUserRole();

        // Validate shop access for VENDOR and EMPLOYEE
        if (userRole == UserRole.VENDOR || userRole == UserRole.EMPLOYEE) {
            com.ecommerce.entity.Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Category not found"));
            if (category.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, category.getShop().getShopId());
            }
        }

        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieve a product category by its ID")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        CategoryDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieve all product categories with pagination")
    public ResponseEntity<Page<CategoryDTO>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) UUID shopId) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Validate shop access only for VENDOR and EMPLOYEE if they are authenticated
        if (shopId != null) {
            UUID currentUserId = getCurrentUserId();
            UserRole userRole = getCurrentUserRole();
            if (currentUserId != null && userRole != null
                    && (userRole == UserRole.VENDOR || userRole == UserRole.EMPLOYEE)) {
                shopAuthorizationService.assertCanManageShop(currentUserId, shopId);
            }
        }

        Page<CategoryDTO> categories = categoryService.getAllCategories(pageable, shopId);

        return ResponseEntity.ok(categories);
    }

    @GetMapping("/top-level")
    @Operation(summary = "Get top-level categories", description = "Retrieve all top-level categories (categories without a parent)")
    public ResponseEntity<List<CategoryDTO>> getTopLevelCategories(@RequestParam(required = false) UUID shopId) {
        List<CategoryDTO> topLevelCategories = categoryService.getTopLevelCategories(shopId);
        return ResponseEntity.ok(topLevelCategories);
    }

    @GetMapping("/navigation")
    @Operation(summary = "Get categories for navigation", description = "Retrieve categories for header navigation with pagination")
    public ResponseEntity<Page<CategoryDTO>> getCategoriesForNavigation(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<CategoryDTO> categories = categoryService.getAllCategories(pageable, null);

        return ResponseEntity.ok(categories);
    }

    @GetMapping("/sub-categories/{parentId}")
    @Operation(summary = "Get sub-categories", description = "Retrieve all sub-categories of a given parent category")
    public ResponseEntity<List<CategoryDTO>> getSubCategories(@PathVariable Long parentId) {
        List<CategoryDTO> subCategories = categoryService.getSubCategories(parentId);
        return ResponseEntity.ok(subCategories);
    }

    @PostMapping("/search")
    @Operation(summary = "Search categories", description = "Search for categories with multiple criteria and pagination")
    public ResponseEntity<Page<CategoryDTO>> searchCategories(
            @RequestBody(required = false) CategorySearchDTO searchDTO) {
        if (searchDTO == null) {
            searchDTO = new CategorySearchDTO();
        }

        // Validate shop access only for VENDOR and EMPLOYEE if they are authenticated
        if (searchDTO.getShopId() != null) {
            UUID currentUserId = getCurrentUserId();
            UserRole userRole = getCurrentUserRole();
            if (currentUserId != null && userRole != null
                    && (userRole == UserRole.VENDOR || userRole == UserRole.EMPLOYEE)) {
                shopAuthorizationService.assertCanManageShop(currentUserId, searchDTO.getShopId());
            }
        }

        Page<CategoryDTO> categories = categoryService.searchCategories(searchDTO);
        return ResponseEntity.ok(categories);
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }

            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email)
                        .map(com.ecommerce.entity.User::getId)
                        .orElse(null);
            }

            return null;
        } catch (Exception e) {
            log.warn("Error getting current user ID: {}", e.getMessage());
            return null;
        }
    }

    private UserRole getCurrentUserRole() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }

            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email)
                        .map(com.ecommerce.entity.User::getRole)
                        .orElse(null);
            }

            return null;
        } catch (Exception e) {
            log.warn("Error getting current user role: {}", e.getMessage());
            return null;
        }
    }
}