package com.ecommerce.controller;

import com.ecommerce.dto.ProductAttributeValueDTO;
import com.ecommerce.dto.ProductAttributeValueRequestDTO;
import com.ecommerce.service.ProductAttributeValueService;
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
@RequestMapping("/api/v1/product-attribute-values")
@Tag(name = "Product Attribute Values", description = "API for managing product attribute values")
@SecurityRequirement(name = "bearerAuth")
public class ProductAttributeValueController {

        private final ProductAttributeValueService productAttributeValueService;

        @Autowired
        public ProductAttributeValueController(ProductAttributeValueService productAttributeValueService) {
                this.productAttributeValueService = productAttributeValueService;
        }

        @PostMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        @Operation(summary = "Create a new product attribute value", description = "Creates a new product attribute value with the provided information", responses = {
                        @ApiResponse(responseCode = "201", description = "Product attribute value created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or EMPLOYEE role"),
                        @ApiResponse(responseCode = "404", description = "Referenced attribute type not found")
        })
        public ResponseEntity<ProductAttributeValueDTO> createAttributeValue(
                        @Valid @RequestBody ProductAttributeValueRequestDTO requestDTO) {
                ProductAttributeValueDTO createdValue = productAttributeValueService.createAttributeValue(requestDTO);
                return new ResponseEntity<>(createdValue, HttpStatus.CREATED);
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        @Operation(summary = "Update a product attribute value", description = "Updates an existing product attribute value with the provided information", responses = {
                        @ApiResponse(responseCode = "200", description = "Product attribute value updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or EMPLOYEE role"),
                        @ApiResponse(responseCode = "404", description = "Product attribute value or referenced type not found")
        })
        public ResponseEntity<ProductAttributeValueDTO> updateAttributeValue(
                        @Parameter(description = "ID of the product attribute value to update") @PathVariable Long id,
                        @Valid @RequestBody ProductAttributeValueRequestDTO requestDTO) {
                ProductAttributeValueDTO updatedValue = productAttributeValueService.updateAttributeValue(id,
                                requestDTO);
                return ResponseEntity.ok(updatedValue);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get a product attribute value by ID", description = "Retrieves a product attribute value by its ID", responses = {
                        @ApiResponse(responseCode = "200", description = "Product attribute value retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Product attribute value not found")
        })
        public ResponseEntity<ProductAttributeValueDTO> getAttributeValueById(
                        @Parameter(description = "ID of the product attribute value to retrieve") @PathVariable Long id) {
                ProductAttributeValueDTO attributeValue = productAttributeValueService.getAttributeValueById(id);
                return ResponseEntity.ok(attributeValue);
        }

        @GetMapping("/type/{attributeTypeId}")
        @Operation(summary = "Get all product attribute values for a specific type", description = "Retrieves all product attribute values for a specific attribute type, optionally paginated", responses = {
                        @ApiResponse(responseCode = "200", description = "Product attribute values retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Product attribute type not found")
        })
        public ResponseEntity<?> getAttributeValuesByTypeId(
                        @Parameter(description = "ID of the attribute type") @PathVariable Long attributeTypeId,
                        @Parameter(description = "Page number (0-based)") @RequestParam(required = false) Integer page,
                        @Parameter(description = "Page size") @RequestParam(required = false) Integer size,
                        @Parameter(description = "Sort field") @RequestParam(defaultValue = "value") String sort,
                        @Parameter(description = "Sort direction (ASC or DESC)") @RequestParam(defaultValue = "ASC") String direction) {

                if (page != null && size != null) {
                        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
                        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                        Page<ProductAttributeValueDTO> attributeValues = productAttributeValueService
                                        .getAttributeValuesByTypeId(attributeTypeId, pageable);
                        return ResponseEntity.ok(attributeValues);
                } else {
                        List<ProductAttributeValueDTO> attributeValues = productAttributeValueService
                                        .getAttributeValuesByTypeId(attributeTypeId);
                        return ResponseEntity.ok(attributeValues);
                }
        }

