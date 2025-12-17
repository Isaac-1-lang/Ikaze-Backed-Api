package com.ecommerce.controller;

import com.ecommerce.dto.BrandDTO;
import com.ecommerce.dto.BrandSearchDTO;
import com.ecommerce.dto.CreateBrandDTO;
import com.ecommerce.dto.UpdateBrandDTO;
import com.ecommerce.service.BrandService;
import com.ecommerce.service.ShopAuthorizationService;
import com.ecommerce.ServiceImpl.CustomUserDetails;
import com.ecommerce.Enum.UserRole;
import com.ecommerce.repository.BrandRepository;
import com.ecommerce.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
@Tag(name = "Brand Management", description = "Endpoints for managing product brands")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class BrandController {

    private final BrandService brandService;
    private final ShopAuthorizationService shopAuthorizationService;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;

    @Autowired
    public BrandController(BrandService brandService,
                          ShopAuthorizationService shopAuthorizationService,
                          BrandRepository brandRepository,
                          UserRepository userRepository) {
        this.brandService = brandService;
        this.shopAuthorizationService = shopAuthorizationService;
        this.brandRepository = brandRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
    @Operation(summary = "Create a new brand", description = "Create a new product brand (Admin/Employee/Vendor only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Brand created successfully", content = @Content(schema = @Schema(implementation = BrandDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Brand name already exists")
    })
    public ResponseEntity<BrandDTO> createBrand(@Valid @RequestBody CreateBrandDTO createBrandDTO) {
        log.info("Creating new brand: {}", createBrandDTO.getBrandName());
        log.info("Brand creation request - shopId: {}, brandName: {}", createBrandDTO.getShopId(), createBrandDTO.getBrandName());
        
        UUID currentUserId = getCurrentUserId();
        UserRole userRole = getCurrentUserRole();
        
        log.info("Current user - userId: {}, role: {}", currentUserId, userRole);
        
        // For VENDOR and EMPLOYEE, shopId is required and must validate access
        if (userRole == UserRole.VENDOR || userRole == UserRole.EMPLOYEE) {
            if (createBrandDTO.getShopId() == null) {
                log.error("shopId is null for VENDOR/EMPLOYEE user - userId: {}, role: {}", currentUserId, userRole);
                throw new IllegalArgumentException("shopId is required for VENDOR and EMPLOYEE roles");
            }
            log.info("Validating shop access - userId: {}, shopId: {}", currentUserId, createBrandDTO.getShopId());
            shopAuthorizationService.assertCanManageShop(currentUserId, createBrandDTO.getShopId());
            log.info("Shop access validated successfully");
        }
        
        BrandDTO createdBrand = brandService.createBrand(createBrandDTO);
        log.info("Brand created successfully - brandId: {}", createdBrand.getBrandId());
        return new ResponseEntity<>(createdBrand, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
    @Operation(summary = "Update a brand", description = "Update an existing product brand (Admin/Employee/Vendor only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brand updated successfully", content = @Content(schema = @Schema(implementation = BrandDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Brand not found"),
            @ApiResponse(responseCode = "409", description = "Brand name already exists")
    })
    public ResponseEntity<BrandDTO> updateBrand(
            @Parameter(description = "Brand ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateBrandDTO updateBrandDTO) {
        log.info("Updating brand with ID: {}", id);
        UUID currentUserId = getCurrentUserId();
        UserRole userRole = getCurrentUserRole();
        
        // Validate shop access for VENDOR and EMPLOYEE
        if (userRole == UserRole.VENDOR || userRole == UserRole.EMPLOYEE) {
            com.ecommerce.entity.Brand brand = brandRepository.findById(id)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Brand not found"));
            if (brand.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, brand.getShop().getShopId());
            }
        }
        
        BrandDTO updatedBrand = brandService.updateBrand(id, updateBrandDTO);
        return ResponseEntity.ok(updatedBrand);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
    @Operation(summary = "Delete a brand", description = "Delete a product brand (Admin/Employee/Vendor only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Brand deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Brand not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete brand with associated products")
    })
    public ResponseEntity<Void> deleteBrand(@Parameter(description = "Brand ID") @PathVariable UUID id) {
        log.info("Deleting brand with ID: {}", id);
        UUID currentUserId = getCurrentUserId();
        UserRole userRole = getCurrentUserRole();
        
        // Validate shop access for VENDOR and EMPLOYEE
        if (userRole == UserRole.VENDOR || userRole == UserRole.EMPLOYEE) {
            com.ecommerce.entity.Brand brand = brandRepository.findById(id)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Brand not found"));
            if (brand.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, brand.getShop().getShopId());
            }
        }
        
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get brand by ID", description = "Retrieve a product brand by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brand retrieved successfully", content = @Content(schema = @Schema(implementation = BrandDTO.class))),
            @ApiResponse(responseCode = "404", description = "Brand not found")
    })
    public ResponseEntity<BrandDTO> getBrandById(@Parameter(description = "Brand ID") @PathVariable UUID id) {
        log.debug("Fetching brand by ID: {}", id);
        BrandDTO brand = brandService.getBrandById(id);
        return ResponseEntity.ok(brand);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get brand by slug", description = "Retrieve a product brand by its slug")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brand retrieved successfully", content = @Content(schema = @Schema(implementation = BrandDTO.class))),
            @ApiResponse(responseCode = "404", description = "Brand not found")
    })
    public ResponseEntity<BrandDTO> getBrandBySlug(@Parameter(description = "Brand slug") @PathVariable String slug) {
        log.debug("Fetching brand by slug: {}", slug);
        BrandDTO brand = brandService.getBrandBySlug(slug);
        return ResponseEntity.ok(brand);
    }

    @GetMapping
    @Operation(summary = "Get all brands", description = "Retrieve all product brands with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brands retrieved successfully", content = @Content(schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<BrandDTO>> getAllBrands(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "brandName") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Shop ID to filter by") @RequestParam(required = false) UUID shopId) {

        log.debug("Fetching all brands with pagination: page={}, size={}, sortBy={}, sortDir={}, shopId={}", 
                page, size, sortBy, sortDir, shopId);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Validate shop access for VENDOR and EMPLOYEE if shopId is provided
        if (shopId != null) {
            UUID currentUserId = getCurrentUserId();
            UserRole userRole = getCurrentUserRole();
            if (userRole == UserRole.VENDOR || userRole == UserRole.EMPLOYEE) {
                shopAuthorizationService.assertCanManageShop(currentUserId, shopId);
            }
        }
        
        Page<BrandDTO> brands = brandService.getAllBrands(pageable, shopId);

        return ResponseEntity.ok(brands);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active brands", description = "Retrieve all active product brands")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active brands retrieved successfully", content = @Content(schema = @Schema(implementation = List.class)))
    })
    public ResponseEntity<List<BrandDTO>> getActiveBrands() {
        log.debug("Fetching all active brands");
        List<BrandDTO> activeBrands = brandService.getActiveBrands();
        return ResponseEntity.ok(activeBrands);
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured brands", description = "Retrieve all featured product brands")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Featured brands retrieved successfully", content = @Content(schema = @Schema(implementation = List.class)))
    })
    public ResponseEntity<List<BrandDTO>> getFeaturedBrands() {
        log.debug("Fetching all featured brands");
        List<BrandDTO> featuredBrands = brandService.getFeaturedBrands();
        return ResponseEntity.ok(featuredBrands);
    }

    @PostMapping("/search")
    @Operation(summary = "Search brands", description = "Search for brands with multiple criteria and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brands search completed successfully", content = @Content(schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<BrandDTO>> searchBrands(@RequestBody(required = false) BrandSearchDTO searchDTO) {
        if (searchDTO == null) {
            searchDTO = new BrandSearchDTO();
        }

        // Validate shop access for VENDOR and EMPLOYEE if shopId is provided
        if (searchDTO.getShopId() != null) {
            UUID currentUserId = getCurrentUserId();
            UserRole userRole = getCurrentUserRole();
            if (userRole == UserRole.VENDOR || userRole == UserRole.EMPLOYEE) {
                shopAuthorizationService.assertCanManageShop(currentUserId, searchDTO.getShopId());
            }
        }

        log.debug("Searching brands with criteria: {}", searchDTO);
        Page<BrandDTO> brands = brandService.searchBrands(searchDTO);
        return ResponseEntity.ok(brands);
    }

    @GetMapping("/check-name")
    @Operation(summary = "Check brand name availability", description = "Check if a brand name is available")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Name availability checked successfully")
    })
    public ResponseEntity<Boolean> checkBrandNameAvailability(
            @Parameter(description = "Brand name to check") @RequestParam String brandName,
            @Parameter(description = "Brand ID to exclude (for updates)") @RequestParam(required = false) UUID excludeId) {

        log.debug("Checking brand name availability: {} (excludeId: {})", brandName, excludeId);
        boolean isAvailable = !brandService.existsByBrandName(brandName, excludeId);
        return ResponseEntity.ok(isAvailable);
    }

    @GetMapping("/check-slug")
    @Operation(summary = "Check brand slug availability", description = "Check if a brand slug is available")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Slug availability checked successfully")
    })
    public ResponseEntity<Boolean> checkBrandSlugAvailability(
            @Parameter(description = "Brand slug to check") @RequestParam String slug,
            @Parameter(description = "Brand ID to exclude (for updates)") @RequestParam(required = false) UUID excludeId) {

        log.debug("Checking brand slug availability: {} (excludeId: {})", slug, excludeId);
        boolean isAvailable = !brandService.existsBySlug(slug, excludeId);
        return ResponseEntity.ok(isAvailable);
    }
    
    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new RuntimeException("User not authenticated");
            }

            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            }

            throw new RuntimeException("Unable to extract user ID from authentication");
        } catch (Exception e) {
            throw new RuntimeException("Error getting current user ID: " + e.getMessage(), e);
        }
    }
    
    private UserRole getCurrentUserRole() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new RuntimeException("User not authenticated");
            }

            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email)
                    .map(com.ecommerce.entity.User::getRole)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            }

            throw new RuntimeException("Unable to extract user role from authentication");
        } catch (Exception e) {
            throw new RuntimeException("Error getting current user role: " + e.getMessage(), e);
        }
    }
}
