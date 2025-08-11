package com.ecommerce.service.impl;

import com.ecommerce.dto.CreateProductDTO;
import com.ecommerce.dto.CreateProductVariantDTO;
import com.ecommerce.dto.ManyProductsDto;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.dto.ProductSearchDTO;
import com.ecommerce.dto.ProductUpdateDTO;
import com.ecommerce.dto.ProductVariantDTO;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.Exception.ProductDeletionException;
import com.ecommerce.service.CloudinaryService;
import com.ecommerce.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantImageRepository productVariantImageRepository;
    private final ProductVideoRepository productVideoRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final DiscountRepository discountRepository;
    private final ProductAttributeTypeRepository attributeTypeRepository;
    private final ProductAttributeValueRepository attributeValueRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final CloudinaryService cloudinaryService;

    // Using thread pool for concurrent image/video uploads
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    @Transactional
    public ProductDTO createProduct(CreateProductDTO createProductDTO) {
        try {
            log.info("Starting product creation for: {}", createProductDTO.getName());

            // Validate category exists and is active
            Category category = validateAndGetCategory(createProductDTO.getCategoryId());

            // Validate SKU uniqueness
            validateSkuUniqueness(createProductDTO.getSku());

            // Validate brand if provided
            Brand brand = null;
            if (createProductDTO.getBrandId() != null) {
                brand = validateAndGetBrand(createProductDTO.getBrandId());
            }

            // Validate discount if provided
            Discount discount = null;
            if (createProductDTO.getDiscountId() != null) {
                discount = validateAndGetDiscount(createProductDTO.getDiscountId());
            }

            // Create and save product entity
            Product product = createProductEntity(createProductDTO, category, brand, discount);
            Product savedProduct = productRepository.save(product);
            log.info("Product saved with ID: {}", savedProduct.getProductId());

            // Process media and variants concurrently for better performance
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // Process product images concurrently
            if (createProductDTO.getProductImages() != null && !createProductDTO.getProductImages().isEmpty()) {
                CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(
                        () -> processProductImages(savedProduct, createProductDTO.getProductImages(),
                                createProductDTO.getImageMetadata()),
                        executorService);
                futures.add(imagesFuture);
            }

            // Process product videos concurrently
            if (createProductDTO.getProductVideos() != null && !createProductDTO.getProductVideos().isEmpty()) {
                CompletableFuture<Void> videosFuture = CompletableFuture.runAsync(
                        () -> processProductVideos(savedProduct, createProductDTO.getProductVideos(),
                                createProductDTO.getVideoMetadata()),
                        executorService);
                futures.add(videosFuture);
            }

            // Process variants (this includes variant images which also use concurrent
            // processing)
            if (createProductDTO.getVariants() != null && !createProductDTO.getVariants().isEmpty()) {
                CompletableFuture<Void> variantsFuture = CompletableFuture.runAsync(
                        () -> processProductVariants(savedProduct, createProductDTO.getVariants()),
                        executorService);
                futures.add(variantsFuture);
            }

            // Wait for all concurrent operations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Refresh product from database to get all relationships
            Product refreshedProduct = productRepository.findById(savedProduct.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found after saving"));

            log.info("Product creation completed successfully for: {}", createProductDTO.getName());
            return mapProductToDTO(refreshedProduct);

        } catch (Exception e) {
            log.error("Error creating product: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create product: " + e.getMessage(), e);
        }
    }

    private Category validateAndGetCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + categoryId));

        if (!category.isActive()) {
            throw new IllegalArgumentException("Cannot create product with inactive category");
        }

        return category;
    }

    private void validateSkuUniqueness(String sku) {
        if (productRepository.findBySku(sku).isPresent()) {
            throw new IllegalArgumentException("Product with SKU " + sku + " already exists");
        }
    }

    private Brand validateAndGetBrand(UUID brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found with ID: " + brandId));

        if (!brand.isActive()) {
            throw new IllegalArgumentException("Cannot create product with inactive brand");
        }

        return brand;
    }

    private Discount validateAndGetDiscount(UUID discountId) {
        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with ID: " + discountId));

        if (!discount.isValid()) {
            throw new IllegalArgumentException("Cannot create product with invalid discount");
        }

        return discount;
    }

    private Product createProductEntity(CreateProductDTO createProductDTO, Category category, Brand brand,
            Discount discount) {
        Product product = new Product();
        product.setProductName(createProductDTO.getName());
        product.setShortDescription(createProductDTO.getDescription());
        product.setSku(createProductDTO.getSku());
        product.setBarcode(createProductDTO.getBarcode());
        product.setPrice(createProductDTO.getBasePrice());
        product.setCompareAtPrice(createProductDTO.getSalePrice());
        product.setCostPrice(createProductDTO.getCostPrice());
        product.setStockQuantity(createProductDTO.getStockQuantity() != null ? createProductDTO.getStockQuantity() : 0);
        product.setLowStockThreshold(
                createProductDTO.getLowStockThreshold() != null ? createProductDTO.getLowStockThreshold() : 5);
        product.setCategory(category);
        product.setBrand(brand);
        product.setDiscount(discount);
        product.setModel(createProductDTO.getModel());
        product.setSlug(createProductDTO.getSlug() != null ? createProductDTO.getSlug()
                : generateSlug(createProductDTO.getName()));
        product.setActive(createProductDTO.getIsActive() != null ? createProductDTO.getIsActive() : true);
        product.setFeatured(createProductDTO.getIsFeatured() != null ? createProductDTO.getIsFeatured() : false);
        product.setBestseller(createProductDTO.getIsBestseller() != null ? createProductDTO.getIsBestseller() : false);
        product.setNewArrival(createProductDTO.getIsNewArrival() != null ? createProductDTO.getIsNewArrival() : false);
        product.setOnSale(createProductDTO.getIsOnSale() != null ? createProductDTO.getIsOnSale() : false);
        product.setSalePercentage(createProductDTO.getSalePercentage());

        // Create product detail if needed
        if (hasProductDetailData(createProductDTO)) {
            ProductDetail productDetail = createProductDetail(createProductDTO);
            productDetail.setProduct(product);
            product.setProductDetail(productDetail);
        }

        return product;
    }

    private boolean hasProductDetailData(CreateProductDTO createProductDTO) {
        return createProductDTO.getFullDescription() != null ||
                createProductDTO.getMetaTitle() != null ||
                createProductDTO.getMetaDescription() != null ||
                createProductDTO.getMetaKeywords() != null ||
                createProductDTO.getSearchKeywords() != null ||
                createProductDTO.getDimensionsCm() != null ||
                createProductDTO.getWeightKg() != null;
    }

    private ProductDetail createProductDetail(CreateProductDTO createProductDTO) {
        ProductDetail productDetail = new ProductDetail();
        productDetail.setDescription(createProductDTO.getFullDescription());
        if (createProductDTO.getMetaTitle() != null) {
            productDetail.setMetaTitle(createProductDTO.getMetaTitle());
        }
        if (createProductDTO.getMetaDescription() != null) {
            productDetail.setMetaDescription(createProductDTO.getMetaDescription());
        }
        if (createProductDTO.getMetaKeywords() != null) {
            productDetail.setMetaKeywords(createProductDTO.getMetaKeywords());
        }
        if (createProductDTO.getSearchKeywords() != null) {
            productDetail.setSearchKeywords(createProductDTO.getSearchKeywords());
        }
        if (createProductDTO.getDimensionsCm() != null) {
            productDetail.setDimensionsCm(createProductDTO.getDimensionsCm());
        }
        if (createProductDTO.getWeightKg() != null) {
            productDetail.setWeightKg(createProductDTO.getWeightKg());
        }
        return productDetail;
    }

    @Override
    public ProductDTO getProductById(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));
        return mapProductToDTO(product);
    }

    @Override
    public ProductDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with slug: " + slug));
        return mapProductToDTO(product);
    }

    @Override
    public Page<ManyProductsDto> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::mapProductToManyProductsDto);
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(UUID productId, ProductUpdateDTO updateProductDTO) {
        try {
            log.info("Starting product update for ID: {}", productId);

            // Get existing product
            Product existingProduct = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            // Update basic product fields if provided
            updateBasicProductFields(existingProduct, updateProductDTO);

            // Update category if provided
            if (updateProductDTO.getCategoryId() != null) {
                Category category = validateAndGetCategory(updateProductDTO.getCategoryId());
                existingProduct.setCategory(category);
            }

            // Update brand if provided
            if (updateProductDTO.getBrandId() != null) {
                Brand brand = validateAndGetBrand(updateProductDTO.getBrandId());
                existingProduct.setBrand(brand);
            }

            // Update discount if provided
            if (updateProductDTO.getDiscountId() != null) {
                Discount discount = validateAndGetDiscount(updateProductDTO.getDiscountId());
                existingProduct.setDiscount(discount);
            }

            // Update product detail if provided
            updateProductDetail(existingProduct, updateProductDTO);

            // Save updated product
            Product savedProduct = productRepository.save(existingProduct);
            log.info("Product basic fields updated successfully for ID: {}", productId);

            // Process new variants if provided
            if (updateProductDTO.getNewVariants() != null && !updateProductDTO.getNewVariants().isEmpty()) {
                processNewVariants(savedProduct, updateProductDTO.getNewVariants(),
                        updateProductDTO.getNewVariantImages(), updateProductDTO.getNewVariantImageMetadata());
            }

            // Refresh product from database to get all relationships
            Product refreshedProduct = productRepository.findById(savedProduct.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found after update"));

            log.info("Product update completed successfully for ID: {}", productId);
            return mapProductToDTO(refreshedProduct);

        } catch (Exception e) {
            log.error("Error updating product with ID {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Failed to update product: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteProduct(UUID productId) {
        try {
            log.info("Starting product deletion for ID: {}", productId);

            // Get the product with all its relationships
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            log.info("Found product: {} for deletion", product.getProductName());

            // Check if there are any pending orders for this product
            checkForPendingOrders(product);

            // Remove product from carts and wishlists
            removeProductFromCartsAndWishlists(product);

            // Delete all product variants (this will cascade to images and attributes)
            deleteProductVariants(product);

            // Delete main product images and videos
            deleteProductMedia(product);

            // Delete product detail
            deleteProductDetail(product);

            // Finally, delete the product itself
            productRepository.delete(product);

            log.info("Product deleted successfully with ID: {}", productId);
            return true;

        } catch (EntityNotFoundException e) {
            log.error("Product not found for deletion with ID {}: {}", productId, e.getMessage());
            throw e;
        } catch (ProductDeletionException e) {
            log.error("Product deletion blocked for ID {}: {}", productId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error deleting product with ID {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete product: " + e.getMessage(), e);
        }
    }

    /**
     * Check if there are any pending orders for the product variants
     * that would prevent deletion
     */
    private void checkForPendingOrders(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            log.debug("No variants found for product, skipping order check");
            return;
        }

        for (ProductVariant variant : product.getVariants()) {
            if (orderRepository.existsByProductVariantAndNotDelivered(variant.getId())) {
                String errorMessage = String.format(
                    "Cannot delete product '%s' because variant '%s' has pending orders that are not yet delivered. " +
                    "Please ensure all orders are delivered, cancelled, refunded, or returned before deleting the product.",
                    product.getProductName(), variant.getVariantSku());
                
                log.warn("Product deletion blocked due to pending orders for variant: {}", variant.getVariantSku());
                throw new ProductDeletionException(errorMessage);
            }
        }

        log.debug("No pending orders found for product variants, deletion can proceed");
    }

    /**
     * Remove product variants from all carts and wishlists
     */
    private void removeProductFromCartsAndWishlists(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            log.debug("No variants found for product, skipping cart/wishlist cleanup");
            return;
        }

        log.info("Removing product variants from carts and wishlists");

        for (ProductVariant variant : product.getVariants()) {
            try {
                // Remove from carts
                cartRepository.deleteCartItemsByProductVariant(variant.getId());
                log.debug("Removed variant {} from all carts", variant.getVariantSku());

                // Remove from wishlists
                wishlistRepository.deleteWishlistProductsByProductVariant(variant.getId());
                log.debug("Removed variant {} from all wishlists", variant.getVariantSku());

            } catch (Exception e) {
                log.warn("Failed to remove variant {} from carts/wishlists: {}", variant.getVariantSku(), e.getMessage());
                // Continue with deletion even if cart/wishlist cleanup fails
            }
        }

        log.info("Successfully removed product variants from carts and wishlists");
    }

    /**
     * Delete all product variants and their associated data
     */
    private void deleteProductVariants(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            log.debug("No variants found for product, skipping variant deletion");
            return;
        }

        log.info("Deleting {} product variants", product.getVariants().size());

        for (ProductVariant variant : product.getVariants()) {
            try {
                // Delete variant images from Cloudinary and database
                deleteVariantImages(variant);

                // Delete variant attributes
                deleteVariantAttributes(variant);

                // Delete the variant itself
                productVariantRepository.delete(variant);

                log.debug("Successfully deleted variant: {}", variant.getVariantSku());

            } catch (Exception e) {
                log.error("Error deleting variant {}: {}", variant.getVariantSku(), e.getMessage(), e);
                throw new RuntimeException("Failed to delete variant: " + variant.getVariantSku(), e);
            }
        }

        log.info("Successfully deleted all product variants");
    }

    /**
     * Delete main product images and videos
     */
    private void deleteProductMedia(Product product) {
        try {
            // Delete product images
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                log.info("Deleting {} product images", product.getImages().size());

                for (ProductImage image : product.getImages()) {
                    try {
                        if (image.getImageUrl() != null && !image.getImageUrl().isEmpty()) {
                            cloudinaryService.deleteImage(image.getImageUrl());
                            log.debug("Deleted image from Cloudinary: {}", image.getImageUrl());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to delete image from Cloudinary: {}. Error: {}", 
                                image.getImageUrl(), e.getMessage());
                    }
                }

                productImageRepository.deleteAll(product.getImages());
                log.info("Successfully deleted {} product images", product.getImages().size());
            }

            // Delete product videos
            if (product.getVideos() != null && !product.getVideos().isEmpty()) {
                log.info("Deleting {} product videos", product.getVideos().size());

                for (ProductVideo video : product.getVideos()) {
                    try {
                        if (video.getUrl() != null && !video.getUrl().isEmpty()) {
                            cloudinaryService.deleteFile(video.getUrl());
                            log.debug("Deleted video from Cloudinary: {}", video.getUrl());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to delete video from Cloudinary: {}. Error: {}", 
                                video.getUrl(), e.getMessage());
                    }
                }

                productVideoRepository.deleteAll(product.getVideos());
                log.info("Successfully deleted {} product videos", product.getVideos().size());
            }

        } catch (Exception e) {
            log.error("Error deleting product media: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete product media: " + e.getMessage(), e);
        }
    }

    /**
     * Delete product detail
     */
    private void deleteProductDetail(Product product) {
        if (product.getProductDetail() != null) {
            try {
                log.debug("Deleting product detail for product: {}", product.getProductName());
                // ProductDetail will be automatically deleted due to cascade
                log.debug("Product detail deleted successfully");
            } catch (Exception e) {
                log.warn("Failed to delete product detail: {}", e.getMessage());
                // Continue with deletion even if detail deletion fails
            }
        }
    }

    @Override
    public Page<ManyProductsDto> searchProducts(ProductSearchDTO searchDTO) {
        try {
            log.info("Searching products with criteria: {}", searchDTO);

            // Create a Pageable object from the search DTO
            int page = searchDTO.getPage() != null ? searchDTO.getPage() : 0;
            int size = searchDTO.getSize() != null ? searchDTO.getSize() : 10;
            String sortBy = searchDTO.getSortBy() != null ? searchDTO.getSortBy() : "createdAt";
            String sortDirection = searchDTO.getSortDirection() != null ? searchDTO.getSortDirection() : "desc";

            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            // Build the search criteria
            Specification<Product> spec = buildProductSearchSpecification(searchDTO);

            // Execute the search
            Page<Product> productPage = productRepository.findAll(spec, pageable);

            // Map to ManyProductsDto
            Page<ManyProductsDto> result = productPage.map(this::mapProductToManyProductsDto);

            log.info("Search completed. Found {} products matching criteria", result.getTotalElements());
            return result;

        } catch (Exception e) {
            log.error("Error searching products: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search products: " + e.getMessage(), e);
        }
    }

    /**
     * Build a JPA Specification for product search based on the search DTO
     */
    private Specification<Product> buildProductSearchSpecification(ProductSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Basic product identifiers
            if (searchDTO.getProductId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productId"), searchDTO.getProductId()));
            }

            if (searchDTO.getName() != null && !searchDTO.getName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), 
                    "%" + searchDTO.getName().toLowerCase() + "%"));
            }

            if (searchDTO.getDescription() != null && !searchDTO.getDescription().trim().isEmpty()) {
                Join<Product, ProductDetail> detailJoin = root.join("productDetail", JoinType.LEFT);
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(detailJoin.get("description")), 
                    "%" + searchDTO.getDescription().toLowerCase() + "%"));
            }

            if (searchDTO.getSku() != null && !searchDTO.getSku().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), 
                    "%" + searchDTO.getSku().toLowerCase() + "%"));
            }

            if (searchDTO.getBarcode() != null && !searchDTO.getBarcode().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("barcode")), 
                    "%" + searchDTO.getBarcode().toLowerCase() + "%"));
            }

            if (searchDTO.getSlug() != null && !searchDTO.getSlug().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), 
                    "%" + searchDTO.getSlug().toLowerCase() + "%"));
            }

            if (searchDTO.getModel() != null && !searchDTO.getModel().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("model")), 
                    "%" + searchDTO.getModel().toLowerCase() + "%"));
            }

            // Price filters
            if (searchDTO.getBasePriceMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), searchDTO.getBasePriceMin()));
            }

            if (searchDTO.getBasePriceMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), searchDTO.getBasePriceMax()));
            }

            if (searchDTO.getSalePriceMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("salePrice"), searchDTO.getSalePriceMin()));
            }

            if (searchDTO.getSalePriceMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("salePrice"), searchDTO.getSalePriceMax()));
            }

            if (searchDTO.getCompareAtPriceMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("compareAtPrice"), searchDTO.getCompareAtPriceMin()));
            }

            if (searchDTO.getCompareAtPriceMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("compareAtPrice"), searchDTO.getCompareAtPriceMax()));
            }

            // Stock filters
            if (searchDTO.getStockQuantityMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("stockQuantity"), searchDTO.getStockQuantityMin()));
            }

            if (searchDTO.getStockQuantityMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("stockQuantity"), searchDTO.getStockQuantityMax()));
            }

            if (searchDTO.getInStock() != null) {
                if (searchDTO.getInStock()) {
                    predicates.add(criteriaBuilder.greaterThan(root.get("stockQuantity"), 0));
                } else {
                    predicates.add(criteriaBuilder.equal(root.get("stockQuantity"), 0));
                }
            }

            // Category filters
            if (searchDTO.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), searchDTO.getCategoryId()));
            }

            if (searchDTO.getCategoryIds() != null && !searchDTO.getCategoryIds().isEmpty()) {
                predicates.add(root.get("category").get("id").in(searchDTO.getCategoryIds()));
            }

            // Brand filters
            if (searchDTO.getBrandId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("brand").get("brandId"), searchDTO.getBrandId()));
            }

            if (searchDTO.getBrandIds() != null && !searchDTO.getBrandIds().isEmpty()) {
                predicates.add(root.get("brand").get("brandId").in(searchDTO.getBrandIds()));
            }

            // Discount filters
            if (searchDTO.getDiscountId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("discount").get("discountId"), searchDTO.getDiscountId()));
            }

            if (searchDTO.getDiscountIds() != null && !searchDTO.getDiscountIds().isEmpty()) {
                predicates.add(root.get("discount").get("discountId").in(searchDTO.getDiscountIds()));
            }

            if (searchDTO.getHasDiscount() != null) {
                if (searchDTO.getHasDiscount()) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("discount")));
                } else {
                    predicates.add(criteriaBuilder.isNull(root.get("discount")));
                }
            }

            if (searchDTO.getIsOnSale() != null) {
                if (searchDTO.getIsOnSale()) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("discount")));
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("discount").get("startDate"), LocalDateTime.now()));
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("discount").get("endDate"), LocalDateTime.now()));
                }
            }

            // Feature flags
            if (searchDTO.getIsFeatured() != null) {
                predicates.add(criteriaBuilder.equal(root.get("featured"), searchDTO.getIsFeatured()));
            }

            if (searchDTO.getIsBestseller() != null) {
                predicates.add(criteriaBuilder.equal(root.get("bestseller"), searchDTO.getIsBestseller()));
            }

            if (searchDTO.getIsNewArrival() != null) {
                // New arrivals are products created within the last 30 days
                if (searchDTO.getIsNewArrival()) {
                    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), thirtyDaysAgo));
                }
            }

            // Date filters
            if (searchDTO.getCreatedAtMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), searchDTO.getCreatedAtMin()));
            }

            if (searchDTO.getCreatedAtMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), searchDTO.getCreatedAtMax()));
            }

            if (searchDTO.getUpdatedAtMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), searchDTO.getUpdatedAtMin()));
            }

            if (searchDTO.getUpdatedAtMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), searchDTO.getUpdatedAtMax()));
            }

            // Creator filter
            if (searchDTO.getCreatedBy() != null) {
                predicates.add(criteriaBuilder.equal(root.get("createdBy"), searchDTO.getCreatedBy()));
            }

            // Text search across multiple fields
            if (searchDTO.getSearchKeyword() != null && !searchDTO.getSearchKeyword().trim().isEmpty()) {
                String keyword = searchDTO.getSearchKeyword().toLowerCase();
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword + "%");
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("productDetail").get("description")), "%" + keyword + "%");
                Predicate skuPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), "%" + keyword + "%");
                Predicate barcodePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("barcode")), "%" + keyword + "%");
                
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate, skuPredicate, barcodePredicate));
            }

            // Combine all predicates with AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void processProductImages(Product product, List<MultipartFile> images,
            List<CreateProductDTO.ImageMetadata> metadata) {
        try {
            log.info("Processing {} product images", images.size());

            // Upload images to Cloudinary concurrently
            List<Map<String, String>> uploadResults = cloudinaryService.uploadMultipleImages(images);

            // Create product images
            for (int i = 0; i < uploadResults.size(); i++) {
                Map<String, String> uploadResult = uploadResults.get(i);

                if (uploadResult.containsKey("error")) {
                    log.error("Failed to upload image {}: {}", i, uploadResult.get("error"));
                    throw new RuntimeException("Failed to upload image: " + uploadResult.get("error"));
                }

                ProductImage productImage = new ProductImage();
                productImage.setProduct(product);
                productImage.setImageUrl(uploadResult.get("url"));

                // Set extracted metadata from Cloudinary
                if (uploadResult.get("width") != null) {
                    productImage.setWidth(Integer.valueOf(uploadResult.get("width")));
                }
                if (uploadResult.get("height") != null) {
                    productImage.setHeight(Integer.valueOf(uploadResult.get("height")));
                }
                if (uploadResult.get("fileSize") != null) {
                    productImage.setFileSize(Long.valueOf(uploadResult.get("fileSize")));
                }
                if (uploadResult.get("mimeType") != null) {
                    productImage.setMimeType(uploadResult.get("mimeType"));
                }

                // Set user-provided metadata if available
                if (metadata != null && i < metadata.size()) {
                    CreateProductDTO.ImageMetadata imgMetadata = metadata.get(i);
                    productImage.setAltText(imgMetadata.getAltText());
                    productImage.setTitle(imgMetadata.getAltText()); // Use alt text as title
                    productImage.setPrimary(imgMetadata.getIsPrimary() != null ? imgMetadata.getIsPrimary() : false);
                    productImage.setSortOrder(imgMetadata.getSortOrder() != null ? imgMetadata.getSortOrder() : i);
                } else {
                    productImage.setSortOrder(i);
                    // Set first image as primary if no metadata
                    if (i == 0) {
                        productImage.setPrimary(true);
                    }
                }

                productImageRepository.save(productImage);
            }

            log.info("Successfully processed {} product images", uploadResults.size());
        } catch (Exception e) {
            log.error("Error processing product images", e);
            throw new RuntimeException("Failed to process product images: " + e.getMessage(), e);
        }
    }

    private void processProductVideos(Product product, List<MultipartFile> videos,
            List<CreateProductDTO.VideoMetadata> metadata) {
        try {
            log.info("Processing {} product videos", videos.size());

            // Validate video duration (max 30 seconds) if metadata provided
            if (metadata != null) {
                for (int i = 0; i < metadata.size() && i < videos.size(); i++) {
                    CreateProductDTO.VideoMetadata videoMetadata = metadata.get(i);
                    if (videoMetadata.getDurationSeconds() != null && videoMetadata.getDurationSeconds() > 30) {
                        throw new IllegalArgumentException(
                                String.format("Video '%s' duration (%d seconds) exceeds maximum allowed (30 seconds)",
                                        videos.get(i).getOriginalFilename(), videoMetadata.getDurationSeconds()));
                    }
                }
            }

            // Additional validation: check file sizes and types are handled in
            // CloudinaryService

            // Upload videos to Cloudinary concurrently
            List<Map<String, String>> uploadResults = cloudinaryService.uploadMultipleVideos(videos);

            // Create product videos
            for (int i = 0; i < uploadResults.size(); i++) {
                Map<String, String> uploadResult = uploadResults.get(i);

                if (uploadResult.containsKey("error")) {
                    log.error("Failed to upload video {}: {}", i, uploadResult.get("error"));
                    throw new RuntimeException("Failed to upload video: " + uploadResult.get("error"));
                }

                ProductVideo productVideo = new ProductVideo();
                productVideo.setProduct(product);
                productVideo.setUrl(uploadResult.get("url"));

                // Set metadata if available
                if (metadata != null && i < metadata.size()) {
                    CreateProductDTO.VideoMetadata vidMetadata = metadata.get(i);
                    productVideo.setTitle(vidMetadata.getTitle());
                    productVideo.setDescription(vidMetadata.getDescription());
                    productVideo.setSortOrder(vidMetadata.getSortOrder() != null ? vidMetadata.getSortOrder() : i);
                } else {
                    productVideo.setSortOrder(i);
                }

                productVideoRepository.save(productVideo);
            }

            log.info("Successfully processed {} product videos", uploadResults.size());
        } catch (Exception e) {
            log.error("Error processing product videos", e);
            throw new RuntimeException("Failed to process product videos: " + e.getMessage(), e);
        }
    }

    private void processProductVariants(Product product, List<CreateProductVariantDTO> variantDTOs) {
        log.info("Processing {} product variants", variantDTOs.size());

        for (CreateProductVariantDTO variantDTO : variantDTOs) {
            // Validate variant SKU uniqueness if provided
            if (variantDTO.getVariantSku() != null &&
                    productVariantRepository.findByVariantSku(variantDTO.getVariantSku()).isPresent()) {
                throw new IllegalArgumentException(
                        "Variant with SKU " + variantDTO.getVariantSku() + " already exists");
            }

            // Create variant
            ProductVariant variant = createProductVariant(product, variantDTO);
            ProductVariant savedVariant = productVariantRepository.save(variant);

            // Process variant attributes if any
            if (variantDTO.getAttributes() != null && !variantDTO.getAttributes().isEmpty()) {
                processVariantAttributes(savedVariant, variantDTO.getAttributes());
            }

            // Process variant images if any (this also uses concurrent processing
            // internally)
            if (variantDTO.getVariantImages() != null && !variantDTO.getVariantImages().isEmpty()) {
                processVariantImages(savedVariant, variantDTO.getVariantImages(), variantDTO.getImageMetadata());
            }
        }

        log.info("Successfully processed {} product variants", variantDTOs.size());
    }

    private ProductVariant createProductVariant(Product product, CreateProductVariantDTO variantDTO) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantSku(variantDTO.getVariantSku());
        variant.setVariantBarcode(variantDTO.getVariantBarcode());
        variant.setPrice(variantDTO.getPrice() != null ? variantDTO.getPrice() : product.getPrice());
        variant.setCompareAtPrice(variantDTO.getSalePrice());
        variant.setCostPrice(variantDTO.getCostPrice());
        variant.setStockQuantity(variantDTO.getStockQuantity() != null ? variantDTO.getStockQuantity() : 0);
        variant.setLowStockThreshold(variantDTO.getLowStockThreshold() != null ? variantDTO.getLowStockThreshold()
                : product.getLowStockThreshold());
        variant.setActive(variantDTO.getIsActive() != null ? variantDTO.getIsActive() : true);
        variant.setSortOrder(variantDTO.getSortOrder() != null ? variantDTO.getSortOrder() : 0);
        return variant;
    }

    private void processVariantAttributes(ProductVariant variant,
            List<CreateProductVariantDTO.VariantAttributeDTO> attributeDTOs) {
        for (CreateProductVariantDTO.VariantAttributeDTO attributeDTO : attributeDTOs) {
            // Validate attribute value exists
            ProductAttributeValue attributeValue = attributeValueRepository.findById(attributeDTO.getAttributeValueId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Attribute value not found with ID: " + attributeDTO.getAttributeValueId()));

            // Create variant attribute value
            VariantAttributeValue variantAttributeValue = new VariantAttributeValue();
            variantAttributeValue.setId(new VariantAttributeValue.VariantAttributeValueId(
                    variant.getId(), attributeValue.getAttributeValueId()));
            variantAttributeValue.setProductVariant(variant);
            variantAttributeValue.setAttributeValue(attributeValue);

            variantAttributeValueRepository.save(variantAttributeValue);
        }
    }

    private void processVariantImages(ProductVariant variant, List<MultipartFile> images,
            List<CreateProductVariantDTO.VariantImageMetadata> metadata) {
        try {
            log.info("Processing {} variant images for variant {}", images.size(), variant.getId());

            // Upload images to Cloudinary concurrently
            List<Map<String, String>> uploadResults = cloudinaryService.uploadMultipleImages(images);

            // Create variant images
            for (int i = 0; i < uploadResults.size(); i++) {
                Map<String, String> uploadResult = uploadResults.get(i);

                if (uploadResult.containsKey("error")) {
                    log.error("Failed to upload variant image {}: {}", i, uploadResult.get("error"));
                    throw new RuntimeException("Failed to upload variant image: " + uploadResult.get("error"));
                }

                ProductVariantImage variantImage = new ProductVariantImage();
                variantImage.setProductVariant(variant);
                variantImage.setImageUrl(uploadResult.get("url"));

                // Set extracted metadata from Cloudinary
                if (uploadResult.get("width") != null) {
                    variantImage.setWidth(Integer.valueOf(uploadResult.get("width")));
                }
                if (uploadResult.get("height") != null) {
                    variantImage.setHeight(Integer.valueOf(uploadResult.get("height")));
                }
                if (uploadResult.get("fileSize") != null) {
                    variantImage.setFileSize(Long.valueOf(uploadResult.get("fileSize")));
                }
                if (uploadResult.get("mimeType") != null) {
                    variantImage.setMimeType(uploadResult.get("mimeType"));
                }

                // Set user-provided metadata if available
                if (metadata != null && i < metadata.size()) {
                    CreateProductVariantDTO.VariantImageMetadata imgMetadata = metadata.get(i);
                    variantImage.setAltText(imgMetadata.getAltText());
                    variantImage.setTitle(imgMetadata.getAltText()); // Use alt text as title
                    variantImage.setPrimary(imgMetadata.getIsPrimary() != null ? imgMetadata.getIsPrimary() : false);
                    variantImage.setSortOrder(imgMetadata.getSortOrder() != null ? imgMetadata.getSortOrder() : i);
                } else {
                    variantImage.setSortOrder(i);
                    // Set first image as primary if no metadata
                    if (i == 0) {
                        variantImage.setPrimary(true);
                    }
                }

                productVariantImageRepository.save(variantImage);
            }

            log.info("Successfully processed {} variant images for variant {}", uploadResults.size(), variant.getId());
        } catch (Exception e) {
            log.error("Error processing variant images for variant {}", variant.getId(), e);
            throw new RuntimeException("Failed to process variant images: " + e.getMessage(), e);
        }
    }

    private void updateBasicProductFields(Product product, ProductUpdateDTO updateDTO) {
        if (updateDTO.getName() != null) {
            product.setProductName(updateDTO.getName());
        }
        if (updateDTO.getDescription() != null) {
            product.setShortDescription(updateDTO.getDescription());
        }
        if (updateDTO.getBarcode() != null) {
            product.setBarcode(updateDTO.getBarcode());
        }
        if (updateDTO.getBasePrice() != null) {
            product.setPrice(updateDTO.getBasePrice());
        }
        if (updateDTO.getSalePrice() != null) {
            product.setCompareAtPrice(updateDTO.getSalePrice());
        }
        if (updateDTO.getCostPrice() != null) {
            product.setCostPrice(updateDTO.getCostPrice());
        }
        if (updateDTO.getStockQuantity() != null) {
            product.setStockQuantity(updateDTO.getStockQuantity());
        }
        if (updateDTO.getLowStockThreshold() != null) {
            product.setLowStockThreshold(updateDTO.getLowStockThreshold());
        }
        if (updateDTO.getModel() != null) {
            product.setModel(updateDTO.getModel());
        }
        if (updateDTO.getSlug() != null) {
            product.setSlug(updateDTO.getSlug());
        } else if (updateDTO.getName() != null) {
            // Generate new slug if name changed but slug not provided
            product.setSlug(generateSlug(updateDTO.getName()));
        }
        if (updateDTO.getIsActive() != null) {
            product.setActive(updateDTO.getIsActive());
        }
        if (updateDTO.getIsFeatured() != null) {
            product.setFeatured(updateDTO.getIsFeatured());
        }
        if (updateDTO.getIsBestseller() != null) {
            product.setBestseller(updateDTO.getIsBestseller());
        }
        if (updateDTO.getIsNewArrival() != null) {
            product.setNewArrival(updateDTO.getIsNewArrival());
        }
        if (updateDTO.getIsOnSale() != null) {
            product.setOnSale(updateDTO.getIsOnSale());
        }
        if (updateDTO.getSalePercentage() != null) {
            product.setSalePercentage(updateDTO.getSalePercentage());
        }
    }

    private void updateProductDetail(Product product, ProductUpdateDTO updateDTO) {
        ProductDetail detail = product.getProductDetail();

        // Create product detail if it doesn't exist and we have data to set
        if (detail == null && hasProductDetailData(updateDTO)) {
            detail = new ProductDetail();
            detail.setProduct(product);
            product.setProductDetail(detail);
        }

        if (detail != null) {
            if (updateDTO.getFullDescription() != null) {
                detail.setDescription(updateDTO.getFullDescription());
            }
            if (updateDTO.getMetaTitle() != null) {
                detail.setMetaTitle(updateDTO.getMetaTitle());
            }
            if (updateDTO.getMetaDescription() != null) {
                detail.setMetaDescription(updateDTO.getMetaDescription());
            }
            if (updateDTO.getMetaKeywords() != null) {
                detail.setMetaKeywords(updateDTO.getMetaKeywords());
            }
            if (updateDTO.getSearchKeywords() != null) {
                detail.setSearchKeywords(updateDTO.getSearchKeywords());
            }
            if (updateDTO.getDimensionsCm() != null) {
                detail.setDimensionsCm(updateDTO.getDimensionsCm());
            }
            if (updateDTO.getWeightKg() != null) {
                detail.setWeightKg(updateDTO.getWeightKg());
            }
        }
    }

    private boolean hasProductDetailData(ProductUpdateDTO updateDTO) {
        return updateDTO.getFullDescription() != null ||
                updateDTO.getMetaTitle() != null ||
                updateDTO.getMetaDescription() != null ||
                updateDTO.getMetaKeywords() != null ||
                updateDTO.getSearchKeywords() != null ||
                updateDTO.getDimensionsCm() != null ||
                updateDTO.getWeightKg() != null;
    }

    private void processNewVariants(Product product, List<CreateProductVariantDTO> newVariants,
            List<MultipartFile> newVariantImages, List<ProductUpdateDTO.VariantImageMetadata> imageMetadata) {
        try {
            log.info("Processing {} new variants for product ID: {}", newVariants.size(), product.getProductId());

            for (int i = 0; i < newVariants.size(); i++) {
                CreateProductVariantDTO variantDTO = newVariants.get(i);

                // Validate variant SKU uniqueness
                if (variantDTO.getVariantSku() != null &&
                        productVariantRepository.findByVariantSku(variantDTO.getVariantSku()).isPresent()) {
                    throw new IllegalArgumentException(
                            "Variant with SKU " + variantDTO.getVariantSku() + " already exists");
                }

                // Create and save variant
                ProductVariant variant = createProductVariant(product, variantDTO);
                ProductVariant savedVariant = productVariantRepository.save(variant);

                // Process variant attributes if any
                if (variantDTO.getAttributes() != null && !variantDTO.getAttributes().isEmpty()) {
                    processVariantAttributes(savedVariant, variantDTO.getAttributes());
                }

                // Process variant images if provided
                if (newVariantImages != null && !newVariantImages.isEmpty()) {
                    processNewVariantImages(savedVariant, newVariantImages, imageMetadata, i);
                }
            }

            log.info("Successfully processed {} new variants for product ID: {}",
                    newVariants.size(), product.getProductId());
        } catch (Exception e) {
            log.error("Error processing new variants for product ID {}: {}", product.getProductId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process new variants: " + e.getMessage(), e);
        }
    }

    private void processNewVariantImages(ProductVariant variant, List<MultipartFile> images,
            List<ProductUpdateDTO.VariantImageMetadata> metadata, int variantIndex) {
        try {
            log.info("Processing images for new variant ID: {}", variant.getId());

            // Filter images for this specific variant
            List<MultipartFile> variantImages = new ArrayList<>();
            List<ProductUpdateDTO.VariantImageMetadata> variantMetadata = new ArrayList<>();

            if (metadata != null) {
                for (int i = 0; i < metadata.size(); i++) {
                    ProductUpdateDTO.VariantImageMetadata imgMeta = metadata.get(i);
                    if (imgMeta.getVariantIndex() != null && imgMeta.getVariantIndex() == variantIndex) {
                        if (i < images.size()) {
                            variantImages.add(images.get(i));
                            variantMetadata.add(imgMeta);
                        }
                    }
                }
            }

            if (!variantImages.isEmpty()) {
                // Upload images to Cloudinary
                List<Map<String, String>> uploadResults = cloudinaryService.uploadMultipleImages(variantImages);

                // Create variant images
                for (int i = 0; i < uploadResults.size(); i++) {
                    Map<String, String> uploadResult = uploadResults.get(i);

                    if (uploadResult.containsKey("error")) {
                        log.error("Failed to upload variant image {}: {}", i, uploadResult.get("error"));
                        throw new RuntimeException("Failed to upload variant image: " + uploadResult.get("error"));
                    }

                    ProductVariantImage variantImage = new ProductVariantImage();
                    variantImage.setProductVariant(variant);
                    variantImage.setImageUrl(uploadResult.get("url"));

                    // Set extracted metadata from Cloudinary
                    if (uploadResult.get("width") != null) {
                        variantImage.setWidth(Integer.valueOf(uploadResult.get("width")));
                    }
                    if (uploadResult.get("height") != null) {
                        variantImage.setHeight(Integer.valueOf(uploadResult.get("height")));
                    }
                    if (uploadResult.get("fileSize") != null) {
                        variantImage.setFileSize(Long.valueOf(uploadResult.get("fileSize")));
                    }
                    if (uploadResult.get("mimeType") != null) {
                        variantImage.setMimeType(uploadResult.get("mimeType"));
                    }

                    // Set user-provided metadata if available
                    if (i < variantMetadata.size()) {
                        ProductUpdateDTO.VariantImageMetadata imgMetadata = variantMetadata.get(i);
                        variantImage.setAltText(imgMetadata.getAltText());
                        variantImage.setTitle(imgMetadata.getAltText()); // Use alt text as title
                        variantImage
                                .setPrimary(imgMetadata.getIsPrimary() != null ? imgMetadata.getIsPrimary() : false);
                        variantImage.setSortOrder(imgMetadata.getSortOrder() != null ? imgMetadata.getSortOrder() : i);
                    } else {
                        variantImage.setSortOrder(i);
                        // Set first image as primary if no metadata
                        if (i == 0) {
                            variantImage.setPrimary(true);
                        }
                    }

                    productVariantImageRepository.save(variantImage);
                }
            }

            log.info("Successfully processed {} images for new variant ID: {}", variantImages.size(), variant.getId());
        } catch (Exception e) {
            log.error("Error processing images for new variant ID {}: {}", variant.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process variant images: " + e.getMessage(), e);
        }
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private ProductDTO mapProductToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(product.getProductId());
        dto.setName(product.getProductName());
        dto.setDescription(product.getShortDescription());
        dto.setSku(product.getSku());
        dto.setBarcode(product.getBarcode());
        dto.setBasePrice(product.getPrice());
        dto.setSalePrice(product.getCompareAtPrice());
        dto.setDiscountedPrice(product.getDiscountedPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        dto.setBrandId(product.getBrand() != null ? product.getBrand().getBrandId() : null);
        dto.setBrandName(product.getBrand() != null ? product.getBrand().getBrandName() : null);
        dto.setModel(product.getModel());
        dto.setSlug(product.getSlug());
        dto.setIsActive(product.isActive());
        dto.setIsFeatured(product.isFeatured());
        dto.setIsBestseller(product.isBestseller());
        dto.setIsNewArrival(product.isNewArrival());
        dto.setIsOnSale(product.isOnSale());
        dto.setAverageRating(product.getAverageRating());
        dto.setReviewCount(product.getReviewCount());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        // Map product details if available
        if (product.getProductDetail() != null) {
            dto.setFullDescription(product.getProductDetail().getDescription());
            dto.setMetaTitle(product.getProductDetail().getMetaTitle());
            dto.setMetaDescription(product.getProductDetail().getMetaDescription());
            dto.setMetaKeywords(product.getProductDetail().getMetaKeywords());
            dto.setDimensionsCm(product.getProductDetail().getDimensionsCm());
            dto.setWeightKg(product.getProductDetail().getWeightKg());
        }

        // Map images
        if (product.getImages() != null) {
            dto.setImages(product.getImages().stream()
                    .map(this::mapProductImageToDTO)
                    .collect(Collectors.toList()));
        }

        // Map videos
        if (product.getVideos() != null) {
            dto.setVideos(product.getVideos().stream()
                    .map(this::mapProductVideoToDTO)
                    .collect(Collectors.toList()));
        }

        // Map variants
        if (product.getVariants() != null) {
            dto.setVariants(product.getVariants().stream()
                    .map(this::mapProductVariantToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private ProductDTO.ProductImageDTO mapProductImageToDTO(ProductImage image) {
        return ProductDTO.ProductImageDTO.builder()
                .imageId(image.getId())
                .url(image.getImageUrl())
                .altText(image.getAltText())
                .isPrimary(image.isPrimary())
                .sortOrder(image.getSortOrder())
                .build();
    }

    private ProductDTO.ProductVideoDTO mapProductVideoToDTO(ProductVideo video) {
        return ProductDTO.ProductVideoDTO.builder()
                .videoId(video.getVideoId())
                .url(video.getUrl())
                .title(video.getTitle())
                .description(video.getDescription())
                .sortOrder(video.getSortOrder())
                .build();
    }

    private ProductVariantDTO mapProductVariantToDTO(ProductVariant variant) {
        ProductVariantDTO dto = new ProductVariantDTO();
        dto.setVariantId(variant.getId());
        dto.setVariantSku(variant.getVariantSku());
        dto.setVariantName(variant.getVariantName());
        dto.setVariantBarcode(variant.getVariantBarcode());
        dto.setPrice(variant.getPrice());
        dto.setSalePrice(variant.getCompareAtPrice());
        dto.setCostPrice(variant.getCostPrice());
        dto.setStockQuantity(variant.getStockQuantity());
        dto.setIsActive(variant.isActive());
        dto.setIsInStock(variant.isInStock());
        dto.setIsLowStock(variant.isLowStock());
        dto.setCreatedAt(variant.getCreatedAt());
        dto.setUpdatedAt(variant.getUpdatedAt());

        // Map variant images
        if (variant.getImages() != null) {
            dto.setImages(variant.getImages().stream()
                    .map(this::mapVariantImageToDTO)
                    .collect(Collectors.toList()));
        }

        // Map variant attributes
        if (variant.getAttributeValues() != null) {
            dto.setAttributes(variant.getAttributeValues().stream()
                    .map(this::mapVariantAttributeToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private ProductVariantDTO.VariantImageDTO mapVariantImageToDTO(ProductVariantImage image) {
        return ProductVariantDTO.VariantImageDTO.builder()
                .imageId(image.getId())
                .url(image.getImageUrl())
                .altText(image.getAltText())
                .isPrimary(image.isPrimary())
                .sortOrder(image.getSortOrder())
                .build();
    }

    private ProductVariantDTO.VariantAttributeDTO mapVariantAttributeToDTO(VariantAttributeValue variantAttribute) {
        ProductAttributeValue attributeValue = variantAttribute.getAttributeValue();
        ProductAttributeType attributeType = attributeValue.getAttributeType();

        return ProductVariantDTO.VariantAttributeDTO.builder()
                .attributeValueId(attributeValue.getAttributeValueId())
                .attributeValue(attributeValue.getValue())
                .attributeTypeId(attributeType.getAttributeTypeId())
                .attributeType(attributeType.getName())
                .build();
    }

    @Override
    @Transactional
    public boolean deleteProductVariant(UUID productId, Long variantId) {
        try {
            log.info("Starting deletion of product variant. Product ID: {}, Variant ID: {}", productId, variantId);

            // Verify product exists
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            // Verify variant exists and belongs to the product
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));

            if (!variant.getProduct().getProductId().equals(productId)) {
                throw new IllegalArgumentException("Variant does not belong to the specified product");
            }

            log.info("Found variant: {} for product: {}", variant.getVariantSku(), product.getProductName());

            // Delete variant images from Cloudinary and database
            deleteVariantImages(variant);

            // Delete variant attribute values
            deleteVariantAttributes(variant);

            // Delete the variant itself
            productVariantRepository.delete(variant);

            log.info("Successfully deleted product variant. Product ID: {}, Variant ID: {}", productId, variantId);
            return true;

        } catch (Exception e) {
            log.error("Error deleting product variant. Product ID: {}, Variant ID: {}: {}",
                    productId, variantId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete product variant: " + e.getMessage(), e);
        }
    }

    /**
     * Delete all images associated with a product variant
     * 
     * @param variant The product variant
     */
    private void deleteVariantImages(ProductVariant variant) {
        try {
            List<ProductVariantImage> variantImages = productVariantImageRepository
                    .findByProductVariantId(variant.getId());

            if (variantImages.isEmpty()) {
                log.debug("No images found for variant ID: {}", variant.getId());
                return;
            }

            log.info("Deleting {} images for variant ID: {}", variantImages.size(), variant.getId());

            for (ProductVariantImage image : variantImages) {
                try {
                    // Delete from Cloudinary
                    if (image.getImageUrl() != null && !image.getImageUrl().isEmpty()) {
                        cloudinaryService.deleteImage(image.getImageUrl());
                        log.debug("Deleted image from Cloudinary: {}", image.getImageUrl());
                    }
                } catch (Exception e) {
                    log.warn("Failed to delete image from Cloudinary: {}. Error: {}",
                            image.getImageUrl(), e.getMessage());
                    // Continue with other images even if one fails
                }
            }

            // Delete all variant images from database
            productVariantImageRepository.deleteAll(variantImages);
            log.info("Successfully deleted {} variant images from database for variant ID: {}",
                    variantImages.size(), variant.getId());

        } catch (Exception e) {
            log.error("Error deleting variant images for variant ID {}: {}", variant.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to delete variant images: " + e.getMessage(), e);
        }
    }

    /**
     * Delete all attribute values associated with a product variant
     * 
     * @param variant The product variant
     */
    private void deleteVariantAttributes(ProductVariant variant) {
        try {
            List<VariantAttributeValue> attributeValues = variantAttributeValueRepository
                    .findByProductVariantId(variant.getId());

            if (attributeValues.isEmpty()) {
                log.debug("No attribute values found for variant ID: {}", variant.getId());
                return;
            }

            log.info("Deleting {} attribute values for variant ID: {}", attributeValues.size(), variant.getId());

            // Delete all variant attribute values from database
            variantAttributeValueRepository.deleteAll(attributeValues);

            log.info("Successfully deleted {} attribute values from database for variant ID: {}",
                    attributeValues.size(), variant.getId());

        } catch (Exception e) {
            log.error("Error deleting variant attributes for variant ID {}: {}", variant.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to delete variant attributes: " + e.getMessage(), e);
        }
    }

    /**
     * Map a Product entity to ManyProductsDto for card display
     * 
     * @param product The product entity
     * @return ManyProductsDto with essential fields for card display
     */
    private ManyProductsDto mapProductToManyProductsDto(Product product) {
        try {
            // Find the primary image for the product
            ProductImage primaryImage = null;
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                primaryImage = product.getImages().stream()
                        .filter(ProductImage::isPrimary)
                        .findFirst()
                        .orElse(product.getImages().get(0)); // Fallback to first image if no primary
            }

            return ManyProductsDto.builder()
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .shortDescription(product.getProductDetail() != null ? product.getProductDetail().getDescription() : null)
                    .price(product.getPrice())
                    .compareAtPrice(product.getCompareAtPrice())
                    .stockQuantity(product.getStockQuantity())
                    .category(product.getCategory())
                    .brand(product.getBrand())
                    .isBestSeller(product.isBestseller())
                    .isFeatured(product.isFeatured())
                    .discountInfo(product.getDiscount())
                    .primaryImage(primaryImage)
                    .build();

        } catch (Exception e) {
            log.error("Error mapping product to ManyProductsDto for product ID {}: {}", 
                    product.getProductId(), e.getMessage(), e);
            throw new RuntimeException("Failed to map product to ManyProductsDto: " + e.getMessage(), e);
        }
    }
}