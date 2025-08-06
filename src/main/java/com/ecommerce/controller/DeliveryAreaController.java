package com.ecommerce.controller;

import com.ecommerce.dto.DeliveryAreaDTO;
import com.ecommerce.service.DeliveryAreaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-areas")
public class DeliveryAreaController {

    private final DeliveryAreaService deliveryAreaService;

    @Autowired
    public DeliveryAreaController(DeliveryAreaService deliveryAreaService) {
        this.deliveryAreaService = deliveryAreaService;
    }

    @PostMapping
    public ResponseEntity<DeliveryAreaDTO> createDeliveryArea(@Valid @RequestBody DeliveryAreaDTO deliveryAreaDTO) {
        DeliveryAreaDTO createdDeliveryArea = deliveryAreaService.createDeliveryArea(deliveryAreaDTO);
        return new ResponseEntity<>(createdDeliveryArea, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryAreaDTO> updateDeliveryArea(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryAreaDTO deliveryAreaDTO) {
        DeliveryAreaDTO updatedDeliveryArea = deliveryAreaService.updateDeliveryArea(id, deliveryAreaDTO);
        return ResponseEntity.ok(updatedDeliveryArea);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeliveryArea(@PathVariable Long id) {
        deliveryAreaService.deleteDeliveryArea(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryAreaDTO> getDeliveryAreaById(@PathVariable Long id) {
        DeliveryAreaDTO deliveryAreaDTO = deliveryAreaService.getDeliveryAreaById(id);
        return ResponseEntity.ok(deliveryAreaDTO);
    }

    @GetMapping
    public ResponseEntity<List<DeliveryAreaDTO>> getAllDeliveryAreas() {
        List<DeliveryAreaDTO> deliveryAreas = deliveryAreaService.getAllDeliveryAreas();
        return ResponseEntity.ok(deliveryAreas);
    }

    @GetMapping("/top-level")
    public ResponseEntity<List<DeliveryAreaDTO>> getTopLevelDeliveryAreas() {
        List<DeliveryAreaDTO> topLevelAreas = deliveryAreaService.getTopLevelDeliveryAreas();
        return ResponseEntity.ok(topLevelAreas);
    }

    @GetMapping("/sub-areas/{parentId}")
    public ResponseEntity<List<DeliveryAreaDTO>> getSubAreas(@PathVariable Long parentId) {
        List<DeliveryAreaDTO> subAreas = deliveryAreaService.getSubAreas(parentId);
        return ResponseEntity.ok(subAreas);
    }
}