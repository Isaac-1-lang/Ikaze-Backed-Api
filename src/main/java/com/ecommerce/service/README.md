# Product Attribute Services

This document provides an overview of the Product Attribute Type and Value services implemented in the e-commerce application.

## Overview

The services handle the management of product attributes, which are used to define variants of products. For example, a T-shirt product might have attributes like "Color" and "Size", with values like "Red", "Blue", "M", "L", etc.

## Service Interfaces

### ProductAttributeTypeService

Manages attribute types (e.g., "Color", "Size").

```java
public interface ProductAttributeTypeService {
    ProductAttributeTypeDTO createAttributeType(ProductAttributeTypeRequestDTO requestDTO);
    ProductAttributeTypeDTO updateAttributeType(Long id, ProductAttributeTypeRequestDTO requestDTO);
    ProductAttributeTypeDTO getAttributeTypeById(Long id);
    ProductAttributeTypeDTO getAttributeTypeByName(String name);
    List<ProductAttributeTypeDTO> getAllAttributeTypes();
    Page<ProductAttributeTypeDTO> getAllAttributeTypes(Pageable pageable);
    Page<ProductAttributeTypeDTO> searchAttributeTypesByName(String name, Pageable pageable);
    boolean deleteAttributeType(Long id);
    boolean attributeTypeExists(String name);
    boolean isAttributeTypeInUse(Long id);
}
```

### ProductAttributeValueService

Manages attribute values (e.g., "Red", "Blue", "M", "L").

```java
public interface ProductAttributeValueService {
    ProductAttributeValueDTO createAttributeValue(ProductAttributeValueRequestDTO requestDTO);
    ProductAttributeValueDTO updateAttributeValue(Long id, ProductAttributeValueRequestDTO requestDTO);
    ProductAttributeValueDTO getAttributeValueById(Long id);
    List<ProductAttributeValueDTO> getAttributeValuesByTypeId(Long attributeTypeId);
    Page<ProductAttributeValueDTO> getAttributeValuesByTypeId(Long attributeTypeId, Pageable pageable);
    List<ProductAttributeValueDTO> getAllAttributeValues();
    Page<ProductAttributeValueDTO> getAllAttributeValues(Pageable pageable);
    Page<ProductAttributeValueDTO> searchAttributeValuesByValue(String value, Pageable pageable);
    Page<ProductAttributeValueDTO> searchAttributeValuesByValueAndTypeId(String value, Long attributeTypeId, Pageable pageable);
    boolean deleteAttributeValue(Long id);
    boolean attributeValueExists(String value, Long attributeTypeId);
    boolean isAttributeValueInUse(Long id);
    ProductAttributeValueDTO getAttributeValueByValueAndTypeId(String value, Long attributeTypeId);
}
```

## DTOs

### ProductAttributeTypeDTO

Represents a product attribute type with its values.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeTypeDTO {
    private Long attributeTypeId;

    @NotBlank(message = "Name is required")
    private String name;

    private boolean isRequired;

    private List<ProductAttributeValueDTO> attributeValues;
}
```

### ProductAttributeTypeRequestDTO

Used for creating or updating a product attribute type.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeTypeRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;

    private boolean isRequired;
}
```

### ProductAttributeValueDTO

Represents a product attribute value.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeValueDTO {
    private Long attributeValueId;

    @NotBlank(message = "Value is required")
    private String value;

    private Long attributeTypeId;

    private String attributeTypeName;
}
```

### ProductAttributeValueRequestDTO

Used for creating or updating a product attribute value.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeValueRequestDTO {
    @NotBlank(message = "Value is required")
    private String value;

    @NotNull(message = "Attribute type ID is required")
    private Long attributeTypeId;
}
```

## Key Features

1. **CRUD Operations**: Full create, read, update, and delete functionality for both attribute types and values.

2. **Validation**:

   - No duplicate attribute types (e.g., only one "Color" type system-wide)
   - No duplicate attribute values per type (e.g., only one "Red" under "Color")
   - Cannot delete attribute types or values that are in use by product variants

3. **Search and Pagination**:

   - Search attribute types by name
   - Search attribute values by value
   - Search attribute values by value and type
   - Paginated results for all list operations

4. **Reuse**:
   - Ability to find existing attribute types and values for reuse when creating product variants

## Use Case Example

When an admin is adding a new product with variants:

1. Check if attribute types ("Color", "Size") exist using `attributeTypeExists` or `getAttributeTypeByName`
2. If not, create them using `createAttributeType`
3. Check if attribute values ("Red", "Blue", "M", "L") exist using `attributeValueExists` or `getAttributeValueByValueAndTypeId`
4. If not, add them using `createAttributeValue`
5. Use these values when creating ProductVariant entries (e.g., "Red, M", "Red, L", "Blue, M")

## Implementation Notes

- All service methods are transactional to ensure data consistency
- Read-only operations are marked with `@Transactional(readOnly = true)` for performance
- Proper exception handling for not found entities and validation errors
- Case-insensitive searches for better user experience
