package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.ProductAttributeTypeDTO;
import com.ecommerce.dto.ProductAttributeTypeRequestDTO;
import com.ecommerce.entity.ProductAttributeType;
import com.ecommerce.repository.ProductAttributeTypeRepository;
import com.ecommerce.service.ProductAttributeTypeService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductAttributeTypeServiceImpl implements ProductAttributeTypeService {

    private final ProductAttributeTypeRepository attributeTypeRepository;

    @Autowired
    public ProductAttributeTypeServiceImpl(ProductAttributeTypeRepository attributeTypeRepository) {
        this.attributeTypeRepository = attributeTypeRepository;
    }

    @Override
    @Transactional
    public ProductAttributeTypeDTO createAttributeType(ProductAttributeTypeRequestDTO requestDTO) {
        // Check if attribute type with the same name already exists
        if (attributeTypeRepository.existsByNameIgnoreCase(requestDTO.getName())) {
            throw new IllegalArgumentException(
                    "Attribute type with name '" + requestDTO.getName() + "' already exists");
        }

        // Create new attribute type
        ProductAttributeType attributeType = new ProductAttributeType();
        attributeType.setName(requestDTO.getName());
        attributeType.setRequired(requestDTO.isRequired());

        ProductAttributeType savedAttributeType = attributeTypeRepository.save(attributeType);

        return convertToDTO(savedAttributeType);
    }

    @Override
    @Transactional
    public ProductAttributeTypeDTO updateAttributeType(Long id, ProductAttributeTypeRequestDTO requestDTO) {
        // Find the attribute type to update
        ProductAttributeType attributeType = attributeTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attribute type not found with id: " + id));

        // Check if the new name conflicts with an existing attribute type (excluding
        // the current one)
        if (!attributeType.getName().equalsIgnoreCase(requestDTO.getName()) &&
                attributeTypeRepository.existsByNameIgnoreCase(requestDTO.getName())) {
            throw new IllegalArgumentException(
                    "Attribute type with name '" + requestDTO.getName() + "' already exists");
        }

        // Update the attribute type
        attributeType.setName(requestDTO.getName());
        attributeType.setRequired(requestDTO.isRequired());

        ProductAttributeType updatedAttributeType = attributeTypeRepository.save(attributeType);

        return convertToDTO(updatedAttributeType);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeTypeDTO getAttributeTypeById(Long id) {
        ProductAttributeType attributeType = attributeTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attribute type not found with id: " + id));

        return convertToDTO(attributeType);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeTypeDTO getAttributeTypeByName(String name) {
        ProductAttributeType attributeType = attributeTypeRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new EntityNotFoundException("Attribute type not found with name: " + name));

        return convertToDTO(attributeType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeTypeDTO> getAllAttributeTypes() {
        List<ProductAttributeType> attributeTypes = attributeTypeRepository.findAll();

        return attributeTypes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductAttributeTypeDTO> getAllAttributeTypes(Pageable pageable) {
        Page<ProductAttributeType> attributeTypesPage = attributeTypeRepository.findAll(pageable);

        return attributeTypesPage.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductAttributeTypeDTO> searchAttributeTypesByName(String name, Pageable pageable) {
        Page<ProductAttributeType> attributeTypesPage = attributeTypeRepository.findByNameContainingIgnoreCase(name,
                pageable);

        return attributeTypesPage.map(this::convertToDTO);
    }

    @Override
    @Transactional
    public boolean deleteAttributeType(Long id) {
        // Check if the attribute type exists
        if (!attributeTypeRepository.existsById(id)) {
            throw new EntityNotFoundException("Attribute type not found with id: " + id);
        }

        // Check if the attribute type is in use
        if (isAttributeTypeInUse(id)) {
            throw new IllegalStateException("Cannot delete attribute type as it is in use by product variants");
        }

        // Delete the attribute type
        attributeTypeRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean attributeTypeExists(String name) {
        return attributeTypeRepository.existsByNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAttributeTypeInUse(Long id) {
        return attributeTypeRepository.isAttributeTypeInUse(id);
    }

    /**
     * Convert a ProductAttributeType entity to a ProductAttributeTypeDTO
     *
     * @param attributeType the entity to convert
     * @return the converted DTO
     */
    private ProductAttributeTypeDTO convertToDTO(ProductAttributeType attributeType) {
        return ProductAttributeTypeDTO.builder()
                .attributeTypeId(attributeType.getAttributeTypeId())
                .name(attributeType.getName())
                .isRequired(attributeType.isRequired())
                .build();
    }

    /**
     * Convert a ProductAttributeTypeDTO to a ProductAttributeType entity
     *
     * @param attributeTypeDTO the DTO to convert
     * @return the converted entity
     */
    private ProductAttributeType convertToEntity(ProductAttributeTypeDTO attributeTypeDTO) {
        ProductAttributeType attributeType = new ProductAttributeType();
        attributeType.setName(attributeTypeDTO.getName());
        attributeType.setRequired(attributeTypeDTO.isRequired());
        return attributeType;
    }
}