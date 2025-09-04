package com.ecommerce.controller;

import com.ecommerce.dto.LandingPageDataDTO;
import com.ecommerce.service.LandingPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/landing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Landing Page", description = "APIs for landing page data")
public class LandingPageController {

    private final LandingPageService landingPageService;

    @GetMapping
    @Operation(summary = "Get all landing page data", description = "Retrieve all data needed for the landing page including top-selling products, new products, discounted products, popular categories, and popular brands", responses = {
            @ApiResponse(responseCode = "200", description = "Landing page data retrieved successfully", content = @Content(schema = @Schema(implementation = LandingPageDataDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getLandingPageData() {
        try {
            log.info("Fetching landing page data");
            LandingPageDataDTO landingPageData = landingPageService.getLandingPageData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Landing page data retrieved successfully");
            response.put("data", landingPageData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching landing page data", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve landing page data");
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/top-selling")
    @Operation(summary = "Get top-selling products", description = "Retrieve top-selling products based on ratings and sales", responses = {
            @ApiResponse(responseCode = "200", description = "Top-selling products retrieved successfully", content = @Content(schema = @Schema(implementation = LandingPageDataDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getTopSellingProducts(
            @RequestParam(defaultValue = "8") int limit) {
        try {
            log.info("Fetching top-selling products with limit: {}", limit);
            LandingPageDataDTO data = landingPageService.getTopSellingProducts(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Top-selling products retrieved successfully");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching top-selling products", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve top-selling products");
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/new-products")
    @Operation(summary = "Get new products", description = "Retrieve newest products", responses = {
            @ApiResponse(responseCode = "200", description = "New products retrieved successfully", content = @Content(schema = @Schema(implementation = LandingPageDataDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getNewProducts(
            @RequestParam(defaultValue = "8") int limit) {
        try {
            log.info("Fetching new products with limit: {}", limit);
            LandingPageDataDTO data = landingPageService.getNewProducts(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "New products retrieved successfully");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching new products", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve new products");
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/discounted")
    @Operation(summary = "Get discounted products", description = "Retrieve products with discounts", responses = {
            @ApiResponse(responseCode = "200", description = "Discounted products retrieved successfully", content = @Content(schema = @Schema(implementation = LandingPageDataDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getDiscountedProducts(
            @RequestParam(defaultValue = "8") int limit) {
        try {
            log.info("Fetching discounted products with limit: {}", limit);
            LandingPageDataDTO data = landingPageService.getDiscountedProducts(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Discounted products retrieved successfully");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching discounted products", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve discounted products");
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/categories")
    @Operation(summary = "Get popular categories", description = "Retrieve popular categories with product counts", responses = {
            @ApiResponse(responseCode = "200", description = "Popular categories retrieved successfully", content = @Content(schema = @Schema(implementation = LandingPageDataDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getPopularCategories(
            @RequestParam(defaultValue = "8") int limit) {
        try {
            log.info("Fetching popular categories with limit: {}", limit);
            LandingPageDataDTO data = landingPageService.getPopularCategories(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Popular categories retrieved successfully");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching popular categories", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve popular categories");
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/brands")
    @Operation(summary = "Get popular brands", description = "Retrieve popular brands with product counts", responses = {
            @ApiResponse(responseCode = "200", description = "Popular brands retrieved successfully", content = @Content(schema = @Schema(implementation = LandingPageDataDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getPopularBrands(
            @RequestParam(defaultValue = "6") int limit) {
        try {
            log.info("Fetching popular brands with limit: {}", limit);
            LandingPageDataDTO data = landingPageService.getPopularBrands(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Popular brands retrieved successfully");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching popular brands", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve popular brands");
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
