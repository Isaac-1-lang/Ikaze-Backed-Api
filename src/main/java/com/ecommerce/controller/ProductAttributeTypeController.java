package com.ecommerce.controller;

import com.ecommerce.dto.ProductAttributeTypeDTO;
import com.ecommerce.dto.ProductAttributeTypeRequestDTO;
import com.ecommerce.service.ProductAttributeTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product-attribute-types")
@Tag(name = "Product Attribute Types", description = "API for managing product attribute types")
@SecurityRequirement(name = "bearerAuth")
public class ProductAttributeTypeController {

        private final ProductAttributeTypeService productAttributeTypeService;

        @Autowired
        public ProductAttributeTypeController(ProductAttributeTypeService productAttributeTypeService) {
                this.productAttributeTypeService = productAttributeTypeService;
        }

        @PostMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        @Operation(summary = "Create a new product attribute type", description = "Creates a new product attribute type with the provided information", responses = {
                        @ApiResponse(responseCode = "201", description = "Product attribute type created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or EMPLOYEE role")
        })
        public ResponseEntity<ProductAttributeTypeDTO> createAttributeType(
                        @Valid @RequestBody ProductAttributeTypeRequestDTO requestDTO) {
                ProductAttributeTypeDTO createdType = productAttributeTypeService.createAttributeType(requestDTO);
                return new ResponseEntity<>(createdType, HttpStatus.CREATED);
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        @Operation(summary = "Update a product attribute type", description = "Updates an existing product attribute type with the provided information", responses = {
                        @ApiResponse(responseCode = "200", description = "Product attribute type updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or EMPLOYEE role"),
                        @ApiResponse(responseCode = "404", description = "Product attribute type not found")
        })
        public ResponseEntity<ProductAttributeTypeDTO> updateAttributeType(
                        @Parameter(description = "ID of the product attribute type to update") @PathVariable Long id,
                        @Valid @RequestBody ProductAttributeTypeRequestDTO requestDTO) {
                ProductAttributeTypeDTO updatedType = productAttributeTypeService.updateAttributeType(id, requestDTO);
                return ResponseEntity.ok(updatedType);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get a product attribute type by ID", description = "Retrieves a product attribute type by its ID", responses = {
                        @ApiResponse(responseCode = "200", description = "Product attribute type retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Product attribute type not found")
        })
        public ResponseEntity<ProductAttributeTypeDTO> getAttributeTypeById(
                        @Parameter(description = "ID of the product attribute type to retrieve") @PathVariable Long id) {
                ProductAttributeTypeDTO attributeType = productAttributeTypeService.getAttributeTypeById(id);
                return ResponseEntity.ok(attributeType);
        }

        @GetMapping("/name/{name}")
        @Operation(summary = "Get a product attribute type by name", description = "Retrieves a product attribute type by its name", responses = {
                        @ApiResponse(responseCode = "200", description = "Product attribute type retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Product attribute type not found")
        })
        public ResponseEntity<ProductAttributeTypeDTO> getAttributeTypeByName(
                        @Parameter(description = "Name of the product attribute type to retrieve") @PathVariable String name) {
                ProductAttributeTypeDTO attributeType = productAttributeTypeService.getAttributeTypeByName(name);
                return ResponseEntity.ok(attributeType);
        }

        @GetMapping
        @Operation(summary = "Get all product attribute types", description = "Retrieves all product attribute types, optionally paginated", responses = {
                        @ApiResponse(responseCode = "200", description = "Product attribute types retrieved successfully")
        })
        public ResponseEntity<?> getAllAttributeTypes(
                        @Parameter(description = "Page number (0-based)") @RequestParam(required = false) Integer page,
                        @Parameter(description = "Page size") @RequestParam(required = false) Integer size,
                        @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sort,
                        @Parameter(description = "Sort direction (ASC or DESC)") @RequestParam(defaultValue = "ASC") String direction) {

                if (page != null && size != null) {
                        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
                        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                        Page<ProductAttributeTypeDTO> attributeTypes = productAttributeTypeService
                                        .getAllAttributeTypes(pageable);
                        return ResponseEntity.ok(attributeTypes);
                } else {
                        List<ProductAttributeTypeDTO> attributeTypes = productAttributeTypeService
                                        .getAllAttributeTypes();
                        return ResponseEntity.ok(attributeTypes);
                }
        }

        @GetMapping("/search")
        @Operation(summary = "Search product attribute types by name", description = "Searches for product attribute types by name with pagination", responses = {
                        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
        })
        public ResponseEntity<Page<ProductAttributeTypeDTO>> searchAttributeTypes(
                        @Parameter(description = "Name to search for") @RequestParam String name,
                        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
                        @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sort,
                        @Parameter(description = "Sort direction (ASC or DESC)") @RequestParam(defaultValue = "ASC") String direction) {

                Sort.Direction sortDirection = Sort.Direction.fromString(direction);
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                Page<ProductAttributeTypeDTO> searchResults = productAttributeTypeService
                                .searchAttributeTypesByName(name, pageable);
                return ResponseEntity.ok(searchResults);
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        @Operation(summary = "Delete a product attribute type", description = "Deletes a product attribute type by its ID", responses = {
                        @ApiResponse(responseCode = "204", description = "Product attribute type deleted successfully"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or EMPLOYEE role"),
                        @ApiResponse(responseCode = "404", description = "Product attribute type not found"),
                        @ApiResponse(responseCode = "409", description = "Product attribute type is in use and cannot be deleted")
        })
        public ResponseEntity<Void> deleteAttributeType(
                        @Parameter(description = "ID of the product attribute type to delete") @PathVariable Long id) {
                boolean deleted = productAttributeTypeService.deleteAttributeType(id);
                return deleted ? ResponseEntity.noContent().build()
                                : ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        @GetMapping("/{id}/in-use")
        @Operation(summary = "Check if a product attribute type is in use", description = "Checks if a product attribute type is currently in use", responses = {
                        @ApiResponse(responseCode = "200", description = "Check completed successfully"),
                        @ApiResponse(responseCode = "404", description = "Product attribute type not found")
        })
        public ResponseEntity<Boolean> isAttributeTypeInUse(
                        @Parameter(description = "ID of the product attribute type to check") @PathVariable Long id) {
                boolean inUse = productAttributeTypeService.isAttributeTypeInUse(id);
                return ResponseEntity.ok(inUse);
        }
}