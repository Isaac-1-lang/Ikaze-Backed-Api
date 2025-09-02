package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.DeliveryAreaDTO;
import com.ecommerce.entity.DeliveryArea;
import com.ecommerce.repository.DeliveryAreaRepository;
import com.ecommerce.service.DeliveryAreaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeliveryAreaServiceImpl implements DeliveryAreaService {

    private final DeliveryAreaRepository deliveryAreaRepository;

    @Autowired
    public DeliveryAreaServiceImpl(DeliveryAreaRepository deliveryAreaRepository) {
        this.deliveryAreaRepository = deliveryAreaRepository;
    }

    @Override
    public DeliveryAreaDTO createDeliveryArea(DeliveryAreaDTO deliveryAreaDTO) {
        DeliveryArea deliveryArea = convertToEntity(deliveryAreaDTO);
        DeliveryArea savedDeliveryArea = deliveryAreaRepository.save(deliveryArea);
        return convertToDTO(savedDeliveryArea);
    }

    @Override
    public DeliveryAreaDTO updateDeliveryArea(Long id, DeliveryAreaDTO deliveryAreaDTO) {
        DeliveryArea existingDeliveryArea = deliveryAreaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Delivery area not found with id: " + id));

        existingDeliveryArea.setDeliveryAreaName(deliveryAreaDTO.getDeliveryAreaName());
        existingDeliveryArea.setDeliveryCost(deliveryAreaDTO.getDeliveryCost());
        existingDeliveryArea.setExpectedDeliveryMinDays(deliveryAreaDTO.getExpectedDeliveryMinDays());
        existingDeliveryArea.setExpectedDeliveryMaxDays(deliveryAreaDTO.getExpectedDeliveryMaxDays());

        // Update parent if changed
        if (deliveryAreaDTO.getParentId() != null) {
            DeliveryArea parent = deliveryAreaRepository.findById(deliveryAreaDTO.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent delivery area not found with id: " + deliveryAreaDTO.getParentId()));
            existingDeliveryArea.setParent(parent);
        } else {
            existingDeliveryArea.setParent(null);
        }

        DeliveryArea updatedDeliveryArea = deliveryAreaRepository.save(existingDeliveryArea);
        return convertToDTO(updatedDeliveryArea);
    }

    @Override
    public void deleteDeliveryArea(Long id) {
        DeliveryArea deliveryArea = deliveryAreaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Delivery area not found with id: " + id));

        // Check if the delivery area has children
        if (!deliveryArea.getChildren().isEmpty()) {
            throw new IllegalStateException("Cannot delete delivery area with sub-areas. Delete sub-areas first.");
        }

        deliveryAreaRepository.delete(deliveryArea);
    }

    @Override
    public DeliveryAreaDTO getDeliveryAreaById(Long id) {
        DeliveryArea deliveryArea = deliveryAreaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Delivery area not found with id: " + id));
        return convertToDTO(deliveryArea);
    }

    @Override
    public List<DeliveryAreaDTO> getAllDeliveryAreas() {
        List<DeliveryArea> deliveryAreas = deliveryAreaRepository.findAll();
        return deliveryAreas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeliveryAreaDTO> getTopLevelDeliveryAreas() {
        List<DeliveryArea> topLevelAreas = deliveryAreaRepository.findByParentIsNull();
        return topLevelAreas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeliveryAreaDTO> getSubAreas(Long parentId) {
        DeliveryArea parent = deliveryAreaRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Parent delivery area not found with id: " + parentId));

        return parent.getChildren().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeliveryAreaDTO> searchDeliveryAreas(String query) {
        List<DeliveryArea> deliveryAreas = deliveryAreaRepository.findByDeliveryAreaNameContainingIgnoreCase(query);
        return deliveryAreas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DeliveryAreaDTO convertToDTO(DeliveryArea deliveryArea) {
        DeliveryAreaDTO dto = new DeliveryAreaDTO();
        dto.setDeliveryAreaId(deliveryArea.getDeliveryAreaId());
        dto.setDeliveryAreaName(deliveryArea.getDeliveryAreaName());
        dto.setCreatedAt(deliveryArea.getCreatedAt());
        dto.setLevel(deliveryArea.getLevel());
        dto.setDeliveryCost(deliveryArea.getDeliveryCost());
        dto.setExpectedDeliveryMinDays(deliveryArea.getExpectedDeliveryMinDays());
        dto.setExpectedDeliveryMaxDays(deliveryArea.getExpectedDeliveryMaxDays());

        if (deliveryArea.getParent() != null) {
            dto.setParentId(deliveryArea.getParent().getDeliveryAreaId());
            dto.setParentName(deliveryArea.getParent().getDeliveryAreaName());
        }

        // Only set immediate children, not all descendants
        List<DeliveryAreaDTO> childrenDTOs = deliveryArea.getChildren().stream()
                .map(child -> {
                    DeliveryAreaDTO childDTO = new DeliveryAreaDTO();
                    childDTO.setDeliveryAreaId(child.getDeliveryAreaId());
                    childDTO.setDeliveryAreaName(child.getDeliveryAreaName());
                    childDTO.setCreatedAt(child.getCreatedAt());
                    childDTO.setParentId(deliveryArea.getDeliveryAreaId());
                    childDTO.setParentName(deliveryArea.getDeliveryAreaName());
                    childDTO.setLevel(child.getLevel());
                    childDTO.setDeliveryCost(child.getDeliveryCost());
                    childDTO.setExpectedDeliveryMinDays(child.getExpectedDeliveryMinDays());
                    childDTO.setExpectedDeliveryMaxDays(child.getExpectedDeliveryMaxDays());
                    return childDTO;
                })
                .collect(Collectors.toList());

        dto.setChildren(childrenDTOs);

        return dto;
    }

    @Override
    public DeliveryArea convertToEntity(DeliveryAreaDTO deliveryAreaDTO) {
        DeliveryArea deliveryArea = new DeliveryArea();
        deliveryArea.setDeliveryAreaName(deliveryAreaDTO.getDeliveryAreaName());
        deliveryArea.setDeliveryCost(deliveryAreaDTO.getDeliveryCost());
        deliveryArea.setExpectedDeliveryMinDays(deliveryAreaDTO.getExpectedDeliveryMinDays());
        deliveryArea.setExpectedDeliveryMaxDays(deliveryAreaDTO.getExpectedDeliveryMaxDays());

        if (deliveryAreaDTO.getDeliveryAreaId() != null) {
            deliveryArea.setDeliveryAreaId(deliveryAreaDTO.getDeliveryAreaId());
        }

        if (deliveryAreaDTO.getParentId() != null) {
            DeliveryArea parent = deliveryAreaRepository.findById(deliveryAreaDTO.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent delivery area not found with id: " + deliveryAreaDTO.getParentId()));
            deliveryArea.setParent(parent);
        }

        return deliveryArea;
    }
}