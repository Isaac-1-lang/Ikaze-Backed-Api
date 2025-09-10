package com.ecommerce.service.impl;

import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.LandingPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LandingPageServiceImpl implements LandingPageService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Override
    public LandingPageDataDTO getLandingPageData() {
        log.info("Generating landing page data");

        try {
            // Get all the required data in parallel for better performance
            List<LandingPageProductDTO> topSellingProducts = getTopSellingProductsList(8);
            List<LandingPageProductDTO> newProducts = getNewProductsList(8);
            List<LandingPageProductDTO> discountedProducts = getDiscountedProductsList(8);
            List<LandingPageCategoryDTO> popularCategories = getPopularCategoriesList(8);
            List<LandingPageBrandDTO> popularBrands = getPopularBrandsList(6);

            // Get statistics
            long totalProducts = productRepository.count();
            long totalCategories = categoryRepository.count();
            long totalBrands = brandRepository.count();
            long totalActiveProducts = productRepository.countActive();

            return LandingPageDataDTO.builder()
                    .topSellingProducts(topSellingProducts)
                    .newProducts(newProducts)
                    .discountedProducts(discountedProducts)
                    .popularCategories(popularCategories)
                    .popularBrands(popularBrands)
                    .totalProducts(totalProducts)
                    .totalCategories(totalCategories)
                    .totalBrands(totalBrands)
                    .totalActiveProducts(totalActiveProducts)
                    .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .version("1.0")
                    .build();

        } catch (Exception e) {
            log.error("Error generating landing page data", e);
            throw new RuntimeException("Failed to generate landing page data", e);
        }
    }

    @Override
    public LandingPageDataDTO getTopSellingProducts(int limit) {
        List<LandingPageProductDTO> products = getTopSellingProductsList(limit);
        return LandingPageDataDTO.builder()
                .topSellingProducts(products)
                .build();
    }

    @Override
    public LandingPageDataDTO getNewProducts(int limit) {
        List<LandingPageProductDTO> products = getNewProductsList(limit);
        return LandingPageDataDTO.builder()
                .newProducts(products)
                .build();
    }

    @Override
    public LandingPageDataDTO getDiscountedProducts(int limit) {
        List<LandingPageProductDTO> products = getDiscountedProductsList(limit);
        return LandingPageDataDTO.builder()
                .discountedProducts(products)
                .build();
    }

    @Override
    public LandingPageDataDTO getPopularCategories(int limit) {
        List<LandingPageCategoryDTO> categories = getPopularCategoriesList(limit);
        return LandingPageDataDTO.builder()
                .popularCategories(categories)
                .build();
    }

    @Override
    public LandingPageDataDTO getPopularBrands(int limit) {
        List<LandingPageBrandDTO> brands = getPopularBrandsList(limit);
        return LandingPageDataDTO.builder()
                .popularBrands(brands)
                .build();
    }

    private List<LandingPageProductDTO> getTopSellingProductsList(int limit) {
        log.info("Fetching top-selling products with limit: {}", limit);

        // Get products sorted by bestseller status and creation date
        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "isBestseller")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Product> products = productRepository.findAll(pageable).getContent();

        return products.stream()
                .filter(Product::isActive)
                .map(this::convertToLandingPageProductDTO)
                .collect(Collectors.toList());
    }

    private List<LandingPageProductDTO> getNewProductsList(int limit) {
        log.info("Fetching new products with limit: {}", limit);

        // Get products sorted by creation date
        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Product> products = productRepository.findAll(pageable).getContent();

        return products.stream()
                .filter(Product::isActive)
                .filter(p -> p.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .map(this::convertToLandingPageProductDTO)
                .collect(Collectors.toList());
    }

    private List<LandingPageProductDTO> getDiscountedProductsList(int limit) {
        log.info("Fetching discounted products with limit: {}", limit);

        // Get all active products and filter for discounted ones
        List<Product> allProducts = productRepository.findAll();

        return allProducts.stream()
                .filter(Product::isActive)
                .filter(this::hasDiscount)
                .sorted((p1, p2) -> calculateDiscountPercentage(p2).compareTo(calculateDiscountPercentage(p1)))
                .limit(limit)
                .map(this::convertToLandingPageProductDTO)
                .collect(Collectors.toList());
    }

    private List<LandingPageCategoryDTO> getPopularCategoriesList(int limit) {
        log.info("Fetching popular categories with limit: {}", limit);

        // Get all active categories
        List<Category> categories = categoryRepository.findByIsActiveTrue(PageRequest.of(0, limit * 2)).getContent();

        return categories.stream()
                .map(this::convertToLandingPageCategoryDTO)
                .sorted((c1, c2) -> Long.compare(c2.getProductCount(), c1.getProductCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<LandingPageBrandDTO> getPopularBrandsList(int limit) {
        log.info("Fetching popular brands with limit: {}", limit);

        // Get all active brands
        List<Brand> brands = brandRepository.findByIsActiveTrue();

        return brands.stream()
                .map(this::convertToLandingPageBrandDTO)
                .sorted((b1, b2) -> Long.compare(b2.getProductCount(), b1.getProductCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private LandingPageProductDTO convertToLandingPageProductDTO(Product product) {
        // Get primary image
        String primaryImageUrl = null;
        String primaryImageAlt = null;

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            ProductImage primaryImage = product.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .orElse(product.getImages().get(0));

            primaryImageUrl = primaryImage.getImageUrl();
            primaryImageAlt = primaryImage.getAltText();
        }

        // Calculate discount
        BigDecimal discountPercentage = calculateDiscountPercentage(product);
        BigDecimal originalPrice = hasDiscount(product) ? product.getPrice() : null;

        return LandingPageProductDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .shortDescription(product.getShortDescription())
                .price(product.getDiscountedPrice() != null ? product.getDiscountedPrice() : product.getPrice())
                .originalPrice(originalPrice)
                .discountPercentage(discountPercentage)
                .stockQuantity(product.getTotalStockQuantity())
                .primaryImageUrl(primaryImageUrl)
                .primaryImageAlt(primaryImageAlt)
                .averageRating(0.0) // Will be calculated from reviews if needed
                .reviewCount(0) // Will be calculated from reviews if needed
                .isNew(product.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .isBestseller(product.isBestseller())
                .isFeatured(product.isFeatured())
                .isInStock(product.getTotalStockQuantity() > 0)
                .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .slug(product.getSlug())
                .createdAt(product.getCreatedAt())
                .discountEndDate(product.getDiscount() != null ? product.getDiscount().getEndDate() : null)
                .discountName(product.getDiscount() != null ? product.getDiscount().getName() : null)
                .hasActiveDiscount(product.getDiscount() != null && product.getDiscount().isValid())
                .build();
    }

    private LandingPageCategoryDTO convertToLandingPageCategoryDTO(Category category) {
        // Count products in this category
        long productCount = productRepository.countByCategoryAndIsActiveTrue(category);

        return LandingPageCategoryDTO.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .slug(category.getSlug())
                .productCount(productCount)
                .isActive(category.isActive())
                .isFeatured(category.isFeatured())
                .build();
    }

    private LandingPageBrandDTO convertToLandingPageBrandDTO(Brand brand) {
        // Count products for this brand
        long productCount = productRepository.countByBrandAndIsActiveTrue(brand);

        return LandingPageBrandDTO.builder()
                .brandId(brand.getBrandId())
                .brandName(brand.getBrandName())
                .description(brand.getDescription())
                .logoUrl(brand.getLogoUrl())
                .slug(brand.getSlug())
                .productCount(productCount)
                .isActive(brand.isActive())
                .isFeatured(brand.isFeatured())
                .build();
    }

    private boolean hasDiscount(Product product) {
        return product.getDiscountedPrice() != null &&
                product.getDiscountedPrice().compareTo(product.getPrice()) < 0;
    }

    private BigDecimal calculateDiscountPercentage(Product product) {
        if (!hasDiscount(product)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = product.getPrice().subtract(product.getDiscountedPrice());
        return discount.divide(product.getPrice(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
