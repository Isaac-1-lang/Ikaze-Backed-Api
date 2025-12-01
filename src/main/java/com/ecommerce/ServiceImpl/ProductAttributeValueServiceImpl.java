package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.ProductAttributeValueDTO;
import com.ecommerce.dto.ProductAttributeValueRequestDTO;
import com.ecommerce.entity.ProductAttributeType;
import com.ecommerce.entity.ProductAttributeValue;
import com.ecommerce.repository.ProductAttributeTypeRepository;
import com.ecommerce.repository.ProductAttributeValueRepository;
import com.ecommerce.service.ProductAttributeValueService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductAttributeValueServiceImpl implements ProductAttributeValueService {

    private final ProductAttributeValueRepository attributeValueRepository;
    private final ProductAttributeTypeRepository attributeTypeRepository;

    @Autowired
    public ProductAttributeValueServiceImpl(ProductAttributeValueRepository attributeValueRepository,
            ProductAttributeTypeRepository attributeTypeRepository) {
        this.attributeValueRepository = attributeValueRepository;
        this.attributeTypeRepository = attributeTypeRepository;
    }

    @Override
    @Transactional
    public ProductAttributeValueDTO createAttributeValue(ProductAttributeValueRequestDTO requestDTO) {
        // Find the attribute type
        ProductAttributeType attributeType = attributeTypeRepository.findById(requestDTO.getAttributeTypeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Attribute type not found with id: " + requestDTO.getAttributeTypeId()));

        // Check if attribute value with the same value and type already exists
        if (attributeValueRepository.existsByValueIgnoreCaseAndAttributeType(requestDTO.getValue(), attributeType)) {
            throw new IllegalArgumentException("Attribute value '" + requestDTO.getValue()
                    + "' already exists for type '" + attributeType.getName() + "'");
        }

        // Create new attribute value
        ProductAttributeValue attributeValue = new ProductAttributeValue();
        attributeValue.setValue(requestDTO.getValue());
        attributeValue.setAttributeType(attributeType);

        ProductAttributeValue savedAttributeValue = attributeValueRepository.save(attributeValue);

        return convertToDTO(savedAttributeValue);
    }

    @Override
    @Transactional
    public ProductAttributeValueDTO updateAttributeValue(Long id, ProductAttributeValueRequestDTO requestDTO) {
        // Find the attribute value to update
        ProductAttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attribute value not found with id: " + id));

        // Find the attribute type if it's being changed
        ProductAttributeType attributeType = attributeValue.getAttributeType();
        if (!attributeValue.getAttributeType().getAttributeTypeId().equals(requestDTO.getAttributeTypeId())) {
            attributeType = attributeTypeRepository.findById(requestDTO.getAttributeTypeId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Attribute type not found with id: " + requestDTO.getAttributeTypeId()));
        }

        // Check if the new value conflicts with an existing attribute value (excluding
        // the current one)
        if ((!attributeValue.getValue().equalsIgnoreCase(requestDTO.getValue()) ||
                !attributeValue.getAttributeType().getAttributeTypeId().equals(requestDTO.getAttributeTypeId())) &&
                attributeValueRepository.existsByValueIgnoreCaseAndAttributeType(requestDTO.getValue(),
                        attributeType)) {
            throw new IllegalArgumentException("Attribute value '" + requestDTO.getValue()
                    + "' already exists for type '" + attributeType.getName() + "'");
        }

        // Update the attribute value
        attributeValue.setValue(requestDTO.getValue());
        attributeValue.setAttributeType(attributeType);

        ProductAttributeValue updatedAttributeValue = attributeValueRepository.save(attributeValue);

        return convertToDTO(updatedAttributeValue);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeValueDTO getAttributeValueById(Long id) {
        ProductAttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attribute value not found with id: " + id));

        return convertToDTO(attributeValue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeValueDTO> getAttributeValuesByTypeId(Long attributeTypeId) {
        ProductAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Attribute type not found with id: " + attributeTypeId));

        List<ProductAttributeValue> attributeValues = attributeValueRepository.findByAttributeType(attributeType);

        return attributeValues.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductAttributeValueDTO> getAttributeValuesByTypeId(Long attributeTypeId, Pageable pageable) {
        ProductAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Attribute type not found with id: " + attributeTypeId));

        Page<ProductAttributeValue> attributeValuesPage = attributeValueRepository.findByAttributeType(attributeType,
                pageable);

        return attributeValuesPage.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeValueDTO> getAllAttributeValues() {
        List<ProductAttributeValue> attributeValues = attributeValueRepository.findAll();

        return attributeValues.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductAttributeValueDTO> getAllAttributeValues(Pageable pageable) {
        Page<ProductAttributeValue> attributeValuesPage = attributeValueRepository.findAll(pageable);

        return attributeValuesPage.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductAttributeValueDTO> searchAttributeValuesByValue(String value, Pageable pageable) {
        Page<ProductAttributeValue> attributeValuesPage = attributeValueRepository
                .findByValueContainingIgnoreCase(value, pageable);

        return attributeValuesPage.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductAttributeValueDTO> searchAttributeValuesByValueAndTypeId(String value, Long attributeTypeId,
            Pageable pageable) {
        ProductAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Attribute type not found with id: " + attributeTypeId));

        Page<ProductAttributeValue> attributeValuesPage = attributeValueRepository
                .findByValueContainingIgnoreCaseAndAttributeType(value, attributeType, pageable);

        return attributeValuesPage.map(this::convertToDTO);
    }

    @Override
    @Transactional
    public boolean deleteAttributeValue(Long id) {
        // Check if the attribute value exists
        if (!attributeValueRepository.existsById(id)) {
            throw new EntityNotFoundException("Attribute value not found with id: " + id);
        }

        // Check if the attribute value is in use
        if (isAttributeValueInUse(id)) {
            throw new IllegalStateException("Cannot delete attribute value as it is in use by product variants");
        }

        // Delete the attribute value
        attributeValueRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean attributeValueExists(String value, Long attributeTypeId) {
        ProductAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Attribute type not found with id: " + attributeTypeId));

        return attributeValueRepository.existsByValueIgnoreCaseAndAttributeType(value, attributeType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAttributeValueInUse(Long id) {
        return attributeValueRepository.isAttributeValueInUse(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeValueDTO getAttributeValueByValueAndTypeId(String value, Long attributeTypeId) {
        ProductAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Attribute type not found with id: " + attributeTypeId));

        ProductAttributeValue attributeValue = attributeValueRepository
                .findByValueIgnoreCaseAndAttributeType(value, attributeType)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Attribute value '" + value + "' not found for type with id: " + attributeTypeId));

        return convertToDTO(attributeValue);
    }

    /**
     * Convert a ProductAttributeValue entity to a ProductAttributeValueDTO
     *
     * @param attributeValue the entity to convert
     * @return the converted DTO
     */
    private ProductAttributeValueDTO convertToDTO(ProductAttributeValue attributeValue) {
        // Calculate product count from variant attribute values
        long productCount = 0L;
        if (attributeValue.getVariantAttributeValues() != null) {
            productCount = attributeValue.getVariantAttributeValues().stream()
                    .map(vav -> vav.getProductVariant().getProduct().getProductId())
                    .distinct()
                    .count();
        }
        
        return ProductAttributeValueDTO.builder()
                .attributeValueId(attributeValue.getAttributeValueId())
                .value(attributeValue.getValue())
                .attributeTypeId(attributeValue.getAttributeType().getAttributeTypeId())
                .attributeTypeName(attributeValue.getAttributeType().getName())
                .productCount(productCount)
                .build();
    }
}