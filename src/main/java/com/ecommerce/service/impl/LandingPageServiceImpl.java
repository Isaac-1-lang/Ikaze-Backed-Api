package com.ecommerce.service.impl;

import com.ecommerce.dto.*;
import com.ecommerce.entity.Brand;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.repository.BrandRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.LandingPageService;
import com.ecommerce.service.ProductAvailabilityService;
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
import java.util.Collections;
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
    private final ProductAvailabilityService productAvailabilityService;

    @Override
    public LandingPageDataDTO getLandingPageData() {
        log.info("Generating landing page data");

        try {
            List<LandingPageProductDTO> topSellingProducts = getTopSellingProductsList(8);
            List<LandingPageProductDTO> newProducts = getNewProductsList(8);
            List<LandingPageProductDTO> discountedProducts = getDiscountedProductsList(8);
            List<CategoryWithProductsDTO> featuredCategories = getFeaturedCategoriesWithProducts(3, 6);
            List<BrandWithProductsDTO> featuredBrands = getFeaturedBrandsWithProducts(3, 6);

            long totalProducts = productRepository.count();
            long totalCategories = categoryRepository.count();
            long totalBrands = brandRepository.count();
            long totalActiveProducts = productRepository.countActive();

            return LandingPageDataDTO.builder()
                    .topSellingProducts(topSellingProducts)
                    .newProducts(newProducts)
                    .discountedProducts(discountedProducts)
                    .featuredCategories(featuredCategories)
                    .featuredBrands(featuredBrands)
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
    public LandingPageDataDTO getFeaturedCategories(int categoryLimit, int productLimit) {
        List<CategoryWithProductsDTO> categories = getFeaturedCategoriesWithProducts(categoryLimit, productLimit);
        return LandingPageDataDTO.builder()
                .featuredCategories(categories)
                .build();
    }

    @Override
    public LandingPageDataDTO getFeaturedBrands(int brandLimit, int productLimit) {
        List<BrandWithProductsDTO> brands = getFeaturedBrandsWithProducts(brandLimit, productLimit);
        return LandingPageDataDTO.builder()
                .featuredBrands(brands)
                .build();
    }

    private List<LandingPageProductDTO> getTopSellingProductsList(int limit) {
        log.info("Fetching top-selling products with limit: {}", limit);

        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "isBestseller")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Product> products = productRepository.findAll(pageable).getContent();

        return products.stream()
                .filter(productAvailabilityService::isProductAvailableForCustomers)
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
                .filter(productAvailabilityService::isProductAvailableForCustomers)
                .filter(p -> p.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .map(this::convertToLandingPageProductDTO)
                .collect(Collectors.toList());
    }

    private List<LandingPageProductDTO> getDiscountedProductsList(int limit) {
        log.info("Fetching discounted products with limit: {}", limit);

        // Get all active products and filter for discounted ones
        List<Product> allProducts = productRepository.findAll();

        return allProducts.stream()
                .filter(productAvailabilityService::isProductAvailableForCustomers)
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
        
        BigDecimal displayPrice;
        BigDecimal originalPrice = null;
        
        if (product.getDiscountedPrice() != null && 
            product.getDiscountedPrice().compareTo(product.getPrice()) < 0) {
            displayPrice = product.getDiscountedPrice();
            originalPrice = product.getPrice();
        } else {
            displayPrice = product.getPrice();
            originalPrice = null;
        }

        boolean hasVariantDiscounts = hasVariantDiscounts(product);
        BigDecimal maxVariantDiscount = getMaxVariantDiscount(product);
        Integer discountedVariantsCount = getDiscountedVariantsCount(product);

        return LandingPageProductDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .shortDescription(product.getShortDescription())
                .price(displayPrice)
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
                .hasVariantDiscounts(hasVariantDiscounts)
                .maxVariantDiscount(maxVariantDiscount)
                .discountedVariantsCount(discountedVariantsCount)
                .build();
    }

    private LandingPageCategoryDTO convertToLandingPageCategoryDTO(Category category) {
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
        // Check for product-level discount
        boolean hasProductDiscount = product.getDiscountedPrice() != null &&
                product.getDiscountedPrice().compareTo(product.getPrice()) < 0;
        
        // Check for variant-level discounts
        boolean hasVariantDiscount = hasVariantDiscounts(product);
        
        return hasProductDiscount || hasVariantDiscount;
    }

    private boolean hasVariantDiscounts(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return false;
        }
        
        return product.getVariants().stream()
                .anyMatch(variant -> variant != null && 
                         variant.getDiscountedPrice() != null &&
                         variant.getDiscountedPrice().compareTo(variant.getPrice()) < 0);
    }

    private BigDecimal getMaxVariantDiscount(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return product.getVariants().stream()
                .filter(variant -> variant != null && 
                               variant.getDiscountedPrice() != null &&
                               variant.getDiscountedPrice().compareTo(variant.getPrice()) < 0)
                .map(variant -> {
                    try {
                        BigDecimal originalPrice = variant.getPrice();
                        BigDecimal discountedPrice = variant.getDiscountedPrice();
                        if (originalPrice != null && discountedPrice != null && 
                            originalPrice.compareTo(BigDecimal.ZERO) > 0 && 
                            originalPrice.compareTo(discountedPrice) > 0) {
                            return originalPrice.subtract(discountedPrice)
                                    .divide(originalPrice, 4, java.math.RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100"));
                        }
                    } catch (Exception e) {
                        log.warn("Error calculating discount for variant {}: {}", variant.getId(), e.getMessage());
                    }
                    return BigDecimal.ZERO;
                })
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private Integer getDiscountedVariantsCount(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return 0;
        }
        
        return (int) product.getVariants().stream()
                .filter(variant -> variant != null && 
                               variant.getDiscountedPrice() != null &&
                               variant.getDiscountedPrice().compareTo(variant.getPrice()) < 0)
                .count();
    }

    private BigDecimal calculateDiscountPercentage(Product product) {
        if (!hasDiscount(product)) {
            return BigDecimal.ZERO;
        }

        // Check for product-level discount first
        if (product.getDiscountedPrice() != null && 
            product.getDiscountedPrice().compareTo(product.getPrice()) < 0) {
            BigDecimal discount = product.getPrice().subtract(product.getDiscountedPrice());
            return discount.divide(product.getPrice(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        
        // If no product-level discount, return the max variant discount
        return getMaxVariantDiscount(product);
    }

    /**
     * Get random top-performing categories with their products
     */
    private List<CategoryWithProductsDTO> getFeaturedCategoriesWithProducts(int categoryLimit, int productLimit) {
        log.info("Fetching {} featured categories with {} products each", categoryLimit, productLimit);

        // Get top categories by product count (active categories only)
        List<Category> topCategories = categoryRepository.findByIsActiveTrue(PageRequest.of(0, categoryLimit * 2))
                .getContent()
                .stream()
                .filter(category -> productRepository.countByCategoryAndIsActiveTrue(category) > 0)
                .sorted((c1, c2) -> Long.compare(
                        productRepository.countByCategoryAndIsActiveTrue(c2),
                        productRepository.countByCategoryAndIsActiveTrue(c1)))
                .limit(categoryLimit * 2) // Get more than needed for randomization
                .collect(Collectors.toList());

        // Randomize and select final categories
        Collections.shuffle(topCategories);
        List<Category> selectedCategories = topCategories.stream()
                .limit(categoryLimit)
                .collect(Collectors.toList());

        return selectedCategories.stream()
                .map(category -> {
                    // Get products for this category
                    List<Product> categoryProducts = productRepository
                            .findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(category, PageRequest.of(0, productLimit))
                            .getContent();

                    List<LandingPageProductDTO> productDTOs = categoryProducts.stream()
                            .filter(productAvailabilityService::isProductAvailableForCustomers)
                            .map(this::convertToLandingPageProductDTO)
                            .collect(Collectors.toList());

                    return CategoryWithProductsDTO.builder()
                            .categoryId(category.getId())
                            .categoryName(category.getName())
                            .description(category.getDescription())
                            .imageUrl(category.getImageUrl())
                            .slug(category.getSlug())
                            .productCount(productRepository.countByCategoryAndIsActiveTrue(category))
                            .isActive(category.isActive())
                            .isFeatured(category.isFeatured())
                            .products(productDTOs)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get random top-performing brands with their products
     */
    private List<BrandWithProductsDTO> getFeaturedBrandsWithProducts(int brandLimit, int productLimit) {
        log.info("Fetching {} featured brands with {} products each", brandLimit, productLimit);

        // Get top brands by product count (active brands only)
        List<Brand> topBrands = brandRepository.findByIsActiveTrue()
                .stream()
                .filter(brand -> productRepository.countByBrandAndIsActiveTrue(brand) > 0)
                .sorted((b1, b2) -> Long.compare(
                        productRepository.countByBrandAndIsActiveTrue(b2),
                        productRepository.countByBrandAndIsActiveTrue(b1)))
                .limit(brandLimit * 2) // Get more than needed for randomization
                .collect(Collectors.toList());

        // Randomize and select final brands
        Collections.shuffle(topBrands);
        List<Brand> selectedBrands = topBrands.stream()
                .limit(brandLimit)
                .collect(Collectors.toList());

        return selectedBrands.stream()
                .map(brand -> {
                    // Get products for this brand
                    List<Product> brandProducts = productRepository
                            .findByBrandAndIsActiveTrueOrderByCreatedAtDesc(brand, PageRequest.of(0, productLimit))
                            .getContent();

                    List<LandingPageProductDTO> productDTOs = brandProducts.stream()
                            .filter(productAvailabilityService::isProductAvailableForCustomers)
                            .map(this::convertToLandingPageProductDTO)
                            .collect(Collectors.toList());

                    return BrandWithProductsDTO.builder()
                            .brandId(brand.getBrandId())
                            .brandName(brand.getBrandName())
                            .description(brand.getDescription())
                            .logoUrl(brand.getLogoUrl())
                            .slug(brand.getSlug())
                            .productCount(productRepository.countByBrandAndIsActiveTrue(brand))
                            .isActive(brand.isActive())
                            .isFeatured(brand.isFeatured())
                            .products(productDTOs)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
