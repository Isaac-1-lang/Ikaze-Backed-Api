package com.ecommerce.controller;

import com.ecommerce.dto.CreateDiscountDTO;
import com.ecommerce.dto.DiscountDTO;
import com.ecommerce.dto.UpdateDiscountDTO;
import com.ecommerce.service.DiscountService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
@Slf4j
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<DiscountDTO> createDiscount(@Valid @RequestBody CreateDiscountDTO createDiscountDTO) {
        log.info("Creating new discount: {}", createDiscountDTO.getName());

        try {
            DiscountDTO createdDiscount = discountService.createDiscount(createDiscountDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDiscount);
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for discount creation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating discount: {}", e.getMessage());
            throw e;
        }
    }

    @PutMapping("/{discountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<DiscountDTO> updateDiscount(@PathVariable UUID discountId,
            @Valid @RequestBody UpdateDiscountDTO updateDiscountDTO) {
        log.info("Updating discount with ID: {}", discountId);

        try {
            DiscountDTO updatedDiscount = discountService.updateDiscount(discountId, updateDiscountDTO);
            return ResponseEntity.ok(updatedDiscount);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with ID: {}", discountId);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for discount update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating discount: {}", e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{discountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Map<String, String>> deleteDiscount(@PathVariable UUID discountId) {
        log.info("Deleting discount with ID: {}", discountId);

        try {
            boolean deleted = discountService.deleteDiscount(discountId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Discount deleted successfully");
            response.put("discountId", discountId.toString());
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with ID: {}", discountId);
            throw e;
        } catch (Exception e) {
            log.error("Error deleting discount: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{discountId}")
    public ResponseEntity<DiscountDTO> getDiscountById(@PathVariable UUID discountId) {
        log.info("Fetching discount with ID: {}", discountId);

        try {
            DiscountDTO discount = discountService.getDiscountById(discountId);
            return ResponseEntity.ok(discount);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with ID: {}", discountId);
            throw e;
        } catch (Exception e) {
            log.error("Error fetching discount: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<Page<DiscountDTO>> getAllDiscounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        log.info("Fetching discounts - page: {}, size: {}, sortBy: {}, sortDirection: {}, activeOnly: {}",
                page, size, sortBy, sortDirection, activeOnly);

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<DiscountDTO> discounts;
            if (activeOnly) {
                discounts = discountService.getActiveDiscounts(pageable);
            } else {
                discounts = discountService.getAllDiscounts(pageable);
            }

            return ResponseEntity.ok(discounts);
        } catch (Exception e) {
            log.error("Error fetching discounts: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/code/{discountCode}")
    public ResponseEntity<DiscountDTO> getDiscountByCode(@PathVariable String discountCode) {
        log.info("Fetching discount by code: {}", discountCode);

        try {
            DiscountDTO discount = discountService.getDiscountByCode(discountCode);
            return ResponseEntity.ok(discount);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with code: {}", discountCode);
            throw e;
        } catch (Exception e) {
            log.error("Error fetching discount by code: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{discountId}/valid")
    public ResponseEntity<Map<String, Object>> isDiscountValid(@PathVariable UUID discountId) {
        log.info("Checking if discount is valid with ID: {}", discountId);

        try {
            boolean isValid = discountService.isDiscountValid(discountId);
            Map<String, Object> response = new HashMap<>();
            response.put("discountId", discountId.toString());
            response.put("isValid", isValid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking discount validity: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/code/{discountCode}/valid")
    public ResponseEntity<Map<String, Object>> isDiscountCodeValid(@PathVariable String discountCode) {
        log.info("Checking if discount code is valid: {}", discountCode);

        try {
            boolean isValid = discountService.isDiscountCodeValid(discountCode);
            Map<String, Object> response = new HashMap<>();
            response.put("discountCode", discountCode);
            response.put("isValid", isValid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking discount code validity: {}", e.getMessage());
            throw e;
        }
    }
}