        @GetMapping
        @Operation(summary = "Get all product attribute values", description = "Retrieves all product attribute values, optionally paginated", responses = {
                        @ApiResponse(responseCode = "200", description = "Product attribute values retrieved successfully")
        })
        public ResponseEntity<?> getAllAttributeValues(
                        @Parameter(description = "Page number (0-based)") @RequestParam(required = false) Integer page,
                        @Parameter(description = "Page size") @RequestParam(required = false) Integer size,
                        @Parameter(description = "Sort field") @RequestParam(defaultValue = "value") String sort,
                        @Parameter(description = "Sort direction (ASC or DESC)") @RequestParam(defaultValue = "ASC") String direction) {

                if (page != null && size != null) {
                        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
                        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                        Page<ProductAttributeValueDTO> attributeValues = productAttributeValueService
                                        .getAllAttributeValues(pageable);
                        return ResponseEntity.ok(attributeValues);
                } else {
                        List<ProductAttributeValueDTO> attributeValues = productAttributeValueService
                                        .getAllAttributeValues();
                        return ResponseEntity.ok(attributeValues);
                }
        }

        @GetMapping("/search")
        @Operation(summary = "Search product attribute values by value", description = "Searches for product attribute values by value with pagination", responses = {
                        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
        })
        public ResponseEntity<Page<ProductAttributeValueDTO>> searchAttributeValues(
                        @Parameter(description = "Value to search for") @RequestParam String value,
                        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
                        @Parameter(description = "Sort field") @RequestParam(defaultValue = "value") String sort,
                        @Parameter(description = "Sort direction (ASC or DESC)") @RequestParam(defaultValue = "ASC") String direction) {

                Sort.Direction sortDirection = Sort.Direction.fromString(direction);
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                Page<ProductAttributeValueDTO> searchResults = productAttributeValueService
                                .searchAttributeValuesByValue(value, pageable);
                return ResponseEntity.ok(searchResults);
        }

        @GetMapping("/search/type/{attributeTypeId}")
        @Operation(summary = "Search product attribute values by value and type", description = "Searches for product attribute values by value and attribute type with pagination", responses = {
                        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Product attribute type not found")
        })
        public ResponseEntity<Page<ProductAttributeValueDTO>> searchAttributeValuesByValueAndTypeId(
                        @Parameter(description = "Value to search for") @RequestParam String value,
                        @Parameter(description = "ID of the attribute type") @PathVariable Long attributeTypeId,
                        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
                        @Parameter(description = "Sort field") @RequestParam(defaultValue = "value") String sort,
                        @Parameter(description = "Sort direction (ASC or DESC)") @RequestParam(defaultValue = "ASC") String direction) {

                Sort.Direction sortDirection = Sort.Direction.fromString(direction);
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                Page<ProductAttributeValueDTO> searchResults = productAttributeValueService
                                .searchAttributeValuesByValueAndTypeId(value, attributeTypeId, pageable);
                return ResponseEntity.ok(searchResults);
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
        @Operation(summary = "Delete a product attribute value", description = "Deletes a product attribute value by its ID", responses = {
                        @ApiResponse(responseCode = "204", description = "Product attribute value deleted successfully"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or EMPLOYEE role"),
                        @ApiResponse(responseCode = "404", description = "Product attribute value not found"),
                        @ApiResponse(responseCode = "409", description = "Product attribute value is in use and cannot be deleted")
        })
        public ResponseEntity<Void> deleteAttributeValue(
                        @Parameter(description = "ID of the product attribute value to delete") @PathVariable Long id) {
                boolean deleted = productAttributeValueService.deleteAttributeValue(id);
                return deleted ? ResponseEntity.noContent().build()
                                : ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        @GetMapping("/{id}/in-use")
        @Operation(summary = "Check if a product attribute value is in use", description = "Checks if a product attribute value is currently in use", responses = {
                        @ApiResponse(responseCode = "200", description = "Check completed successfully"),
                        @ApiResponse(responseCode = "404", description = "Product attribute value not found")
        })
        public ResponseEntity<Boolean> isAttributeValueInUse(
                        @Parameter(description = "ID of the product attribute value to check") @PathVariable Long id) {
                boolean inUse = productAttributeValueService.isAttributeValueInUse(id);
                return ResponseEntity.ok(inUse);
        }

        @GetMapping("/type/{attributeTypeId}/value/{value}")
        @Operation(summary = "Get a product attribute value by value and type ID", description = "Retrieves a product attribute value by its value and attribute type ID", responses = {
                        @ApiResponse(responseCode = "200", description = "Product attribute value retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Product attribute value not found")
        })
        public ResponseEntity<ProductAttributeValueDTO> getAttributeValueByValueAndTypeId(
                        @Parameter(description = "Value of the product attribute") @PathVariable String value,
                        @Parameter(description = "ID of the attribute type") @PathVariable Long attributeTypeId) {
                ProductAttributeValueDTO attributeValue = productAttributeValueService
                                .getAttributeValueByValueAndTypeId(value, attributeTypeId);
                return ResponseEntity.ok(attributeValue);
        }
}