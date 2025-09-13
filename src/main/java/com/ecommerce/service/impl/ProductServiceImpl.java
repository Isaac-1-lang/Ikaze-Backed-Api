package com.ecommerce.service.impl;

import com.ecommerce.dto.CreateProductDTO;

import com.ecommerce.dto.CreateProductVariantDTO;

import com.ecommerce.dto.ImageMetadata;

import com.ecommerce.dto.ManyProductsDto;

import com.ecommerce.dto.ProductDTO;

import com.ecommerce.dto.ProductSearchDTO;

import com.ecommerce.dto.ProductUpdateDTO;

import com.ecommerce.dto.ProductVariantDTO;

import com.ecommerce.dto.VariantAttributeDTO;

import com.ecommerce.dto.VariantImageMetadata;

import com.ecommerce.dto.VideoMetadata;
import com.ecommerce.dto.ReviewDTO;
import com.ecommerce.dto.WarehouseStockDTO;

import com.ecommerce.entity.*;

import com.ecommerce.repository.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;

import jakarta.annotation.PostConstruct;

import com.ecommerce.Exception.ProductDeletionException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import java.security.SecureRandom;

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

    private final WarehouseRepository warehouseRepository;

    private final StockRepository stockRepository;

    private final CloudinaryService cloudinaryService;

    private final ElasticsearchClient elasticsearchClient;

    // Using thread pool for concurrent image/video uploads

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override

    @Transactional

    public ProductDTO createProduct(CreateProductDTO createProductDTO, List<MultipartFile> productImages,
            List<MultipartFile> productVideos) {
        Product savedProduct = null;
        try {

            log.info("Starting product creation for: {}", createProductDTO.getName());

            Category category = validateAndGetCategory(createProductDTO.getCategoryId());

            // SKU resolution will happen after brand/discount validation

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

            // Resolve or generate SKU now that we know category and brand

            String resolvedSku = resolveOrGenerateSku(

                    createProductDTO.getSku(),

                    createProductDTO.getName(),

                    category,

                    brand);

            Product product = createProductEntity(createProductDTO, category, brand, discount);

            product.setSku(resolvedSku);

            savedProduct = productRepository.save(product);

            // Verify the product was saved successfully
            if (savedProduct.getProductId() == null) {
                throw new RuntimeException("Failed to save product - no ID generated");
            }

            log.info("Product saved with ID: {}", savedProduct.getProductId());

            // Flush to ensure the product is committed to the database
            productRepository.flush();

            // Handle warehouse stock assignments for products without variants
            if (createProductDTO.getVariants() == null || createProductDTO.getVariants().isEmpty()) {
                log.info("Product has no variants, creating warehouse stock assignments for product");
                createProductStockAssignments(savedProduct, createProductDTO.getWarehouseStock());
            } else {
                log.info("Product has variants, warehouse stock will be assigned to variants instead");
            }

            if (productImages != null && !productImages.isEmpty()) {
                try {
                    processProductImages(savedProduct, productImages,
                            createProductDTO.getImageMetadata());
                    log.info("Successfully processed {} product images", productImages.size());
                } catch (Exception e) {
                    log.error("Failed to process product images: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to process product images: " + e.getMessage(), e);
                }
            }

            // Process product videos
            if (productVideos != null && !productVideos.isEmpty()) {
                try {
                    processProductVideos(savedProduct, productVideos,
                            parseVideoMetadataFromString(createProductDTO.getVideoMetadata()));
                    log.info("Successfully processed {} product videos", productVideos.size());
                } catch (Exception e) {
                    log.error("Failed to process product videos: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to process product videos: " + e.getMessage(), e);
                }
            }

            // Process variants
            log.info("Checking for variants: variants={}, size={}",
                    createProductDTO.getVariants() != null ? "not null" : "null",
                    createProductDTO.getVariants() != null ? createProductDTO.getVariants().size() : "N/A");

            if (createProductDTO.getVariants() != null && !createProductDTO.getVariants().isEmpty()) {
                log.info("Processing {} variants", createProductDTO.getVariants().size());
                try {
                    processProductVariants(savedProduct, createProductDTO.getVariants(),
                            createProductDTO.getVariantImages(), createProductDTO.getVariantImageMapping());
                    log.info("Successfully processed {} product variants", createProductDTO.getVariants().size());
                } catch (Exception e) {
                    log.error("Failed to process product variants: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to process product variants: " + e.getMessage(), e);
                }
            } else {
                log.info("No variants to process");
            }

            // Refresh product from database to get all relationships

            Product refreshedProduct = productRepository.findById(savedProduct.getProductId())

                    .orElseThrow(() -> new EntityNotFoundException("Product not found after saving"));

            log.info("Product creation completed successfully for: {}", createProductDTO.getName());

            // Index product in Elasticsearch
            indexProductInElasticsearch(refreshedProduct);

            return mapProductToDTO(refreshedProduct);

        } catch (Exception e) {

            log.error("Error creating product: {}", e.getMessage(), e);

            // If we have a saved product but something failed, try to clean up
            if (savedProduct != null && savedProduct.getProductId() != null) {
                try {
                    log.warn("Attempting to clean up partially created product with ID: {}",
                            savedProduct.getProductId());
                    productRepository.deleteById(savedProduct.getProductId());
                    log.info("Successfully cleaned up partially created product");
                } catch (Exception cleanupError) {
                    log.error("Failed to clean up partially created product: {}", cleanupError.getMessage(),
                            cleanupError);
                }
            }

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

        if (sku == null || sku.trim().isEmpty()) {

            return;

        }

        if (productRepository.findBySku(sku).isPresent()) {

            throw new IllegalArgumentException("Product with SKU " + sku + " already exists");

        }

    }

    private String resolveOrGenerateSku(String requestedSku, String productName, Category category, Brand brand) {

        if (requestedSku != null && !requestedSku.trim().isEmpty()) {

            validateSkuUniqueness(requestedSku);

            return requestedSku.trim().toUpperCase();

        }

        // Build a base SKU: CAT-Brand-Name + 6 random alphanumerics

        String categoryPart = category != null && category.getName() != null

                ? abbreviate(category.getName(), 3)

                : "GEN";

        String brandPart = brand != null && brand.getBrandName() != null

                ? abbreviate(brand.getBrandName(), 3)

                : "NON";

        String namePart = productName != null ? abbreviate(productName, 4) : "PRD";

        String base = String.join("-", categoryPart, brandPart, namePart);

        String candidate;

        int attempts = 0;

        do {

            String suffix = randomAlphaNum(6);

            candidate = (base + "-" + suffix).toUpperCase();

            attempts++;

            if (attempts > 10) {

                // fallback to a timestamp-based suffix to avoid rare collisions

                candidate = (base + "-" + System.currentTimeMillis()).toUpperCase();

                break;

            }

        } while (productRepository.findBySku(candidate).isPresent());

        return candidate;

    }

    private String abbreviate(String input, int maxLen) {

        String cleaned = input.replaceAll("[^A-Za-z0-9]", "");

        if (cleaned.isEmpty())

            return "X".repeat(Math.max(1, maxLen)).substring(0, maxLen);

        cleaned = cleaned.toUpperCase();

        return cleaned.length() <= maxLen ? cleaned : cleaned.substring(0, maxLen);

    }

    private static final char[] ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private String randomAlphaNum(int len) {

        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {

            sb.append(ALPHANUM[RANDOM.nextInt(ALPHANUM.length)]);

        }

        return sb.toString();

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

        // SKU set externally before save using resolved logic

        product.setBarcode(createProductDTO.getBarcode());

        product.setPrice(createProductDTO.getBasePrice());

        product.setCompareAtPrice(createProductDTO.getSalePrice());

        product.setCostPrice(createProductDTO.getCostPrice());

        // TODO: Implement proper stock management through Stock entities
        // product.setStockQuantity(createProductDTO.getStockQuantity() != null ?
        // createProductDTO.getStockQuantity() : 0);
        // product.setLowStockThreshold(createProductDTO.getLowStockThreshold() != null
        // ? createProductDTO.getLowStockThreshold() : 5);

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

            // Index product in Elasticsearch
            indexProductInElasticsearch(refreshedProduct);

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
     * 
     * Check if there are any pending orders for the product variants
     * 
     * that would prevent deletion
     * 
     */

    private void checkForPendingOrders(Product product) {

        if (product.getVariants() == null || product.getVariants().isEmpty()) {

            log.debug("No variants found for product, skipping order check");

            return;

        }

        for (ProductVariant variant : product.getVariants()) {

            if (orderRepository.existsByProductVariantAndNotDelivered(variant.getId())) {

                String errorMessage = String.format(

                        "Cannot delete product '%s' because variant '%s' has pending orders that are not yet delivered. "

                                +

                                "Please ensure all orders are delivered, cancelled, refunded, or returned before deleting the product.",

                        product.getProductName(), variant.getVariantSku());

                log.warn("Product deletion blocked due to pending orders for variant: {}", variant.getVariantSku());

                throw new ProductDeletionException(errorMessage);

            }

        }

        log.debug("No pending orders found for product variants, deletion can proceed");

    }

    /**
     * 
     * Remove product variants from all carts and wishlists
     * 
     */

    private void removeProductFromCartsAndWishlists(Product product) {

        if (product.getVariants() == null || product.getVariants().isEmpty()) {

            log.debug("No variants found for product, skipping cart/wishlist cleanup");

            return;

        }

        log.info("Removing product variants from carts and wishlists");

        for (ProductVariant variant : product.getVariants()) {

            try {

                cartRepository.deleteCartItemsByProductVariant(variant.getId());

                log.debug("Removed variant {} from all carts", variant.getVariantSku());
                log.debug("Skipping wishlist cleanup for variant {} (wishlist is now product-based)",
                        variant.getVariantSku());

            } catch (Exception e) {

                log.warn("Failed to remove variant {} from carts/wishlists: {}", variant.getVariantSku(),

                        e.getMessage());
            }

        }

        log.info("Successfully removed product variants from carts and wishlists");

    }

    /**
     * 
     * Delete all product variants and their associated data
     * 
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
     * 
     * Delete main product images and videos
     * 
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
     * 
     * Delete product detail
     * 
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

            boolean isTextSearch = (searchDTO.getName() != null && !searchDTO.getName().trim().isEmpty()) ||
                    (searchDTO.getSearchKeyword() != null && !searchDTO.getSearchKeyword().trim().isEmpty());

            if (isTextSearch) {
                // Use Elasticsearch for text search
                return searchProductsWithElasticsearch(searchDTO, page, size, sortBy, sortDirection);
            } else {
                // Use JPA Specification for other filters
                Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
                Specification<Product> spec = buildProductSearchSpecification(searchDTO);
                Page<Product> productPage = productRepository.findAll(spec, pageable);
                Page<ManyProductsDto> result = productPage.map(this::mapProductToManyProductsDto);
                log.info("Search completed. Found {} products matching criteria", result.getTotalElements());
                return result;
            }

        } catch (Exception e) {
            log.error("Error searching products: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search products: " + e.getMessage(), e);
        }
    }

    private Page<ManyProductsDto> searchProductsWithElasticsearch(ProductSearchDTO searchDTO, int page, int size,
            String sortBy, String sortDirection) {
        try {
            ensureProductsIndexExists();

            String searchTerm = searchDTO.getName() != null ? searchDTO.getName() : searchDTO.getSearchKeyword();

            // Create multi-match query with typo tolerance
            Query multiMatchQuery = MultiMatchQuery.of(m -> m
                    .query(searchTerm)
                    .fields("productName^3", "shortDescription^2", "metaKeywords^2", "searchKeywords^2",
                            "metaKeywordsArray^3", "searchKeywordsArray^3", "metaDescription", "description", "sku^2",
                            "slug", "barcode")
                    .fuzziness("AUTO")
                    .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields))._toQuery();

            // Create search request with pagination
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("products")
                    .query(multiMatchQuery)
                    .from(page * size)
                    .size(size));
            SearchResponse<Map> searchResponse = elasticsearchClient.search(searchRequest, Map.class);

            // Get product IDs from Elasticsearch results
            List<String> productIds = searchResponse.hits().hits().stream()
                    .map(hit -> (String) hit.source().get("productId"))
                    .collect(Collectors.toList());

            if (productIds.isEmpty()) {
                // Return empty page
                return new org.springframework.data.domain.PageImpl<>(
                        Collections.emptyList(),
                        PageRequest.of(page, size),
                        0);
            }

            // Fetch products from database using the IDs
            List<Product> products = productRepository.findByProductIdInOrderByCreatedAtDesc(
                    productIds.stream().map(UUID::fromString).collect(Collectors.toList()));

            // Apply additional filters if any
            List<Product> filteredProducts = applyAdditionalFilters(products, searchDTO);

            // Calculate pagination
            int totalElements = filteredProducts.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);

            List<Product> pageProducts = startIndex < totalElements ? filteredProducts.subList(startIndex, endIndex)
                    : Collections.emptyList();

            // Map to DTOs
            List<ManyProductsDto> dtoList = pageProducts.stream()
                    .map(this::mapProductToManyProductsDto)
                    .collect(Collectors.toList());

            log.info("Elasticsearch search completed. Found {} products matching criteria", totalElements);

            return new org.springframework.data.domain.PageImpl<>(
                    dtoList,
                    PageRequest.of(page, size),
                    totalElements);

        } catch (Exception e) {
            log.warn("Elasticsearch search failed, falling back to JPA search: {}", e.getMessage());
            // Fallback to JPA search with improved text search
            return searchProductsWithJPA(searchDTO, page, size, sortBy, sortDirection);
        }
    }

    private Page<ManyProductsDto> searchProductsWithJPA(ProductSearchDTO searchDTO, int page, int size, String sortBy,
            String sortDirection) {
        try {
            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            String searchTerm = searchDTO.getName() != null ? searchDTO.getName() : searchDTO.getSearchKeyword();

            // Use enhanced search with comma-separated keyword handling
            List<Product> allMatchingProducts = findProductsWithCommaSeparatedKeywords(searchTerm);

            // Apply additional filters
            List<Product> filteredProducts = applyAdditionalFilters(allMatchingProducts, searchDTO);

            // Apply sorting
            filteredProducts = applySorting(filteredProducts, sortBy, sortDirection);

            // Apply pagination
            int totalElements = filteredProducts.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);

            List<Product> pageProducts = startIndex < totalElements ? filteredProducts.subList(startIndex, endIndex)
                    : Collections.emptyList();

            // Map to DTOs
            List<ManyProductsDto> dtoList = pageProducts.stream()
                    .map(this::mapProductToManyProductsDto)
                    .collect(Collectors.toList());

            Page<ManyProductsDto> result = new org.springframework.data.domain.PageImpl<>(
                    dtoList, pageable, totalElements);

            log.info("JPA enhanced search with comma-separated keywords completed. Found {} products matching criteria",
                    result.getTotalElements());
            return result;

        } catch (Exception e) {
            log.error("JPA enhanced search failed, trying basic search: {}", e.getMessage());
            // Fallback to basic search
            try {
                String searchTerm = searchDTO.getName() != null ? searchDTO.getName() : searchDTO.getSearchKeyword();
                Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

                Page<Product> productPage = productRepository
                        .findByProductNameContainingIgnoreCaseOrShortDescriptionContainingIgnoreCase(
                                searchTerm, searchTerm, pageable);

                Page<ManyProductsDto> result = productPage.map(this::mapProductToManyProductsDto);
                log.info("JPA basic search completed. Found {} products matching criteria", result.getTotalElements());
                return result;

            } catch (Exception e2) {
                log.error("JPA basic search also failed: {}", e2.getMessage(), e2);
                // Return empty page as last resort
                return new org.springframework.data.domain.PageImpl<>(
                        Collections.emptyList(),
                        PageRequest.of(page, size),
                        0);
            }
        }
    }

    private List<Product> applyAdditionalFilters(List<Product> products, ProductSearchDTO searchDTO) {
        return products.stream()
                .filter(product -> {
                    // Apply category filter
                    if (searchDTO.getCategoryId() != null) {
                        return product.getCategory() != null &&
                                product.getCategory().getId().equals(searchDTO.getCategoryId());
                    }

                    // Apply brand filter
                    if (searchDTO.getBrandId() != null) {
                        return product.getBrand() != null &&
                                product.getBrand().getBrandId().equals(searchDTO.getBrandId());
                    }

                    // Apply price filters
                    if (searchDTO.getBasePriceMin() != null) {
                        if (product.getPrice().compareTo(searchDTO.getBasePriceMin()) < 0) {
                            return false;
                        }
                    }
                    if (searchDTO.getBasePriceMax() != null) {
                        if (product.getPrice().compareTo(searchDTO.getBasePriceMax()) > 0) {
                            return false;
                        }
                    }

                    // Apply stock filter
                    if (searchDTO.getInStock() != null && searchDTO.getInStock()) {
                        return product.getTotalStockQuantity() > 0;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Enhanced search method that properly handles comma-separated keywords
     * in metaKeywords and searchKeywords fields
     */
    private List<Product> findProductsWithCommaSeparatedKeywords(String searchTerm) {
        try {
            log.debug("Searching products with comma-separated keyword handling for term: {}", searchTerm);

            // First, get products that match basic fields (name, description, etc.)
            List<Product> basicMatches = productRepository.findProductsForKeywordSearch(searchTerm);

            // Then, find products that match comma-separated keywords
            List<Product> keywordMatches = findProductsMatchingCommaSeparatedKeywords(searchTerm);

            // Combine and deduplicate results
            Set<Product> allMatches = new HashSet<>();
            allMatches.addAll(basicMatches);
            allMatches.addAll(keywordMatches);

            List<Product> result = new ArrayList<>(allMatches);
            log.debug("Found {} products matching search term '{}'", result.size(), searchTerm);

            return result;

        } catch (Exception e) {
            log.error("Error in comma-separated keyword search: {}", e.getMessage(), e);
            // Fallback to basic search
            return productRepository.findProductsForKeywordSearch(searchTerm);
        }
    }

    /**
     * Find products that match comma-separated keywords in metaKeywords and
     * searchKeywords
     */
    private List<Product> findProductsMatchingCommaSeparatedKeywords(String searchTerm) {
        List<Product> matchingProducts = new ArrayList<>();
        String lowerSearchTerm = searchTerm.toLowerCase().trim();

        try {
            // Get all active products with their details
            List<Product> allProducts = productRepository.findAll().stream()
                    .filter(Product::isActive)
                    .collect(Collectors.toList());

            for (Product product : allProducts) {
                if (product.getProductDetail() != null) {
                    ProductDetail detail = product.getProductDetail();

                    // Check metaKeywords
                    if (detail.getMetaKeywords() != null && !detail.getMetaKeywords().trim().isEmpty()) {
                        if (matchesCommaSeparatedKeywords(detail.getMetaKeywords(), lowerSearchTerm)) {
                            matchingProducts.add(product);
                            continue; // Avoid adding the same product twice
                        }
                    }

                    // Check searchKeywords
                    if (detail.getSearchKeywords() != null && !detail.getSearchKeywords().trim().isEmpty()) {
                        if (matchesCommaSeparatedKeywords(detail.getSearchKeywords(), lowerSearchTerm)) {
                            matchingProducts.add(product);
                        }
                    }
                }
            }

            log.debug("Found {} products matching comma-separated keywords for term '{}'",
                    matchingProducts.size(), searchTerm);

        } catch (Exception e) {
            log.error("Error finding products with comma-separated keywords: {}", e.getMessage(), e);
        }

        return matchingProducts;
    }

    /**
     * Check if any keyword in a comma-separated string matches the search term
     */
    private boolean matchesCommaSeparatedKeywords(String keywordsString, String searchTerm) {
        if (keywordsString == null || keywordsString.trim().isEmpty()) {
            return false;
        }

        try {
            // Split by comma and check each keyword
            String[] keywords = keywordsString.split(",");
            for (String keyword : keywords) {
                String trimmedKeyword = keyword.trim().toLowerCase();
                if (!trimmedKeyword.isEmpty() && trimmedKeyword.contains(searchTerm)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Error processing comma-separated keywords '{}': {}", keywordsString, e.getMessage());
        }

        return false;
    }

    /**
     * Apply sorting to a list of products
     */
    private List<Product> applySorting(List<Product> products, String sortBy, String sortDirection) {
        try {
            Comparator<Product> comparator = getProductComparator(sortBy, sortDirection);
            return products.stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error applying sorting, using default sort: {}", e.getMessage());
            return products.stream()
                    .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
                    .collect(Collectors.toList());
        }
    }

    /**
     * Get comparator for product sorting
     */
    private Comparator<Product> getProductComparator(String sortBy, String sortDirection) {
        boolean ascending = "asc".equalsIgnoreCase(sortDirection);

        Comparator<Product> comparator;
        switch (sortBy.toLowerCase()) {
            case "name":
            case "productname":
                comparator = Comparator.comparing(Product::getProductName);
                break;
            case "price":
                comparator = Comparator.comparing(Product::getPrice);
                break;
            case "createdat":
            case "created_at":
                comparator = Comparator.comparing(Product::getCreatedAt);
                break;
            case "updatedat":
            case "updated_at":
                comparator = Comparator.comparing(Product::getUpdatedAt);
                break;
            case "averagerating":
            case "average_rating":
                comparator = Comparator.comparing(Product::getAverageRating);
                break;
            default:
                comparator = Comparator.comparing(Product::getCreatedAt);
                break;
        }

        return ascending ? comparator : comparator.reversed();
    }

    /**
     * 
     * Build a JPA Specification for product search based on the search DTO
     * 
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

                predicates

                        .add(criteriaBuilder.greaterThanOrEqualTo(root.get("salePrice"), searchDTO.getSalePriceMin()));

            }

            if (searchDTO.getSalePriceMax() != null) {

                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("salePrice"), searchDTO.getSalePriceMax()));

            }

            if (searchDTO.getCompareAtPriceMin() != null) {

                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("compareAtPrice"),

                        searchDTO.getCompareAtPriceMin()));

            }

            if (searchDTO.getCompareAtPriceMax() != null) {

                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("compareAtPrice"),

                        searchDTO.getCompareAtPriceMax()));

            }

            // TODO: Implement proper stock management through Stock entities
            // Stock filters
            // if (searchDTO.getStockQuantityMin() != null) {
            // predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("stockQuantity"),
            // searchDTO.getStockQuantityMin()));
            // }
            // if (searchDTO.getStockQuantityMax() != null) {
            // predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("stockQuantity"),
            // searchDTO.getStockQuantityMax()));
            // }
            // if (searchDTO.getInStock() != null) {
            // if (searchDTO.getInStock()) {
            // predicates.add(criteriaBuilder.greaterThan(root.get("stockQuantity"), 0));
            // } else {
            // predicates.add(criteriaBuilder.equal(root.get("stockQuantity"), 0));
            // }
            // }

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

                predicates

                        .add(criteriaBuilder.equal(root.get("discount").get("discountId"), searchDTO.getDiscountId()));

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

                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("discount").get("startDate"),

                            LocalDateTime.now()));

                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("discount").get("endDate"),

                            LocalDateTime.now()));

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

                predicates

                        .add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), searchDTO.getCreatedAtMin()));

            }

            if (searchDTO.getCreatedAtMax() != null) {

                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), searchDTO.getCreatedAtMax()));

            }

            if (searchDTO.getUpdatedAtMin() != null) {

                predicates

                        .add(criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), searchDTO.getUpdatedAtMin()));

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

                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")),

                        "%" + keyword + "%");

                Predicate descPredicate = criteriaBuilder

                        .like(criteriaBuilder.lower(root.get("productDetail").get("description")), "%" + keyword + "%");

                Predicate skuPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")),

                        "%" + keyword + "%");

                Predicate barcodePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("barcode")),

                        "%" + keyword + "%");

                predicates.add(criteriaBuilder.or(namePredicate, descPredicate, skuPredicate, barcodePredicate));

            }

            // Combine all predicates with AND

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };

    }

    private void processProductImages(Product product, List<String> imageUrls,

            String imageMetadataJson) {

        // Parse metadata from JSON string if provided

        List<ImageMetadata> metadata = null;

        if (imageMetadataJson != null && !imageMetadataJson.trim().isEmpty()) {

            try {

                // For now, we'll create a simple metadata object

                // In a real implementation, you'd parse the JSON

                metadata = new ArrayList<>();

                // TODO: Implement JSON parsing for metadata

            } catch (Exception e) {

                log.warn("Failed to parse image metadata JSON: {}", e.getMessage());

            }

        }

        // Process image URLs directly (assuming they're already uploaded)

        for (int i = 0; i < imageUrls.size(); i++) {

            String imageUrl = imageUrls.get(i);

            ProductImage productImage = new ProductImage();

            productImage.setProduct(product);

            productImage.setImageUrl(imageUrl);

            productImage.setSortOrder(i);

            // Set first image as primary

            if (i == 0) {

                productImage.setPrimary(true);

            }

            productImageRepository.save(productImage);

        }

        log.info("Successfully processed {} product images from URLs", imageUrls.size());

    }

    private void processProductImages(Product product, List<MultipartFile> images,

            List<ImageMetadata> metadata) {

        try {

            log.info("Processing {} product images", images.size());

            // Validate product has a valid ID
            if (product == null || product.getProductId() == null) {
                throw new RuntimeException("Cannot process images for null product or product without ID");
            }

            // Validate file sizes (max 50MB for product images)
            for (int i = 0; i < images.size(); i++) {
                MultipartFile image = images.get(i);
                if (image.getSize() > 50 * 1024 * 1024) {
                    throw new IllegalArgumentException(
                            String.format("Product image '%s' file size (%.2f MB) exceeds maximum allowed (50 MB)",
                                    image.getOriginalFilename(), image.getSize() / (1024.0 * 1024.0)));
                }
            }

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

                    ImageMetadata imgMetadata = metadata.get(i);

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

            // Check if it's a foreign key constraint violation
            if (e.getMessage() != null && e.getMessage().contains("violates foreign key constraint")) {
                throw new RuntimeException("Product was not properly saved before processing images. Please try again.",
                        e);
            }
            throw new RuntimeException("Failed to process product images: " + e.getMessage(), e);

        }

    }

    private void processProductVideos(Product product, List<String> videoUrls,

            String videoMetadataJson) {

        // Parse metadata from JSON string if provided

        List<VideoMetadata> metadata = parseVideoMetadataFromString(videoMetadataJson);

        // Process video URLs directly (assuming they're already uploaded)

        for (int i = 0; i < videoUrls.size(); i++) {

            String videoUrl = videoUrls.get(i);

            ProductVideo productVideo = new ProductVideo();

            productVideo.setProduct(product);

            productVideo.setUrl(videoUrl);

            productVideoRepository.save(productVideo);

        }

        log.info("Successfully processed {} product videos from URLs", videoUrls.size());

    }

    private List<VideoMetadata> parseVideoMetadataFromString(String videoMetadataJson) {
        List<VideoMetadata> metadata = new ArrayList<>();
        if (videoMetadataJson != null && !videoMetadataJson.trim().isEmpty()) {
            try {
                // For now, we'll create a simple metadata object
                // In a real implementation, you'd parse the JSON
                // TODO: Implement JSON parsing for metadata
            } catch (Exception e) {
                log.warn("Failed to parse video metadata JSON: {}", e.getMessage());
            }
        }
        return metadata;
    }

    private List<ImageMetadata> parseImageMetadataFromString(String imageMetadataJson) {
        List<ImageMetadata> metadata = new ArrayList<>();
        if (imageMetadataJson != null && !imageMetadataJson.trim().isEmpty()) {
            try {
                // For now, we'll create a simple metadata object
                // In a real implementation, you'd parse the JSON
                // TODO: Implement JSON parsing for metadata
            } catch (Exception e) {
                log.warn("Failed to parse image metadata JSON: {}", e.getMessage());
            }
        }
        return metadata;
    }

    private List<VariantImageMetadata> parseVariantImageMetadataFromString(String imageMetadataJson) {
        List<VariantImageMetadata> metadata = new ArrayList<>();
        if (imageMetadataJson != null && !imageMetadataJson.trim().isEmpty()) {
            try {
                // For now, we'll create a simple metadata object
                // In a real implementation, you'd parse the JSON
                // TODO: Implement JSON parsing for metadata
            } catch (Exception e) {
                log.warn("Failed to parse variant image metadata JSON: {}", e.getMessage());
            }
        }
        return metadata;
    }

    private void processProductVideos(Product product, List<MultipartFile> videos,

            List<VideoMetadata> metadata) {

        try {

            log.info("Processing {} product videos", videos.size());

            // Validate product has a valid ID
            if (product == null || product.getProductId() == null) {
                throw new RuntimeException("Cannot process videos for null product or product without ID");
            }

            // Validate video duration (max 30 seconds) and file size
            for (int i = 0; i < videos.size(); i++) {
                MultipartFile video = videos.get(i);

                // Check file size (max 100MB)
                if (video.getSize() > 100 * 1024 * 1024) {
                    throw new IllegalArgumentException(
                            String.format("Video '%s' file size (%.2f MB) exceeds maximum allowed (100 MB)",
                                    video.getOriginalFilename(), video.getSize() / (1024.0 * 1024.0)));
                }

                // Validate video duration if metadata provided
                if (metadata != null && i < metadata.size()) {
                    VideoMetadata videoMetadata = metadata.get(i);
                    if (videoMetadata.getDuration() != null && videoMetadata.getDuration() > 30) {
                        throw new IllegalArgumentException(
                                String.format("Video '%s' duration (%d seconds) exceeds maximum allowed (30 seconds)",
                                        video.getOriginalFilename(), videoMetadata.getDuration()));
                    }
                }
            }

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

                    VideoMetadata vidMetadata = metadata.get(i);

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

            // Check if it's a foreign key constraint violation
            if (e.getMessage() != null && e.getMessage().contains("violates foreign key constraint")) {
                throw new RuntimeException("Product was not properly saved before processing videos. Please try again.",
                        e);
            }
            throw new RuntimeException("Failed to process product videos: " + e.getMessage(), e);

        }

    }

    private void processProductVariants(Product product, List<CreateProductVariantDTO> variantDTOs,
            List<MultipartFile> variantImages, String variantImageMapping) {

        log.info("Processing {} product variants", variantDTOs.size());
        log.info("Variant images available: {}", variantImages != null ? variantImages.size() : "null");
        log.info("Variant image mapping: {}", variantImageMapping);

        if (variantImages != null && !variantImages.isEmpty()) {
            for (int i = 0; i < variantImages.size(); i++) {
                log.debug("Variant image {}: {} (size: {} bytes)", i,
                        variantImages.get(i).getOriginalFilename(), variantImages.get(i).getSize());
            }
        }

        // Validate product has a valid ID
        if (product == null || product.getProductId() == null) {
            throw new RuntimeException("Cannot process variants for null product or product without ID");
        }

        try {
            for (int i = 0; i < variantDTOs.size(); i++) {
                CreateProductVariantDTO variantDTO = variantDTOs.get(i);
                log.info("Processing variant {}/{}: {}", i + 1, variantDTOs.size(), variantDTO.getVariantSku());

                // Validate variant SKU uniqueness if provided
                if (variantDTO.getVariantSku() != null &&
                        productVariantRepository.findByVariantSku(variantDTO.getVariantSku()).isPresent()) {
                    throw new IllegalArgumentException(
                            "Variant with SKU " + variantDTO.getVariantSku() + " already exists");
                }

                // Create variant
                ProductVariant variant = createProductVariant(product, variantDTO);
                log.debug("Created variant entity for SKU: {}", variant.getVariantSku());

                ProductVariant savedVariant = productVariantRepository.save(variant);
                log.info("Saved variant with ID: {} and SKU: {}", savedVariant.getId(), savedVariant.getVariantSku());

                // Handle warehouse stock assignments for this variant
                createVariantStockAssignments(savedVariant, variantDTO.getWarehouseStock());

                // Process variant attributes if any
                if (variantDTO.getAttributes() != null && !variantDTO.getAttributes().isEmpty()) {
                    log.debug("Processing {} attributes for variant {}", variantDTO.getAttributes().size(),
                            savedVariant.getId());
                    processVariantAttributes(savedVariant, variantDTO.getAttributes());
                } else {
                    log.debug("No attributes to process for variant {}", savedVariant.getId());
                }

                // Process variant images from the main variant images list if available
                // (primary method)
                if (variantImages != null && !variantImages.isEmpty() && variantImageMapping != null) {
                    try {
                        List<MultipartFile> variantSpecificImages = getVariantImagesForIndex(variantImages,
                                variantImageMapping, i);
                        if (!variantSpecificImages.isEmpty()) {
                            log.info("Processing {} images for variant {} (SKU: {})", variantSpecificImages.size(),
                                    savedVariant.getId(), savedVariant.getVariantSku());
                            processVariantImages(savedVariant, variantSpecificImages, null);
                        } else {
                            log.debug("No images mapped for variant {} (SKU: {})", savedVariant.getId(),
                                    savedVariant.getVariantSku());
                        }
                    } catch (Exception e) {
                        log.error("Failed to process variant images for variant {} (SKU: {}): {}",
                                savedVariant.getId(), savedVariant.getVariantSku(), e.getMessage(), e);
                    }
                } else {
                    log.debug("No variant images or mapping available for variant {} (SKU: {})",
                            savedVariant.getId(), savedVariant.getVariantSku());
                }

                // Also check if variant has its own embedded images (for backward
                // compatibility)
                if (variantDTO.getVariantImages() != null && !variantDTO.getVariantImages().isEmpty()) {
                    log.info("Processing {} embedded images for variant {} (SKU: {})",
                            variantDTO.getVariantImages().size(), savedVariant.getId(),
                            savedVariant.getVariantSku());
                    processVariantImages(savedVariant, variantDTO.getVariantImages(),
                            parseVariantImageMetadataFromString(variantDTO.getImageMetadata()));
                }

                log.info("Successfully processed variant {}/{} with ID: {}", i + 1, variantDTOs.size(),
                        savedVariant.getId());
            }

            log.info("Successfully processed {} product variants", variantDTOs.size());
        } catch (Exception e) {
            log.error("Error processing product variants: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process product variants: " + e.getMessage(), e);
        }
    }

    /**
     * Get variant images for a specific variant index based on the mapping
     */
    private List<MultipartFile> getVariantImagesForIndex(List<MultipartFile> allVariantImages,
            String variantImageMapping, int variantIndex) {
        try {
            if (variantImageMapping == null || variantImageMapping.trim().isEmpty()) {
                log.debug("No variant image mapping provided for variant index {}", variantIndex);
                return new ArrayList<>();
            }

            log.debug("Parsing variant image mapping: {}", variantImageMapping);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, List<Integer>> mapping = objectMapper.readValue(variantImageMapping,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, List.class));

            log.debug("Parsed mapping: {}", mapping);
            List<Integer> imageIndices = mapping.get(String.valueOf(variantIndex));
            if (imageIndices == null || imageIndices.isEmpty()) {
                log.debug("No image indices found for variant index {}", variantIndex);
                return new ArrayList<>();
            }

            log.debug("Found image indices {} for variant index {}", imageIndices, variantIndex);
            List<MultipartFile> variantImages = new ArrayList<>();
            for (Integer index : imageIndices) {
                if (index >= 0 && index < allVariantImages.size()) {
                    variantImages.add(allVariantImages.get(index));
                    log.debug("Added image {} (filename: {}) for variant index {}",
                            index, allVariantImages.get(index).getOriginalFilename(), variantIndex);
                } else {
                    log.warn("Invalid image index {} for variant index {} (total images: {})",
                            index, variantIndex, allVariantImages.size());
                }
            }

            log.info("Returning {} images for variant index {}", variantImages.size(), variantIndex);
            return variantImages;
        } catch (Exception e) {
            log.error("Failed to parse variant image mapping: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private ProductVariant createProductVariant(Product product, CreateProductVariantDTO variantDTO) {

        ProductVariant variant = new ProductVariant();

        variant.setProduct(product);

        variant.setVariantSku(variantDTO.getVariantSku());

        variant.setVariantBarcode(variantDTO.getVariantBarcode());

        variant.setPrice(variantDTO.getPrice() != null ? variantDTO.getPrice() : product.getPrice());

        variant.setCompareAtPrice(variantDTO.getSalePrice());

        variant.setCostPrice(variantDTO.getCostPrice());

        // TODO: Implement proper stock management through Stock entities
        // variant.setStockQuantity(variantDTO.getStockQuantity() != null ?
        // variantDTO.getStockQuantity() : 0);
        // variant.setLowStockThreshold(variantDTO.getLowStockThreshold() != null ?
        // variantDTO.getLowStockThreshold() : product.getLowStockThreshold());

        variant.setActive(variantDTO.getIsActive() != null ? variantDTO.getIsActive() : true);

        variant.setSortOrder(variantDTO.getSortOrder() != null ? variantDTO.getSortOrder() : 0);

        return variant;

    }

    private void processVariantAttributes(ProductVariant variant,

            Map<String, String> attributes) {

        if (attributes == null || attributes.isEmpty()) {
            log.debug("No attributes to process for variant {}", variant.getId());
            return;
        }

        log.info("Processing {} attributes for variant {}", attributes.size(), variant.getId());

        try {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                String attributeName = entry.getKey();
                String attributeValue = entry.getValue();

                log.debug("Processing attribute: {} = {} for variant {}", attributeName, attributeValue,
                        variant.getId());

                // Find or create the attribute type by name (case-insensitive)
                ProductAttributeType attributeType = attributeTypeRepository.findByNameIgnoreCase(attributeName)
                        .orElseGet(() -> {
                            log.info("Creating new attribute type: {}", attributeName);
                            // Create new attribute type if it doesn't exist
                            ProductAttributeType newType = new ProductAttributeType();
                            newType.setName(attributeName);
                            newType.setRequired(false);
                            return attributeTypeRepository.save(newType);
                        });

                log.debug("Found/created attribute type: {} with ID: {}", attributeType.getName(),
                        attributeType.getAttributeTypeId());

                // Find or create the attribute value (case-insensitive)
                ProductAttributeValue productAttributeValue = attributeValueRepository
                        .findByValueIgnoreCaseAndAttributeType(attributeValue, attributeType)
                        .orElseGet(() -> {
                            log.info("Creating new attribute value: {} for type: {}", attributeValue, attributeName);
                            ProductAttributeValue newValue = new ProductAttributeValue();
                            newValue.setAttributeType(attributeType);
                            newValue.setValue(attributeValue);
                            return attributeValueRepository.save(newValue);
                        });

                log.debug("Found/created attribute value: {} with ID: {}", productAttributeValue.getValue(),
                        productAttributeValue.getAttributeValueId());

                // Create variant attribute value
                VariantAttributeValue variantAttributeValue = new VariantAttributeValue();
                VariantAttributeValue.VariantAttributeValueId id = new VariantAttributeValue.VariantAttributeValueId(
                        variant.getId(), productAttributeValue.getAttributeValueId());
                variantAttributeValue.setId(id);
                variantAttributeValue.setProductVariant(variant);
                variantAttributeValue.setAttributeValue(productAttributeValue);

                log.debug("Saving variant attribute value with ID: variantId={}, attributeValueId={}",
                        id.getVariantId(), id.getAttributeValueId());

                VariantAttributeValue savedVariantAttribute = variantAttributeValueRepository
                        .save(variantAttributeValue);
                log.debug("Successfully saved variant attribute value with ID: {}", savedVariantAttribute.getId());

            }
            log.info("Successfully processed {} attributes for variant {}", attributes.size(), variant.getId());
        } catch (Exception e) {
            log.error("Error processing attributes for variant {}: {}", variant.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process variant attributes: " + e.getMessage(), e);
        }

    }

    private void processVariantImages(ProductVariant variant, List<MultipartFile> images,

            List<VariantImageMetadata> metadata) {

        try {

            log.info("Processing {} variant images for variant {}", images.size(), variant.getId());

            // Validate file sizes (max 50MB for variant images)
            for (int i = 0; i < images.size(); i++) {
                MultipartFile image = images.get(i);
                if (image.getSize() > 50 * 1024 * 1024) {
                    throw new IllegalArgumentException(
                            String.format("Variant image '%s' file size (%.2f MB) exceeds maximum allowed (50 MB)",
                                    image.getOriginalFilename(), image.getSize() / (1024.0 * 1024.0)));
                }
            }

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

                    VariantImageMetadata imgMetadata = metadata.get(i);

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

        // TODO: Implement proper stock management through Stock entities
        // if (updateDTO.getStockQuantity() != null) {
        // product.setStockQuantity(updateDTO.getStockQuantity());
        // }
        // if (updateDTO.getLowStockThreshold() != null) {
        // product.setLowStockThreshold(updateDTO.getLowStockThreshold());
        // }

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

        dto.setStockQuantity(product.getTotalStockQuantity());

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

        // Map reviews
        if (product.getReviews() != null) {
            dto.setReviews(product.getReviews().stream()
                    .filter(review -> review.isApproved()) // Only include approved reviews
                    .map(this::mapReviewToDTO)
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

    private ReviewDTO mapReviewToDTO(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .userEmail(review.getUser().getUserEmail())
                .productId(review.getProduct().getProductId())
                .productName(review.getProduct().getProductName())
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .status(review.getStatus().name())
                .isVerifiedPurchase(review.isVerifiedPurchase())
                .helpfulVotes(review.getHelpfulVotes())
                .notHelpfulVotes(review.getNotHelpfulVotes())
                .moderatorNotes(review.getModeratorNotes())
                .moderatedBy(review.getModeratedBy())
                .moderatedAt(review.getModeratedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .canEdit(false) // This would be determined by business logic
                .canDelete(false) // This would be determined by business logic
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

        dto.setStockQuantity(variant.getTotalStockQuantity());

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
     * 
     * Delete all images associated with a product variant
     * 
     * 
     * 
     * @param variant The product variant
     * 
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
     * 
     * Delete all attribute values associated with a product variant
     * 
     * 
     * 
     * @param variant The product variant
     * 
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
     * 
     * Map a Product entity to ManyProductsDto for card display
     * 
     * 
     * 
     * @param product The product entity
     * 
     * @return ManyProductsDto with essential fields for card display
     * 
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

            // Build simplified category DTO
            ManyProductsDto.SimpleCategoryDto categoryDto = null;
            if (product.getCategory() != null) {
                categoryDto = ManyProductsDto.SimpleCategoryDto.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .description(product.getCategory().getDescription())
                        .slug(product.getCategory().getSlug())
                        .imageUrl(product.getCategory().getImageUrl())
                        .build();
            }

            // Build simplified brand DTO
            ManyProductsDto.SimpleBrandDto brandDto = null;
            if (product.getBrand() != null) {
                brandDto = ManyProductsDto.SimpleBrandDto.builder()
                        .brandId(product.getBrand().getBrandId())
                        .brandName(product.getBrand().getBrandName())
                        .description(product.getBrand().getDescription())
                        .logoUrl(product.getBrand().getLogoUrl())
                        .build();
            }

            // Build simplified discount DTO
            ManyProductsDto.SimpleDiscountDto discountDto = null;
            if (product.getDiscount() != null) {
                discountDto = ManyProductsDto.SimpleDiscountDto.builder()
                        .discountId(product.getDiscount().getDiscountId())
                        .name(product.getDiscount().getName())
                        .percentage(product.getDiscount().getPercentage())
                        .startDate(product.getDiscount().getStartDate() != null
                                ? product.getDiscount().getStartDate().toString()
                                : null)
                        .endDate(product.getDiscount().getEndDate() != null
                                ? product.getDiscount().getEndDate().toString()
                                : null)
                        .active(product.getDiscount().isActive())
                        .build();
            }

            // Build simplified image DTO
            ManyProductsDto.SimpleProductImageDto imageDto = null;
            if (primaryImage != null) {
                imageDto = ManyProductsDto.SimpleProductImageDto.builder()
                        .id(primaryImage.getId())
                        .imageUrl(primaryImage.getImageUrl())
                        .altText(primaryImage.getAltText())
                        .title(primaryImage.getTitle())
                        .isPrimary(primaryImage.isPrimary())
                        .sortOrder(primaryImage.getSortOrder())
                        .width(primaryImage.getWidth())
                        .height(primaryImage.getHeight())
                        .fileSize(primaryImage.getFileSize())
                        .mimeType(primaryImage.getMimeType())
                        .build();
            }

            return ManyProductsDto.builder()

                    .productId(product.getProductId())

                    .productName(product.getProductName())

                    .shortDescription(

                            product.getProductDetail() != null ? product.getProductDetail().getDescription() : null)

                    .price(product.getPrice())

                    .compareAtPrice(product.getCompareAtPrice())

                    .stockQuantity(product.getTotalStockQuantity())

                    .category(categoryDto)
                    .brand(brandDto)
                    .isBestSeller(product.isBestseller())

                    .isFeatured(product.isFeatured())

                    .discountInfo(discountDto)
                    .primaryImage(imageDto)
                    .averageRating(product.getAverageRating())
                    .reviewCount(product.getReviewCount())
                    .build();

        } catch (Exception e) {

            log.error("Error mapping product to ManyProductsDto for product ID {}: {}",

                    product.getProductId(), e.getMessage(), e);

            throw new RuntimeException("Failed to map product to ManyProductsDto: " + e.getMessage(), e);

        }
    }

    @Override
    public List<Map<String, Object>> getSearchSuggestions(String query) {
        try {
            List<Map<String, Object>> suggestions = new ArrayList<>();

            // Ensure products index exists
            ensureProductsIndexExists();

            // Get search term suggestions with typo tolerance
            suggestions.addAll(getSearchTermSuggestions(query));

            // Get category suggestions
            suggestions.addAll(getCategorySuggestions(query));

            // Get brand suggestions
            suggestions.addAll(getBrandSuggestions(query));

            // Get keyword suggestions from meta keywords
            suggestions.addAll(getKeywordSuggestions(query));

            // Limit total suggestions to 10
            return suggestions.stream().limit(10).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting Elasticsearch search suggestions for query: {}", query, e);
            return getFallbackSearchSuggestions(query);
        }
    }

    private List<Map<String, Object>> getSearchTermSuggestions(String query) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        try {
            // Create multi-match query with typo tolerance for search terms
            Query multiMatchQuery = MultiMatchQuery.of(m -> m
                    .query(query)
                    .fields("productName^3", "shortDescription^2", "metaKeywords", "searchKeywords",
                            "metaKeywordsArray^2", "searchKeywordsArray^2")
                    .fuzziness("AUTO")
                    .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields))._toQuery();

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("products")
                    .query(multiMatchQuery)
                    .size(5));

            SearchResponse<Map> searchResponse = elasticsearchClient.search(searchRequest, Map.class);

            Set<String> uniqueTerms = new HashSet<>();
            for (Hit<Map> hit : searchResponse.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    String productName = (String) source.get("productName");
                    if (productName != null && !uniqueTerms.contains(productName.toLowerCase())) {
                        uniqueTerms.add(productName.toLowerCase());

                        Map<String, Object> suggestion = new HashMap<>();
                        suggestion.put("id", "suggestion-" + productName.hashCode());
                        suggestion.put("text", productName);
                        suggestion.put("type", "suggestion");
                        suggestion.put("searchTerm", productName);
                        suggestions.add(suggestion);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error getting search term suggestions: {}", e.getMessage());
        }

        return suggestions;
    }

    private List<Map<String, Object>> getCategorySuggestions(String query) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        try {
            List<Category> categories = categoryRepository.findTop5ByNameContainingIgnoreCase(query.toLowerCase());
            for (Category category : categories) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("id", "category-" + category.getId());
                suggestion.put("text", category.getName());
                suggestion.put("type", "category");
                suggestion.put("categoryId", category.getId().toString());
                suggestions.add(suggestion);
            }
        } catch (Exception e) {
            log.error("Error getting category suggestions: {}", e.getMessage());
        }

        return suggestions;
    }

    private List<Map<String, Object>> getBrandSuggestions(String query) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        try {
            List<Brand> brands = brandRepository.findTop5ByBrandNameContainingIgnoreCase(query.toLowerCase());
            for (Brand brand : brands) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("id", "brand-" + brand.getBrandId());
                suggestion.put("text", brand.getBrandName());
                suggestion.put("type", "brand");
                suggestion.put("brandId", brand.getBrandId().toString());
                suggestions.add(suggestion);
            }
        } catch (Exception e) {
            log.error("Error getting brand suggestions: {}", e.getMessage());
        }

        return suggestions;
    }

    private List<Map<String, Object>> getKeywordSuggestions(String query) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        try {
            // Get both metaKeywords and searchKeywords
            List<String> metaKeywordsStrings = productRepository.findDistinctMetaKeywordsByQuery(query.toLowerCase());
            Set<String> uniqueKeywords = new HashSet<>();

            // Process metaKeywords
            for (String metaKeywordsString : metaKeywordsStrings) {
                if (metaKeywordsString != null && !metaKeywordsString.trim().isEmpty()) {
                    String[] keywords = metaKeywordsString.split(",");
                    for (String keyword : keywords) {
                        String trimmedKeyword = keyword.trim();
                        if (!trimmedKeyword.isEmpty() &&
                                trimmedKeyword.toLowerCase().contains(query.toLowerCase()) &&
                                !uniqueKeywords.contains(trimmedKeyword.toLowerCase()) &&
                                uniqueKeywords.size() < 5) {
                            uniqueKeywords.add(trimmedKeyword.toLowerCase());

                            Map<String, Object> suggestion = new HashMap<>();
                            suggestion.put("id", "meta-keyword-" + trimmedKeyword.hashCode());
                            suggestion.put("text", trimmedKeyword);
                            suggestion.put("type", "keyword");
                            suggestion.put("searchTerm", trimmedKeyword);
                            suggestion.put("source", "metaKeywords");
                            suggestions.add(suggestion);
                        }
                    }
                }
            }

            // Also get searchKeywords suggestions
            List<Product> productsWithSearchKeywords = productRepository.findAll().stream()
                    .filter(Product::isActive)
                    .filter(p -> p.getProductDetail() != null &&
                            p.getProductDetail().getSearchKeywords() != null &&
                            !p.getProductDetail().getSearchKeywords().trim().isEmpty())
                    .collect(Collectors.toList());

            for (Product product : productsWithSearchKeywords) {
                String searchKeywordsString = product.getProductDetail().getSearchKeywords();
                if (searchKeywordsString != null && !searchKeywordsString.trim().isEmpty()) {
                    String[] keywords = searchKeywordsString.split(",");
                    for (String keyword : keywords) {
                        String trimmedKeyword = keyword.trim();
                        if (!trimmedKeyword.isEmpty() &&
                                trimmedKeyword.toLowerCase().contains(query.toLowerCase()) &&
                                !uniqueKeywords.contains(trimmedKeyword.toLowerCase()) &&
                                uniqueKeywords.size() < 5) {
                            uniqueKeywords.add(trimmedKeyword.toLowerCase());

                            Map<String, Object> suggestion = new HashMap<>();
                            suggestion.put("id", "search-keyword-" + trimmedKeyword.hashCode());
                            suggestion.put("text", trimmedKeyword);
                            suggestion.put("type", "keyword");
                            suggestion.put("searchTerm", trimmedKeyword);
                            suggestion.put("source", "searchKeywords");
                            suggestions.add(suggestion);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error getting keyword suggestions: {}", e.getMessage());
        }

        return suggestions;
    }

    private void ensureProductsIndexExists() {
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index("products"));
            boolean exists = elasticsearchClient.indices().exists(existsRequest).value();

            if (!exists) {
                CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
                        .index("products")
                        .mappings(m -> m
                                .properties("productId", p -> p.keyword(k -> k))
                                .properties("productName", p -> p.text(t -> t.analyzer("standard")))
                                .properties("shortDescription", p -> p.text(t -> t.analyzer("standard")))
                                .properties("slug", p -> p.keyword(k -> k))
                                .properties("sku", p -> p.keyword(k -> k))
                                .properties("metaDescription", p -> p.text(t -> t.analyzer("standard")))
                                .properties("metaKeywords", p -> p.text(t -> t.analyzer("standard")))
                                .properties("searchKeywords", p -> p.text(t -> t.analyzer("standard")))));
                elasticsearchClient.indices().create(createRequest);
                log.info("Created Elasticsearch products index");
            }
        } catch (Exception e) {
            log.error("Error ensuring products index exists", e);
        }
    }

    private List<Map<String, Object>> getFallbackSearchSuggestions(String query) {
        try {
            List<Map<String, Object>> suggestions = new ArrayList<>();
            String lowerQuery = query.toLowerCase().trim();

            // Get product suggestions based on name and meta keywords
            List<Product> products = productRepository
                    .findTop10ByProductNameContainingIgnoreCaseOrProductDetail_MetaKeywordsContainingIgnoreCase(
                            lowerQuery, lowerQuery);

            for (Product product : products) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("id", "product-" + product.getProductId());
                suggestion.put("text", product.getProductName());
                suggestion.put("type", "product");
                suggestion.put("productId", product.getProductId().toString());
                suggestions.add(suggestion);
            }

            return suggestions;
        } catch (Exception e) {
            log.error("Error in fallback search suggestions", e);
            return new ArrayList<>();
        }
    }

    /**
     * Creates Stock entities for a product based on warehouse assignments
     * 
     * @param product            The product to assign stock to
     * @param warehouseStockList List of warehouse stock assignments
     */
    private void createProductStockAssignments(Product product, List<WarehouseStockDTO> warehouseStockList) {
        if (warehouseStockList == null || warehouseStockList.isEmpty()) {
            log.warn("No warehouse stock assignments provided for product: {}", product.getProductName());
            return;
        }

        for (WarehouseStockDTO warehouseStock : warehouseStockList) {
            try {
                // Validate warehouse exists
                Warehouse warehouse = warehouseRepository.findById(warehouseStock.getWarehouseId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Warehouse not found with ID: " + warehouseStock.getWarehouseId()));

                // Create Stock entity
                Stock stock = new Stock();
                stock.setWarehouse(warehouse);
                stock.setProduct(product);
                stock.setProductVariant(null); // Product-level stock, not variant
                stock.setQuantity(warehouseStock.getStockQuantity() != null ? warehouseStock.getStockQuantity() : 0);
                stock.setLowStockThreshold(
                        warehouseStock.getLowStockThreshold() != null ? warehouseStock.getLowStockThreshold() : 5);

                // Save the stock entry
                stockRepository.save(stock);

                log.info("Created stock entry for product {} in warehouse {}: quantity={}, threshold={}",
                        product.getProductName(), warehouse.getName(),
                        stock.getQuantity(), stock.getLowStockThreshold());

            } catch (Exception e) {
                log.error("Error creating stock assignment for product {} in warehouse {}: {}",
                        product.getProductName(), warehouseStock.getWarehouseId(), e.getMessage(), e);
                throw new RuntimeException("Failed to create stock assignment: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Creates Stock entities for a product variant based on warehouse assignments
     * 
     * @param variant            The product variant to assign stock to
     * @param warehouseStockList List of warehouse stock assignments
     */
    private void createVariantStockAssignments(ProductVariant variant, List<WarehouseStockDTO> warehouseStockList) {
        if (warehouseStockList == null || warehouseStockList.isEmpty()) {
            log.warn("No warehouse stock assignments provided for variant: {}", variant.getId());
            return;
        }

        for (WarehouseStockDTO warehouseStock : warehouseStockList) {
            try {
                // Validate warehouse exists
                Warehouse warehouse = warehouseRepository.findById(warehouseStock.getWarehouseId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Warehouse not found with ID: " + warehouseStock.getWarehouseId()));

                // Create Stock entity
                Stock stock = new Stock();
                stock.setWarehouse(warehouse);
                stock.setProduct(null); // Variant-level stock, not product
                stock.setProductVariant(variant);
                stock.setQuantity(warehouseStock.getStockQuantity() != null ? warehouseStock.getStockQuantity() : 0);
                stock.setLowStockThreshold(
                        warehouseStock.getLowStockThreshold() != null ? warehouseStock.getLowStockThreshold() : 5);

                // Save the stock entry
                stockRepository.save(stock);

                log.info("Created stock entry for variant {} in warehouse {}: quantity={}, threshold={}",
                        variant.getId(), warehouse.getName(),
                        stock.getQuantity(), stock.getLowStockThreshold());

            } catch (Exception e) {
                log.error("Error creating stock assignment for variant {} in warehouse {}: {}",
                        variant.getId(), warehouseStock.getWarehouseId(), e.getMessage(), e);
                throw new RuntimeException("Failed to create variant stock assignment: " + e.getMessage(), e);
            }
        }
    }

    @PostConstruct
    public void indexExistingProducts() {
        try {
            log.info("Starting to index existing products in Elasticsearch...");
            List<Product> products = productRepository.findAll();
            int indexedCount = 0;

            for (Product product : products) {
                try {
                    indexProductInElasticsearch(product);
                    indexedCount++;
                } catch (Exception e) {
                    log.warn("Failed to index product {}: {}", product.getProductId(), e.getMessage());
                }
            }

            log.info("Successfully indexed {} products in Elasticsearch", indexedCount);
        } catch (Exception e) {
            log.error("Error indexing existing products: {}", e.getMessage(), e);
        }
    }

    private void indexProductInElasticsearch(Product product) {
        try {
            // Ensure products index exists
            ensureProductsIndexExists();

            // Create product document for Elasticsearch
            Map<String, Object> productDoc = new HashMap<>();
            productDoc.put("productId", product.getProductId().toString());
            productDoc.put("productName", product.getProductName());
            productDoc.put("shortDescription", product.getShortDescription());
            productDoc.put("slug", product.getSlug());
            productDoc.put("sku", product.getSku());
            productDoc.put("barcode", product.getBarcode());

            // Add ProductDetails fields if available
            if (product.getProductDetail() != null) {
                productDoc.put("metaDescription", product.getProductDetail().getMetaDescription());
                productDoc.put("metaKeywords", product.getProductDetail().getMetaKeywords());
                productDoc.put("searchKeywords", product.getProductDetail().getSearchKeywords());
                productDoc.put("description", product.getProductDetail().getDescription());

                // Add comma-separated keywords as arrays for better search
                if (product.getProductDetail().getMetaKeywords() != null &&
                        !product.getProductDetail().getMetaKeywords().trim().isEmpty()) {
                    String[] metaKeywordsArray = product.getProductDetail().getMetaKeywords()
                            .split(",");
                    List<String> trimmedMetaKeywords = Arrays.stream(metaKeywordsArray)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                    productDoc.put("metaKeywordsArray", trimmedMetaKeywords);
                }

                if (product.getProductDetail().getSearchKeywords() != null &&
                        !product.getProductDetail().getSearchKeywords().trim().isEmpty()) {
                    String[] searchKeywordsArray = product.getProductDetail().getSearchKeywords()
                            .split(",");
                    List<String> trimmedSearchKeywords = Arrays.stream(searchKeywordsArray)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                    productDoc.put("searchKeywordsArray", trimmedSearchKeywords);
                }
            }

            // Index the document
            elasticsearchClient.index(i -> i
                    .index("products")
                    .id(product.getProductId().toString())
                    .document(productDoc));

            log.debug("Successfully indexed product {} in Elasticsearch", product.getProductId());

        } catch (Exception e) {
            log.error("Error indexing product {} in Elasticsearch: {}", product.getProductId(), e.getMessage(), e);
            // Don't throw exception to avoid breaking the main flow
        }
    }
}