package com.ecommerce.service.impl;

import com.ecommerce.dto.CreateProductDTO;
import com.ecommerce.dto.CreateProductVariantDTO;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.dto.ProductVariantDTO;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.CloudinaryService;
import com.ecommerce.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::mapProductToDTO);
    }

    @Override
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable)
                .map(this::mapProductToDTO);
    }

    @Override
    public ProductDTO updateProduct(UUID productId, CreateProductDTO updateProductDTO) {
        throw new UnsupportedOperationException("Method not yet implemented");
    }

    @Override
    public boolean deleteProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            return false;
        }
        productRepository.deleteById(productId);
        return true;
    }

    @Override
    public Page<ProductDTO> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchProducts(keyword, pageable)
                .map(this::mapProductToDTO);
    }

    @Override
    public List<ProductDTO> getFeaturedProducts(int limit) {
        return productRepository.findFeaturedProducts(Pageable.ofSize(limit))
                .stream()
                .map(this::mapProductToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getBestsellerProducts(int limit) {
        return productRepository.findBestsellerProducts(Pageable.ofSize(limit))
                .stream()
                .map(this::mapProductToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getNewArrivalProducts(int limit) {
        return productRepository.findNewArrivalProducts(Pageable.ofSize(limit))
                .stream()
                .map(this::mapProductToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getOnSaleProducts(int limit) {
        return productRepository.findOnSaleProducts(Pageable.ofSize(limit))
                .stream()
                .map(this::mapProductToDTO)
                .collect(Collectors.toList());
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
}