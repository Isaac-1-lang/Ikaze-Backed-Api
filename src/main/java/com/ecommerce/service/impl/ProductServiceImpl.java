package com.ecommerce.service.impl;

import com.ecommerce.dto.CreateProductVariantDTO;

import com.ecommerce.dto.ImageMetadata;

import com.ecommerce.dto.ManyProductsDto;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.dto.CustomerProductDTO;
import com.ecommerce.dto.CustomerProductVariantDTO;

import com.ecommerce.dto.ProductSearchDTO;

import com.ecommerce.dto.ProductUpdateDTO;
import com.ecommerce.dto.ProductBasicInfoDTO;
import com.ecommerce.dto.ProductBasicInfoUpdateDTO;
import com.ecommerce.dto.ProductPricingDTO;
import com.ecommerce.dto.ProductPricingUpdateDTO;
import com.ecommerce.dto.ProductMediaDTO;
import com.ecommerce.dto.ProductVideoDTO;
import com.ecommerce.dto.SimilarProductsRequestDTO;
import com.ecommerce.dto.ProductVariantDTO;
import com.ecommerce.dto.ProductVariantImageDTO;
import com.ecommerce.dto.ProductVariantAttributeDTO;
import com.ecommerce.dto.VariantAttributeRequest;
import com.ecommerce.dto.CreateVariantRequest;
import com.ecommerce.dto.WarehouseStockRequest;
import com.ecommerce.dto.WarehouseStockWithBatchesRequest;
import com.ecommerce.dto.StockBatchDTO;
import com.ecommerce.dto.ProductDetailsDTO;
import com.ecommerce.dto.ProductDetailsUpdateDTO;

import com.ecommerce.dto.VariantImageMetadata;

import com.ecommerce.dto.ReviewDTO;
import com.ecommerce.dto.DiscountDTO;
import com.ecommerce.dto.WarehouseStockDTO;
import com.ecommerce.dto.WarehouseStockPageResponse;

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

import com.ecommerce.service.CloudinaryService;

import com.ecommerce.service.ProductService;

import com.ecommerce.service.ProductAvailabilityService;

import jakarta.persistence.EntityNotFoundException;

import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageImpl;

import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDateTime;

import java.util.*;

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

    private final ProductAttributeTypeRepository productAttributeTypeRepository;

    private final ProductAttributeValueRepository productAttributeValueRepository;

    private final VariantAttributeValueRepository variantAttributeValueRepository;

    private final OrderRepository orderRepository;

    private final CartRepository cartRepository;

    private final WarehouseRepository warehouseRepository;

    private final StockRepository stockRepository;

    private final StockBatchRepository stockBatchRepository;

    private final ProductDetailRepository productDetailRepository;

    private final CloudinaryService cloudinaryService;

    private final ElasticsearchClient elasticsearchClient;

    private final ProductAvailabilityService productAvailabilityService;

    @Override
    @Transactional
    public Map<String, Object> createEmptyProduct(String name) {
        try {
            log.info("Creating empty product with name: {}", name);

            Product product = new Product();
            product.setProductName(name);
            product.setSku(generateTemporarySku(name));
            product.setPrice(new java.math.BigDecimal("0.01"));
            product.setStatus(com.ecommerce.enums.ProductStatus.DRAFT);
            product.setCompletionPercentage(0);
            product.setDisplayToCustomers(false);
            product.setActive(false);

            Product savedProduct = productRepository.save(product);

            Map<String, Object> response = new HashMap<>();
            response.put("productId", savedProduct.getProductId());
            response.put("status", savedProduct.getStatus());
            response.put("completionPercentage", savedProduct.getCompletionPercentage());
            response.put("displayToCustomers", savedProduct.getDisplayToCustomers());

            log.info("Empty product created successfully with ID: {}", savedProduct.getProductId());
            return response;

        } catch (Exception e) {
            log.error("Error creating empty product: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create empty product: " + e.getMessage(), e);
        }
    }

    private String generateTemporarySku(String name) {
        String baseSku = name.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, Math.min(name.length(), 8));
        return "TEMP-" + baseSku + "-" + System.currentTimeMillis();
    }

    @Override
    public boolean productHasVariants(UUID productId) {
        try {
            log.info("Checking if product {} has variants", productId);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            boolean hasVariants = product.getVariants() != null && !product.getVariants().isEmpty();
            log.info("Product {} has variants: {}", productId, hasVariants);
            return hasVariants;

        } catch (Exception e) {
            log.error("Error checking product variants: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check product variants: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean productHasStock(UUID productId) {
        try {
            log.info("Checking if product {} has stock", productId);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            List<Stock> stocks = stockRepository.findByProduct(product);
            boolean hasStock = !stocks.isEmpty();
            log.info("Product {} has stock: {}", productId, hasStock);
            return hasStock;

        } catch (Exception e) {
            log.error("Error checking product stock: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check product stock: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void removeProductStock(UUID productId) {
        try {
            log.info("Removing all stock for product {}", productId);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            List<Stock> stocks = stockRepository.findByProduct(product);
            for (Stock stock : stocks) {
                // Safely handle existing batches (preserve those referenced by orders)
                safelyHandleExistingStockBatches(stock);
                // Delete the stock entry (batches are handled above)
                stockRepository.delete(stock);
            }
            log.info("Successfully removed all stock for product {}", productId);

        } catch (Exception e) {
            log.error("Error removing product stock: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to remove product stock: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> assignProductStockWithBatches(UUID productId,
            List<WarehouseStockWithBatchesRequest> warehouseStocks) {
        try {
            log.info("Assigning stock with batches to product {} for {} warehouses", 
                    productId, warehouseStocks.size());

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                throw new IllegalArgumentException("Product has variants. Use assignVariantStockWithBatches instead.");
            }

            List<Stock> existingStocks = stockRepository.findByProduct(product);
            for (Stock existingStock : existingStocks) {
                safelyHandleExistingStockBatches(existingStock);
            }

            List<Stock> processedStocks = new ArrayList<>();
            int totalBatchesCreated = 0;

            for (WarehouseStockWithBatchesRequest stockRequest : warehouseStocks) {
                Warehouse warehouse = warehouseRepository.findById(stockRequest.getWarehouseId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Warehouse not found: " + stockRequest.getWarehouseId()));

                Stock stock = existingStocks.stream()
                        .filter(s -> s.getWarehouse().getId().equals(warehouse.getId()))
                        .findFirst()
                        .orElse(null);

                if (stock == null) {
                    stock = new Stock();
                    stock.setProduct(product);
                    stock.setWarehouse(warehouse);
                }
                
                // Update stock threshold
                stock.setLowStockThreshold(stockRequest.getLowStockThreshold());
                Stock savedStock = stockRepository.save(stock);

                // Create batches for this stock
                List<StockBatch> batches = new ArrayList<>();
                for (WarehouseStockWithBatchesRequest.StockBatchRequest batchRequest : stockRequest.getBatches()) {
                    // Check if batch number already exists for this stock
                    if (stockBatchRepository.findByStockAndBatchNumber(savedStock, batchRequest.getBatchNumber())
                            .isPresent()) {
                        throw new IllegalArgumentException(
                                "Batch number '" + batchRequest.getBatchNumber() + "' already exists for this product stock");
                    }

                    StockBatch batch = new StockBatch();
                    batch.setStock(savedStock);
                    batch.setBatchNumber(batchRequest.getBatchNumber());
                    batch.setManufactureDate(batchRequest.getManufactureDate());
                    batch.setExpiryDate(batchRequest.getExpiryDate());
                    batch.setQuantity(batchRequest.getQuantity());
                    batch.setSupplierName(batchRequest.getSupplierName());
                    batch.setSupplierBatchNumber(batchRequest.getSupplierBatchNumber());
                    batch.setStatus(com.ecommerce.enums.BatchStatus.ACTIVE);

                    StockBatch savedBatch = stockBatchRepository.save(batch);
                    batches.add(savedBatch);
                    totalBatchesCreated++;
                }

                // Update stock quantity based on active batches
                Integer totalQuantity = batches.stream()
                        .filter(batch -> batch.getStatus() == com.ecommerce.enums.BatchStatus.ACTIVE)
                        .mapToInt(StockBatch::getQuantity)
                        .sum();
                stockRepository.save(savedStock);

                processedStocks.add(savedStock);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product stock with batches assigned successfully");
            response.put("assignedWarehouses", warehouseStocks.size());
            response.put("totalBatchesCreated", totalBatchesCreated);

            log.info("Successfully assigned stock with {} batches to product {} for {} warehouses",
                    totalBatchesCreated, productId, warehouseStocks.size());
            return response;

        } catch (IllegalArgumentException e) {
            log.error("Validation error assigning product stock with batches: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error assigning product stock with batches: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign product stock with batches: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> assignProductStock(UUID productId, List<WarehouseStockRequest> warehouseStocks) {
        try {
            log.info("Assigning stock to product {} for {} warehouses", productId, warehouseStocks.size());

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                throw new IllegalArgumentException(
                        "Cannot assign stock to product with variants. Stock should be managed at variant level.");
            }

            List<Stock> existingStocks = stockRepository.findByProduct(product);
            for (Stock existingStock : existingStocks) {
                // Safely handle existing batches (preserve those referenced by orders)
                safelyHandleExistingStockBatches(existingStock);
                // Delete the stock entry (batches are handled above)
                stockRepository.delete(existingStock);
            }

            List<Stock> newStocks = new ArrayList<>();
            for (WarehouseStockRequest stockRequest : warehouseStocks) {
                Warehouse warehouse = warehouseRepository.findById(stockRequest.getWarehouseId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Warehouse not found: " + stockRequest.getWarehouseId()));

                Stock stock = new Stock();
                stock.setProduct(product);
                stock.setWarehouse(warehouse);
                stock.setLowStockThreshold(stockRequest.getLowStockThreshold());

                Stock savedStock = stockRepository.save(stock);

                StockBatch defaultBatch = new StockBatch();
                defaultBatch.setStock(savedStock);
                defaultBatch.setBatchNumber("DEFAULT-" + System.currentTimeMillis());
                defaultBatch.setQuantity(stockRequest.getStockQuantity());
                defaultBatch.setStatus(com.ecommerce.enums.BatchStatus.ACTIVE);

                stockBatchRepository.save(defaultBatch);
                stockRepository.save(savedStock);

                newStocks.add(savedStock);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock assigned successfully");
            response.put("assignedWarehouses", warehouseStocks.size());

            log.info("Successfully assigned stock to product {} for {} warehouses", productId, warehouseStocks.size());
            return response;

        } catch (IllegalArgumentException e) {
            log.error("Validation error assigning product stock: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error assigning product stock: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign product stock: " + e.getMessage(), e);
        }
    }

    /**
     * Safely handle existing stock batches - preserve those referenced by orders, 
     * deactivate others, and only delete unreferenced ones
     */
    private void safelyHandleExistingStockBatches(Stock existingStock) {
        List<StockBatch> existingBatches = stockBatchRepository.findByStock(existingStock);
        
        int preservedBatches = 0;
        int deactivatedBatches = 0;
        int deletedBatches = 0;
        
        for (StockBatch batch : existingBatches) {
            if (stockBatchRepository.isReferencedByOrders(batch.getId())) {
                if (batch.getStatus() == com.ecommerce.enums.BatchStatus.ACTIVE) {
                    batch.setStatus(com.ecommerce.enums.BatchStatus.INACTIVE);
                    stockBatchRepository.save(batch);
                    deactivatedBatches++;
                } else {
                    preservedBatches++;
                }
            } else {
                stockBatchRepository.delete(batch);
                deletedBatches++;
            }
        }
    }

    @Override
    @Transactional
    public Map<String, Object> assignVariantStockWithBatches(UUID productId, Long variantId,
            List<WarehouseStockWithBatchesRequest> warehouseStocks) {
        try {
            log.info("Assigning stock with batches to variant {} of product {} for {} warehouses", 
                    variantId, productId, warehouseStocks.size());

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    
            ProductVariant variant = product.getVariants().stream()
                    .filter(v -> v.getId().equals(variantId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Variant not found"));
            List<Stock> existingStocks = stockRepository.findByProductVariant(variant);
            for (Stock existingStock : existingStocks) {
                safelyHandleExistingStockBatches(existingStock);
            }

            List<Stock> processedStocks = new ArrayList<>();
            int totalBatchesCreated = 0;

            for (WarehouseStockWithBatchesRequest stockRequest : warehouseStocks) {
                Warehouse warehouse = warehouseRepository.findById(stockRequest.getWarehouseId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Warehouse not found: " + stockRequest.getWarehouseId()));

                Stock stock = existingStocks.stream()
                        .filter(s -> s.getWarehouse().getId().equals(warehouse.getId()))
                        .findFirst()
                        .orElse(null);

                if (stock == null) {
                    stock = new Stock();
                    stock.setProductVariant(variant);
                    stock.setWarehouse(warehouse);
                }
                
                // Update stock threshold
                stock.setLowStockThreshold(stockRequest.getLowStockThreshold());
                Stock savedStock = stockRepository.save(stock);
    
                List<StockBatch> batches = new ArrayList<>();
                for (WarehouseStockWithBatchesRequest.StockBatchRequest batchRequest : stockRequest.getBatches()) {
                    if (stockBatchRepository.findByStockAndBatchNumber(savedStock, batchRequest.getBatchNumber())
                            .isPresent()) {
                        throw new IllegalArgumentException(
                                "Batch number '" + batchRequest.getBatchNumber() + "' already exists for this variant stock");
                    }
    
                    StockBatch batch = new StockBatch();
                    batch.setStock(savedStock);
                    batch.setBatchNumber(batchRequest.getBatchNumber());
                    batch.setManufactureDate(batchRequest.getManufactureDate());
                    batch.setExpiryDate(batchRequest.getExpiryDate());
                    batch.setQuantity(batchRequest.getQuantity());
                    batch.setSupplierName(batchRequest.getSupplierName());
                    batch.setSupplierBatchNumber(batchRequest.getSupplierBatchNumber());
                    batch.setStatus(com.ecommerce.enums.BatchStatus.ACTIVE);
    
                    StockBatch savedBatch = stockBatchRepository.save(batch);
                    batches.add(savedBatch);
                    totalBatchesCreated++;
                }
    
                // Update stock quantity based on active batches
                Integer totalQuantity = batches.stream()
                        .filter(batch -> batch.getStatus() == com.ecommerce.enums.BatchStatus.ACTIVE)
                        .mapToInt(StockBatch::getQuantity)
                        .sum();
                stockRepository.save(savedStock);
    
                processedStocks.add(savedStock);
            }
    
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Variant stock with batches assigned successfully");
            response.put("assignedWarehouses", warehouseStocks.size());
            response.put("totalBatchesCreated", totalBatchesCreated);
    
            log.info("Successfully assigned stock with {} batches to variant {} of product {} for {} warehouses",
                    totalBatchesCreated, variantId, productId, warehouseStocks.size());
            return response;
    
        } catch (IllegalArgumentException e) {
            log.error("Validation error assigning variant stock with batches: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error assigning variant stock with batches: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign variant stock with batches: " + e.getMessage(), e);
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

    @Override

    public ProductDTO getProductById(UUID productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        return mapProductToDTO(product);

    }

    @Override
    public ProductBasicInfoDTO getProductBasicInfo(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        return mapProductToBasicInfoDTO(product);
    }

    @Override
    public CustomerProductDTO getCustomerProductById(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        return mapProductToCustomerDTO(product);
    }

    @Override
    public CustomerProductDTO getCustomerProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with slug: " + slug));

        return mapProductToCustomerDTO(product);
    }

    @Override
    public ProductBasicInfoDTO updateProductBasicInfo(UUID productId, ProductBasicInfoUpdateDTO updateDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        // Validate that at least one field is provided for update
        boolean hasUpdates = updateDTO.getProductName() != null ||
                updateDTO.getShortDescription() != null ||
                updateDTO.getDescription() != null ||
                updateDTO.getSku() != null ||
                updateDTO.getBarcode() != null ||
                updateDTO.getModel() != null ||
                updateDTO.getSlug() != null ||
                updateDTO.getMaterial() != null ||
                updateDTO.getWarrantyInfo() != null ||
                updateDTO.getCareInstructions() != null ||
                updateDTO.getPrice() != null ||
                updateDTO.getCompareAtPrice() != null ||
                updateDTO.getCostPrice() != null ||
                updateDTO.getCategoryId() != null ||
                updateDTO.getBrandId() != null ||
                updateDTO.getActive() != null ||
                updateDTO.getFeatured() != null ||
                updateDTO.getBestseller() != null ||
                updateDTO.getNewArrival() != null ||
                updateDTO.getOnSale() != null ||
                updateDTO.getSalePercentage() != null;

        if (!hasUpdates) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        // Check if SKU is being changed and if it's unique
        if (updateDTO.getSku() != null && !product.getSku().equals(updateDTO.getSku())) {
            Optional<Product> existingProductWithSku = productRepository.findBySku(updateDTO.getSku());
            if (existingProductWithSku.isPresent()) {
                throw new IllegalArgumentException("SKU '" + updateDTO.getSku() + "' already exists");
            }
        }

        // Update basic product fields only if provided
        if (updateDTO.getProductName() != null) {
            product.setProductName(updateDTO.getProductName());
        }
        if (updateDTO.getShortDescription() != null) {
            product.setShortDescription(updateDTO.getShortDescription());
        }
        if (updateDTO.getSku() != null) {
            product.setSku(updateDTO.getSku());
        }
        if (updateDTO.getBarcode() != null) {
            product.setBarcode(updateDTO.getBarcode());
        }
        if (updateDTO.getModel() != null) {
            product.setModel(updateDTO.getModel());
        }
        if (updateDTO.getSlug() != null) {
            product.setSlug(updateDTO.getSlug());
        }
        if (updateDTO.getPrice() != null) {
            product.setPrice(updateDTO.getPrice());
        }
        if (updateDTO.getCompareAtPrice() != null) {
            product.setCompareAtPrice(updateDTO.getCompareAtPrice());
        }
        if (updateDTO.getCostPrice() != null) {
            product.setCostPrice(updateDTO.getCostPrice());
        }
        if (updateDTO.getActive() != null) {
            product.setActive(updateDTO.getActive());
        }
        if (updateDTO.getFeatured() != null) {
            product.setFeatured(updateDTO.getFeatured());
        }
        if (updateDTO.getBestseller() != null) {
            product.setBestseller(updateDTO.getBestseller());
        }
        if (updateDTO.getNewArrival() != null) {
            product.setNewArrival(updateDTO.getNewArrival());
        }
        if (updateDTO.getOnSale() != null) {
            product.setOnSale(updateDTO.getOnSale());
        }
        if (updateDTO.getSalePercentage() != null) {
            product.setSalePercentage(updateDTO.getSalePercentage());
        }

        // Update category if provided
        if (updateDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(updateDTO.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Category not found with ID: " + updateDTO.getCategoryId()));
            product.setCategory(category);
        }

        // Update brand if provided
        if (updateDTO.getBrandId() != null) {
            Brand brand = brandRepository.findById(updateDTO.getBrandId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Brand not found with ID: " + updateDTO.getBrandId()));
            product.setBrand(brand);
        }

        // Update product detail fields only if provided
        ProductDetail productDetail = product.getProductDetail();
        if (updateDTO.getDescription() != null || updateDTO.getMaterial() != null ||
                updateDTO.getWarrantyInfo() != null || updateDTO.getCareInstructions() != null) {

            if (productDetail == null) {
                productDetail = new ProductDetail();
                productDetail.setProduct(product);
                product.setProductDetail(productDetail);
            }

            if (updateDTO.getDescription() != null) {
                productDetail.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getMaterial() != null) {
                productDetail.setMaterial(updateDTO.getMaterial());
            }
            if (updateDTO.getWarrantyInfo() != null) {
                productDetail.setWarrantyInfo(updateDTO.getWarrantyInfo());
            }
            if (updateDTO.getCareInstructions() != null) {
                productDetail.setCareInstructions(updateDTO.getCareInstructions());
            }
        }

        // Save the updated product
        Product savedProduct = productRepository.save(product);

        // Return the updated basic info
        return mapProductToBasicInfoDTO(savedProduct);
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
            int page = searchDTO.getPage() != null ? searchDTO.getPage() : 0;
            int size = searchDTO.getSize() != null ? searchDTO.getSize() : 10;
            String sortBy = searchDTO.getSortBy() != null ? searchDTO.getSortBy() : "createdAt";
            String sortDirection = searchDTO.getSortDirection() != null ? searchDTO.getSortDirection() : "desc";

            boolean isTextSearch = (searchDTO.getName() != null && !searchDTO.getName().trim().isEmpty()) ||
                    (searchDTO.getSearchKeyword() != null && !searchDTO.getSearchKeyword().trim().isEmpty());

            if (isTextSearch) {
                return searchProductsWithElasticsearch(searchDTO, page, size, sortBy, sortDirection);
            } else {
                Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
                Specification<Product> spec = buildProductSearchSpecification(searchDTO);
                Page<Product> productPage = productRepository.findAll(spec, pageable);

                List<Product> filteredProducts = productPage.getContent();
                if (searchDTO.getAverageRatingMin() != null || searchDTO.getAverageRatingMax() != null) {
                    filteredProducts = applyRatingFilter(filteredProducts, searchDTO.getAverageRatingMin(),
                            searchDTO.getAverageRatingMax());
                }

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
            if (!ensureProductsIndexExists()) {
                log.warn("Elasticsearch not available, falling back to database search");
                return searchProductsWithJPA(searchDTO, page, size, sortBy, sortDirection);
            }

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

            if (searchDTO.getCategoryNames() != null && !searchDTO.getCategoryNames().isEmpty()) {

                predicates.add(root.get("category").get("name").in(searchDTO.getCategoryNames()));

            }

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
                // Filter products that have the discount directly OR have variants with the
                // discount
                Predicate productDiscountPredicate = root.get("discount").get("discountId")
                        .in(searchDTO.getDiscountIds());

                // Create subquery for variants with the discount
                Subquery<ProductVariant> variantSubquery = query.subquery(ProductVariant.class);
                Root<ProductVariant> variantRoot = variantSubquery.from(ProductVariant.class);
                variantSubquery.select(variantRoot)
                        .where(criteriaBuilder.and(
                                criteriaBuilder.equal(variantRoot.get("product"), root),
                                variantRoot.get("discount").get("discountId").in(searchDTO.getDiscountIds())));

                Predicate variantDiscountPredicate = criteriaBuilder.exists(variantSubquery);

                // Combine both conditions with OR
                predicates.add(criteriaBuilder.or(productDiscountPredicate, variantDiscountPredicate));
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

                predicates.add(criteriaBuilder.equal(root.get("isFeatured"), searchDTO.getIsFeatured()));

            }

            if (searchDTO.getIsBestseller() != null) {

                predicates.add(criteriaBuilder.equal(root.get("isBestseller"), searchDTO.getIsBestseller()));

            }

            if (searchDTO.getIsNewArrival() != null) {

                // New arrivals are products created within the last 30 days

                if (searchDTO.getIsNewArrival()) {

                    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), thirtyDaysAgo));

                }

            }

            /*
             * if (searchDTO.getAverageRatingMin() != null ||
             * searchDTO.getAverageRatingMax() != null) {
             * Subquery<Double> avgRatingSubquery = criteriaQuery.subquery(Double.class);
             * Root<Review> reviewRoot = avgRatingSubquery.from(Review.class);
             * 
             * avgRatingSubquery.select(criteriaBuilder.avg(reviewRoot.get("rating")))
             * .where(criteriaBuilder.equal(reviewRoot.get("product"), root));
             * 
             * if (searchDTO.getAverageRatingMin() != null) {
             * predicates.add(
             * criteriaBuilder.greaterThanOrEqualTo(avgRatingSubquery,
             * searchDTO.getAverageRatingMin()));
             * }
             * 
             * if (searchDTO.getAverageRatingMax() != null) {
             * predicates
             * .add(criteriaBuilder.lessThanOrEqualTo(avgRatingSubquery,
             * searchDTO.getAverageRatingMax()));
             * }
             * }
             */

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

            // Variant attributes filter
            if (searchDTO.getVariantAttributes() != null && !searchDTO.getVariantAttributes().isEmpty()) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.LEFT);
                Join<ProductVariant, VariantAttributeValue> attributeJoin = variantJoin.join("attributeValues",
                        JoinType.LEFT);
                Join<VariantAttributeValue, ProductAttributeValue> valueJoin = attributeJoin.join("attributeValue",
                        JoinType.LEFT);
                Join<ProductAttributeValue, ProductAttributeType> typeJoin = valueJoin.join("attributeType",
                        JoinType.LEFT);

                List<Predicate> variantPredicates = new ArrayList<>();
                for (String variantAttr : searchDTO.getVariantAttributes()) {
                    String[] parts = variantAttr.split(":");
                    if (parts.length == 2) {
                        String typeName = parts[0];
                        String valueName = parts[1];

                        Predicate typePredicate = criteriaBuilder.equal(
                                criteriaBuilder.lower(typeJoin.get("name")),
                                typeName.toLowerCase());
                        Predicate valuePredicate = criteriaBuilder.equal(
                                criteriaBuilder.lower(valueJoin.get("value")),
                                valueName.toLowerCase());

                        variantPredicates.add(criteriaBuilder.and(typePredicate, valuePredicate));
                    }
                }

                if (!variantPredicates.isEmpty()) {
                    predicates.add(criteriaBuilder.or(variantPredicates.toArray(new Predicate[0])));
                }
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
                    .map(this::mapReviewToDTO)
                    .collect(Collectors.toList()));
        }

        // Map warehouse stock information
        mapWarehouseStockToDTO(product, dto);

        return dto;
    }

    private CustomerProductDTO mapProductToCustomerDTO(Product product) {
        CustomerProductDTO dto = new CustomerProductDTO();

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

        // Map product details if available (include customer-relevant fields)
        if (product.getProductDetail() != null) {
            dto.setFullDescription(product.getProductDetail().getDescription());
            dto.setDimensionsCm(product.getProductDetail().getDimensionsCm());
            dto.setWeightKg(product.getProductDetail().getWeightKg());
            dto.setMaterial(product.getProductDetail().getMaterial());
            dto.setCareInstructions(product.getProductDetail().getCareInstructions());
            dto.setWarrantyInfo(product.getProductDetail().getWarrantyInfo());
            dto.setShippingInfo(product.getProductDetail().getShippingInfo());
            dto.setReturnPolicy(product.getProductDetail().getReturnPolicy());
        }

        // Map images
        if (product.getImages() != null) {
            dto.setImages(product.getImages().stream()
                    .map(this::mapProductImageToCustomerDTO)
                    .collect(Collectors.toList()));
        }

        // Map videos
        if (product.getVideos() != null) {
            dto.setVideos(product.getVideos().stream()
                    .map(this::mapProductVideoToCustomerDTO)
                    .collect(Collectors.toList()));
        }

        // Map variants (without warehouse stock information)
        if (product.getVariants() != null) {
            dto.setVariants(product.getVariants().stream()
                    .map(this::mapProductVariantToCustomerDTO)
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

    private ProductBasicInfoDTO mapProductToBasicInfoDTO(Product product) {
        ProductBasicInfoDTO dto = new ProductBasicInfoDTO();

        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setShortDescription(product.getShortDescription());
        dto.setSku(product.getSku());
        dto.setBarcode(product.getBarcode());
        dto.setModel(product.getModel());
        dto.setSlug(product.getSlug());
        dto.setPrice(product.getPrice());
        dto.setCompareAtPrice(product.getCompareAtPrice());
        dto.setCostPrice(product.getCostPrice());
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        dto.setBrandId(product.getBrand() != null ? product.getBrand().getBrandId() : null);
        dto.setBrandName(product.getBrand() != null ? product.getBrand().getBrandName() : null);
        dto.setBrandLogoUrl(product.getBrand() != null ? product.getBrand().getLogoUrl() : null);
        dto.setActive(product.isActive());
        dto.setFeatured(product.isFeatured());
        dto.setBestseller(product.isBestseller());
        dto.setNewArrival(product.isNewArrival());
        dto.setOnSale(product.isOnSale());
        dto.setSalePercentage(product.getSalePercentage());

        if (product.getProductDetail() != null) {
            dto.setDescription(product.getProductDetail().getDescription());
            dto.setMaterial(product.getProductDetail().getMaterial());
            dto.setWarrantyInfo(product.getProductDetail().getWarrantyInfo());
            dto.setCareInstructions(product.getProductDetail().getCareInstructions());
        }

        return dto;
    }

    private CustomerProductDTO.ProductImageDTO mapProductImageToCustomerDTO(ProductImage image) {
        return CustomerProductDTO.ProductImageDTO.builder()
                .imageId(image.getId())
                .url(image.getImageUrl())
                .altText(image.getAltText())
                .isPrimary(image.isPrimary())
                .sortOrder(image.getSortOrder())
                .build();
    }

    private CustomerProductDTO.ProductVideoDTO mapProductVideoToCustomerDTO(ProductVideo video) {
        return CustomerProductDTO.ProductVideoDTO.builder()
                .videoId(video.getVideoId())
                .url(video.getUrl())
                .title(video.getTitle())
                .description(video.getDescription())
                .sortOrder(video.getSortOrder())
                .build();
    }

    private CustomerProductVariantDTO mapProductVariantToCustomerDTO(ProductVariant variant) {
        CustomerProductVariantDTO dto = new CustomerProductVariantDTO();

        dto.setVariantId(variant.getId());
        dto.setVariantSku(variant.getVariantSku());
        dto.setVariantName(variant.getVariantName());
        dto.setVariantBarcode(variant.getVariantBarcode());
        dto.setPrice(variant.getPrice());
        dto.setSalePrice(variant.getCompareAtPrice());
        dto.setCostPrice(variant.getCostPrice());
        dto.setIsActive(variant.isActive());
        dto.setIsInStock(variant.isInStock());
        dto.setIsLowStock(variant.isLowStock());
        dto.setCreatedAt(variant.getCreatedAt());
        dto.setUpdatedAt(variant.getUpdatedAt());

        // Map discount information
        if (variant.getDiscount() != null) {
            DiscountDTO discountDTO = mapDiscountToDTO(variant.getDiscount());
            dto.setDiscount(discountDTO);

            // Check if discount is currently active
            boolean isActive = isDiscountCurrentlyActive(variant.getDiscount());
            dto.setHasActiveDiscount(isActive);

            // Calculate discounted price if discount is active
            if (isActive && variant.getDiscount().getPercentage() != null) {
                BigDecimal discountPercentage = variant.getDiscount().getPercentage();
                BigDecimal originalPrice = variant.getPrice();
                BigDecimal discountedPrice = originalPrice.multiply(
                        BigDecimal.ONE.subtract(discountPercentage.divide(BigDecimal.valueOf(100))));
                dto.setDiscountedPrice(discountedPrice);
            }
        } else {
            dto.setHasActiveDiscount(false);
        }

        // Map variant images
        if (variant.getImages() != null) {
            dto.setImages(variant.getImages().stream()
                    .map(this::mapVariantImageToCustomerDTO)
                    .collect(Collectors.toList()));
        }

        // Map variant attributes
        if (variant.getAttributeValues() != null) {
            dto.setAttributes(variant.getAttributeValues().stream()
                    .map(this::mapVariantAttributeToCustomerDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private CustomerProductVariantDTO.VariantImageDTO mapVariantImageToCustomerDTO(ProductVariantImage image) {
        return CustomerProductVariantDTO.VariantImageDTO.builder()
                .imageId(image.getId())
                .url(image.getImageUrl())
                .altText(image.getAltText())
                .isPrimary(image.isPrimary())
                .sortOrder(image.getSortOrder())
                .build();
    }

    private CustomerProductVariantDTO.VariantAttributeDTO mapVariantAttributeToCustomerDTO(VariantAttributeValue attributeValue) {
        return CustomerProductVariantDTO.VariantAttributeDTO.builder()
                .attributeValueId(attributeValue.getId().getAttributeValueId())
                .attributeValue(attributeValue.getAttributeValue().getValue())
                .attributeTypeId(attributeValue.getAttributeValue().getAttributeType().getAttributeTypeId())
                .attributeType(attributeValue.getAttributeValue().getAttributeType().getName())
                .build();
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

        dto.setIsActive(variant.isActive());

        dto.setIsInStock(variant.isInStock());

        dto.setIsLowStock(variant.isLowStock());

        dto.setCreatedAt(variant.getCreatedAt());

        dto.setUpdatedAt(variant.getUpdatedAt());

        // Map discount information
        if (variant.getDiscount() != null) {
            DiscountDTO discountDTO = mapDiscountToDTO(variant.getDiscount());
            dto.setDiscount(discountDTO);

            // Check if discount is currently active
            boolean isActive = isDiscountCurrentlyActive(variant.getDiscount());
            dto.setHasActiveDiscount(isActive);

            // Calculate discounted price if discount is active
            if (isActive && variant.getDiscount().getPercentage() != null) {
                BigDecimal discountPercentage = variant.getDiscount().getPercentage();
                BigDecimal originalPrice = variant.getPrice();
                BigDecimal discountedPrice = originalPrice.multiply(
                        BigDecimal.ONE.subtract(discountPercentage.divide(BigDecimal.valueOf(100))));
                dto.setDiscountedPrice(discountedPrice);
            }
        } else {
            dto.setHasActiveDiscount(false);
        }

        // Map variant images

        if (variant.getImages() != null) {

            dto.setImages(variant.getImages().stream()

                    .map(this::mapVariantImageToDTO)

                    .collect(Collectors.toList()));

        }

        // Map variant attributes

        if (variant.getAttributeValues() != null) {

            dto.setAttributes(variant.getAttributeValues().stream()
                    .map(this::mapVariantAttributeToInnerDTO)
                    .collect(Collectors.toList()));

        }

        List<Stock> variantStocks = stockRepository.findByProductVariant(variant);
        if (variantStocks != null && !variantStocks.isEmpty()) {
            dto.setWarehouseStocks(variantStocks.stream()
                    .map(this::mapStockToVariantWarehouseStockDTO)
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

    private ProductVariantDTO.VariantWarehouseStockDTO mapStockToVariantWarehouseStockDTO(Stock stock) {
        return ProductVariantDTO.VariantWarehouseStockDTO.builder()
                .warehouseId(stock.getWarehouse().getId())
                .warehouseName(stock.getWarehouse().getName())
                .warehouseLocation(stock.getWarehouse().getCity() + ", " + stock.getWarehouse().getState())
                .stockQuantity(stock.getQuantity())
                .lowStockThreshold(stock.getLowStockThreshold())
                .isLowStock(stock.getQuantity() <= stock.getLowStockThreshold())
                .lastUpdated(stock.getUpdatedAt())
                .build();
    }

    private DiscountDTO mapDiscountToDTO(Discount discount) {
        return DiscountDTO.builder()
                .discountId(discount.getDiscountId())
                .name(discount.getName())
                .description(discount.getDescription())
                .percentage(discount.getPercentage())
                .discountCode(discount.getDiscountCode())
                .startDate(discount.getStartDate())
                .endDate(discount.getEndDate())
                .active(discount.isActive())
                .usageLimit(discount.getUsageLimit())
                .usedCount(discount.getUsedCount())
                .discountType(discount.getDiscountType() != null ? discount.getDiscountType().toString() : null)
                .createdAt(discount.getCreatedAt())
                .updatedAt(discount.getUpdatedAt())
                .valid(discount.isValid())
                .build();
    }

    private boolean isDiscountCurrentlyActive(Discount discount) {
        if (discount == null || !discount.isActive()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return (discount.getStartDate() == null || now.isAfter(discount.getStartDate())
                || now.isEqual(discount.getStartDate())) &&
                (discount.getEndDate() == null || now.isBefore(discount.getEndDate())
                        || now.isEqual(discount.getEndDate()));
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

            // Delete variant stock entries

            deleteVariantStocks(variant);

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
     * Delete all stock entries associated with a product variant
     * 
     * @param variant The product variant
     */
    private void deleteVariantStocks(ProductVariant variant) {
        try {
            List<Stock> variantStocks = stockRepository.findByProductVariant(variant);

            if (variantStocks.isEmpty()) {
                log.debug("No stock entries found for variant ID: {}", variant.getId());
                return;
            }

            log.info("Deleting {} stock entries for variant ID: {}", variantStocks.size(), variant.getId());

            // Delete all stock entries from database
            stockRepository.deleteAll(variantStocks);

            log.info("Successfully deleted {} stock entries from database for variant ID: {}",
                    variantStocks.size(), variant.getId());

        } catch (Exception e) {
            log.error("Error deleting variant stocks for variant ID {}: {}", variant.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to delete variant stocks: " + e.getMessage(), e);
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
            if (!ensureProductsIndexExists()) {
                log.warn("Elasticsearch not available, falling back to database search");
                return getFallbackSearchSuggestions(query);
            }

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

    private boolean ensureProductsIndexExists() {
        try {
            if (elasticsearchClient == null) {
                log.warn("Elasticsearch client is not available");
                return false;
            }
            
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
            return true;
        } catch (Exception e) {
            log.warn("Elasticsearch is not available: {}. Search functionality will be limited.", e.getMessage());
            return false;
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
                stock.setProductVariant(null);
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
                Warehouse warehouse = warehouseRepository.findById(warehouseStock.getWarehouseId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Warehouse not found with ID: " + warehouseStock.getWarehouseId()));

                // Create Stock entity
                Stock stock = new Stock();
                stock.setWarehouse(warehouse);
                stock.setProduct(null); // Variant-level stock, not product
                stock.setProductVariant(variant);
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
            if (!ensureProductsIndexExists()) {
                log.warn("Elasticsearch not available, skipping product indexing for product: {}", product.getProductId());
                return;
            }

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

    private List<Product> applyRatingFilter(List<Product> products, Double minRating, Double maxRating) {
        return products.stream()
                .filter(product -> {
                    double averageRating = product.getAverageRating();
                    boolean matchesMin = minRating == null || averageRating >= minRating;
                    boolean matchesMax = maxRating == null || averageRating <= maxRating;
                    return matchesMin && matchesMax;
                })
                .collect(Collectors.toList());
    }

    @Override
    public WarehouseStockPageResponse getProductWarehouseStock(UUID productId, Pageable pageable) {
        try {
            log.info("Getting warehouse stock for product ID: {}", productId);

            // Verify product exists
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            List<Stock> allStock = stockRepository.findByProduct(product);

            Page<Stock> stockPage = stockRepository.findByProductOrProductVariantProduct(product, pageable);

            if (stockPage.getContent().isEmpty() && !allStock.isEmpty()) {

                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), allStock.size());
                List<Stock> paginatedStock = allStock.subList(start, end);

                stockPage = new org.springframework.data.domain.PageImpl<>(
                        paginatedStock,
                        pageable,
                        allStock.size());
            }

            // Map to DTOs
            List<WarehouseStockDTO> warehouseStockDTOs = stockPage.getContent().stream()
                    .map(this::mapStockToWarehouseStockDTO)
                    .collect(Collectors.toList());

            // Build response
            return WarehouseStockPageResponse.builder()
                    .content(warehouseStockDTOs)
                    .page(stockPage.getNumber())
                    .size(stockPage.getSize())
                    .totalElements(stockPage.getTotalElements())
                    .totalPages(stockPage.getTotalPages())
                    .first(stockPage.isFirst())
                    .last(stockPage.isLast())
                    .hasNext(stockPage.hasNext())
                    .hasPrevious(stockPage.hasPrevious())
                    .build();

        } catch (Exception e) {
            log.error("Error getting warehouse stock for product ID {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Failed to get warehouse stock: " + e.getMessage(), e);
        }
    }

    @Override
    public WarehouseStockPageResponse getVariantWarehouseStock(UUID productId, Long variantId, Pageable pageable) {
        try {

            // Verify product exists
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            // Verify variant exists and belongs to the product
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));

            if (!variant.getProduct().getProductId().equals(productId)) {
                throw new IllegalArgumentException("Variant does not belong to the specified product");
            }

            // Get stock entries for the variant
            Page<Stock> stockPage = stockRepository.findByProductVariant(variant, pageable);

            // Map to DTOs
            List<WarehouseStockDTO> warehouseStockDTOs = stockPage.getContent().stream()
                    .map(this::mapStockToWarehouseStockDTO)
                    .collect(Collectors.toList());

            // Build response
            return WarehouseStockPageResponse.builder()
                    .content(warehouseStockDTOs)
                    .page(stockPage.getNumber())
                    .size(stockPage.getSize())
                    .totalElements(stockPage.getTotalElements())
                    .totalPages(stockPage.getTotalPages())
                    .first(stockPage.isFirst())
                    .last(stockPage.isLast())
                    .hasNext(stockPage.hasNext())
                    .hasPrevious(stockPage.hasPrevious())
                    .build();

        } catch (Exception e) {
            log.error("Error getting warehouse stock for product ID {} and variant ID {}: {}",
                    productId, variantId, e.getMessage(), e);
            throw new RuntimeException("Failed to get variant warehouse stock: " + e.getMessage(), e);
        }
    }

    /**
     * Map warehouse stock information to ProductDTO
     */
    private void mapWarehouseStockToDTO(Product product, ProductDTO dto) {
        try {
            // Get all stock entries for the product (both product-level and variant-level)
            List<Stock> allStock = stockRepository.findByProductOrProductVariantProduct(product,
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();

            if (!allStock.isEmpty()) {
                // Map stock entries to DTOs
                List<WarehouseStockDTO> warehouseStockDTOs = allStock.stream()
                        .map(this::mapStockToWarehouseStockDTO)
                        .collect(Collectors.toList());

                dto.setWarehouseStock(warehouseStockDTOs);
                dto.setTotalWarehouses((int) warehouseStockDTOs.stream()
                        .mapToLong(WarehouseStockDTO::getWarehouseId)
                        .distinct()
                        .count());
                dto.setTotalWarehouseStock(warehouseStockDTOs.stream()
                        .mapToInt(WarehouseStockDTO::getQuantity)
                        .sum());
            } else {
                dto.setWarehouseStock(new ArrayList<>());
                dto.setTotalWarehouses(0);
                dto.setTotalWarehouseStock(0);
            }
        } catch (Exception e) {
            log.warn("Error mapping warehouse stock for product {}: {}", product.getProductId(), e.getMessage());
            dto.setWarehouseStock(new ArrayList<>());
            dto.setTotalWarehouses(0);
            dto.setTotalWarehouseStock(0);
        }
    }

    /**
     * Map Stock entity to WarehouseStockDTO
     */
    private WarehouseStockDTO mapStockToWarehouseStockDTO(Stock stock) {
        Warehouse warehouse = stock.getWarehouse();

        // Get batches for this stock
        List<StockBatch> batches = stockBatchRepository.findByStockOrderByCreatedAtDesc(stock);
        List<StockBatchDTO> batchDTOs = batches.stream()
                .map(this::mapStockBatchToDTO)
                .collect(Collectors.toList());

        // Calculate batch statistics
        int totalBatches = batches.size();
        int activeBatches = (int) batches.stream()
                .filter(batch -> batch.getStatus() == com.ecommerce.enums.BatchStatus.ACTIVE)
                .count();
        int expiredBatches = (int) batches.stream()
                .filter(batch -> batch.getStatus() == com.ecommerce.enums.BatchStatus.EXPIRED)
                .count();
        int recalledBatches = (int) batches.stream()
                .filter(batch -> batch.getStatus() == com.ecommerce.enums.BatchStatus.RECALLED)
                .count();

        return WarehouseStockDTO.builder()
                .stockId(stock.getId())
                .warehouseId(warehouse.getId())
                .warehouseName(warehouse.getName())
                .warehouseAddress(warehouse.getAddress())
                .warehouseCity(warehouse.getCity())
                .warehouseState(warehouse.getState())
                .warehouseCountry(warehouse.getCountry())
                .warehouseContactNumber(warehouse.getContactNumber())
                .warehouseEmail(warehouse.getEmail())
                .quantity(stock.getQuantity())
                .lowStockThreshold(stock.getLowStockThreshold())
                .isInStock(stock.isInStock())
                .isLowStock(stock.isLowStock())
                .isOutOfStock(stock.isOutOfStock())
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .variantId(stock.getProductVariant() != null ? stock.getProductVariant().getId() : null)
                .variantSku(stock.getProductVariant() != null ? stock.getProductVariant().getVariantSku() : null)
                .variantName(stock.getProductVariant() != null ? stock.getProductVariant().getVariantName() : null)
                .isVariantBased(stock.isVariantBased())
                .batches(batchDTOs)
                .totalBatches(totalBatches)
                .activeBatches(activeBatches)
                .expiredBatches(expiredBatches)
                .recalledBatches(recalledBatches)
                .build();
    }

    /**
     * Map StockBatch entity to StockBatchDTO
     */
    private StockBatchDTO mapStockBatchToDTO(StockBatch stockBatch) {
        return StockBatchDTO.builder()
                .id(stockBatch.getId())
                .stockId(stockBatch.getStock().getId())
                .batchNumber(stockBatch.getBatchNumber())
                .manufactureDate(stockBatch.getManufactureDate())
                .expiryDate(stockBatch.getExpiryDate())
                .quantity(stockBatch.getQuantity())
                .status(stockBatch.getStatus())
                .supplierName(stockBatch.getSupplierName())
                .supplierBatchNumber(stockBatch.getSupplierBatchNumber())
                .createdAt(stockBatch.getCreatedAt())
                .updatedAt(stockBatch.getUpdatedAt())
                .productName(stockBatch.getEffectiveProductName())
                .warehouseName(stockBatch.getWarehouseName())
                .warehouseId(stockBatch.getStock().getWarehouse().getId())
                .productId(stockBatch.getStock().getEffectiveProduct() != null
                        ? stockBatch.getStock().getEffectiveProduct().getProductId().toString()
                        : null)
                .variantId(stockBatch.getStock().getProductVariant() != null
                        ? stockBatch.getStock().getProductVariant().getId().toString()
                        : null)
                .variantName(stockBatch.getStock().getProductVariant() != null
                        ? stockBatch.getStock().getProductVariant().getVariantName()
                        : null)
                .isExpired(stockBatch.isExpired())
                .isExpiringSoon(stockBatch.isExpiringSoon(30))
                .isEmpty(stockBatch.isEmpty())
                .isRecalled(stockBatch.isRecalled())
                .isAvailable(stockBatch.isAvailable())
                .build();
    }

    @Override
    public Page<ProductVariantDTO> getProductVariants(UUID productId, Pageable pageable) {
        try {
            log.info("Getting variants for product ID: {} with pagination", productId);

            // Verify product exists
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            // Get variants with pagination
            Page<ProductVariant> variantPage = productVariantRepository.findByProduct(product, pageable);

            // Map to DTOs
            Page<ProductVariantDTO> variantDTOPage = variantPage.map(this::mapProductVariantToDTO);

            log.info("Found {} variants for product ID: {}", variantDTOPage.getTotalElements(), productId);
            return variantDTOPage;

        } catch (Exception e) {
            log.error("Error getting variants for product ID {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Failed to get product variants: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ManyProductsDto> getSimilarProducts(SimilarProductsRequestDTO request) {
        try {
            log.info("Getting similar products for product ID: {}", request.getProductId());

            Product currentProduct = productRepository.findById(request.getProductId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Product not found with ID: " + request.getProductId()));

            Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
            Page<Product> similarProducts;

            switch (request.getAlgorithm().toLowerCase()) {
                case "brand":
                    similarProducts = getSimilarProductsByBrand(currentProduct, pageable,
                            request.isIncludeOutOfStock());
                    break;
                case "category":
                    similarProducts = getSimilarProductsByCategory(currentProduct, pageable,
                            request.isIncludeOutOfStock());
                    break;
                case "keywords":
                    similarProducts = getSimilarProductsByKeywords(currentProduct, pageable,
                            request.isIncludeOutOfStock());
                    break;
                case "popular":
                    similarProducts = getSimilarProductsByPopularity(currentProduct, pageable,
                            request.isIncludeOutOfStock());
                    break;
                case "mixed":
                default:
                    similarProducts = getSimilarProductsMixed(currentProduct, pageable, request.isIncludeOutOfStock());
                    break;
            }

            return similarProducts.map(this::mapProductToManyProductsDto);

        } catch (Exception e) {
            log.error("Error getting similar products for product ID: {}", request.getProductId(), e);
            throw new RuntimeException("Failed to get similar products", e);
        }
    }

    private Page<Product> getSimilarProductsByBrand(Product currentProduct, Pageable pageable,
            boolean includeOutOfStock) {
        if (currentProduct.getBrand() == null) {
            return Page.empty(pageable);
        }

        if (includeOutOfStock) {
            return productRepository.findByBrandAndProductIdNot(currentProduct.getBrand(),
                    currentProduct.getProductId(), pageable);
        } else {
            return productRepository.findByBrandAndProductIdNotAndInStock(currentProduct.getBrand(),
                    currentProduct.getProductId(), pageable);
        }
    }

    private Page<Product> getSimilarProductsByCategory(Product currentProduct, Pageable pageable,
            boolean includeOutOfStock) {
        if (currentProduct.getCategory() == null) {
            return Page.empty(pageable);
        }

        if (includeOutOfStock) {
            return productRepository.findByCategoryAndProductIdNot(currentProduct.getCategory(),
                    currentProduct.getProductId(), pageable);
        } else {
            return productRepository.findByCategoryAndProductIdNotAndInStock(currentProduct.getCategory(),
                    currentProduct.getProductId(), pageable);

        }
    }

    private Page<Product> getSimilarProductsByKeywords(Product currentProduct, Pageable pageable,
            boolean includeOutOfStock) {
        String searchKeywords = extractKeywords(currentProduct);
        if (searchKeywords.isEmpty()) {
            return Page.empty(pageable);
        }

        if (includeOutOfStock) {
            return productRepository.findByKeywordsAndProductIdNot(searchKeywords, currentProduct.getProductId(),
                    pageable);
        } else {
            return productRepository.findByKeywordsAndProductIdNotAndInStock(searchKeywords,
                    currentProduct.getProductId(), pageable);
        }
    }

    private Page<Product> getSimilarProductsByPopularity(Product currentProduct, Pageable pageable,
            boolean includeOutOfStock) {
        if (includeOutOfStock) {
            return productRepository.findByPopularityAndProductIdNot(currentProduct.getProductId(), pageable);
        } else {
            return productRepository.findByPopularityAndProductIdNotAndInStock(currentProduct.getProductId(), pageable);
        }
    }

    private Page<Product> getSimilarProductsMixed(Product currentProduct, Pageable pageable,
            boolean includeOutOfStock) {
        List<Product> similarProducts = new ArrayList<>();

        if (currentProduct.getBrand() != null) {
            Page<Product> brandProducts = getSimilarProductsByBrand(currentProduct, PageRequest.of(0, 4),
                    includeOutOfStock);
            similarProducts.addAll(brandProducts.getContent());
        }

        if (currentProduct.getCategory() != null) {
            Page<Product> categoryProducts = getSimilarProductsByCategory(currentProduct, PageRequest.of(0, 4),
                    includeOutOfStock);
            similarProducts.addAll(categoryProducts.getContent());
        }

        String keywords = extractKeywords(currentProduct);
        if (!keywords.isEmpty()) {
            Page<Product> keywordProducts = getSimilarProductsByKeywords(currentProduct, PageRequest.of(0, 4),
                    includeOutOfStock);
            similarProducts.addAll(keywordProducts.getContent());
        }

        Page<Product> popularProducts = getSimilarProductsByPopularity(currentProduct, PageRequest.of(0, 4),
                includeOutOfStock);
        similarProducts.addAll(popularProducts.getContent());

        Set<Product> uniqueProducts = new LinkedHashSet<>(similarProducts);
        List<Product> finalList = new ArrayList<>(uniqueProducts);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), finalList.size());

        if (start >= finalList.size()) {
            return Page.empty(pageable);
        }

        List<Product> pageContent = finalList.subList(start, end);
        return new PageImpl<>(pageContent, pageable, finalList.size());
    }

    private String extractKeywords(Product product) {
        StringBuilder keywords = new StringBuilder();

        if (product.getProductName() != null) {
            keywords.append(product.getProductName()).append(" ");
        }

        if (product.getDescription() != null) {
            keywords.append(product.getDescription()).append(" ");
        }

        if (product.getMetaKeywords() != null) {
            keywords.append(product.getMetaKeywords()).append(" ");
        }

        return keywords.toString().trim();
    }

    @Override
    public List<ManyProductsDto> getProductsByIds(List<String> productIds) {
        try {
            log.info("Fetching products by IDs: {}", productIds);

            // Convert string IDs to UUIDs
            List<UUID> uuids = productIds.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());

            // Fetch products from database
            List<Product> products = productRepository.findAllById(uuids);

            // Convert to DTOs
            return products.stream()
                    .map(this::mapProductToManyProductsDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching products by IDs: {}", productIds, e);
            throw new RuntimeException("Failed to fetch products by IDs", e);
        }
    }

    @Override
    public ProductPricingDTO getProductPricing(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        return mapProductToPricingDTO(product);
    }

    @Override
    public ProductPricingDTO updateProductPricing(UUID productId, ProductPricingUpdateDTO updateDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        // Validate that at least one pricing field is provided
        boolean hasUpdates = updateDTO.getPrice() != null ||
                updateDTO.getCompareAtPrice() != null ||
                updateDTO.getCostPrice() != null;

        if (!hasUpdates) {
            throw new IllegalArgumentException("At least one pricing field must be provided for update");
        }

        // Update pricing fields only if provided
        if (updateDTO.getPrice() != null) {
            if (updateDTO.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
            product.setPrice(updateDTO.getPrice());
        }

        if (updateDTO.getCompareAtPrice() != null) {
            if (updateDTO.getCompareAtPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Compare at price cannot be negative");
            }
            product.setCompareAtPrice(updateDTO.getCompareAtPrice());
        }

        if (updateDTO.getCostPrice() != null) {
            if (updateDTO.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Cost price cannot be negative");
            }
            product.setCostPrice(updateDTO.getCostPrice());
        }

        // Save the updated product
        Product savedProduct = productRepository.save(product);

        // Return the updated pricing info
        return mapProductToPricingDTO(savedProduct);
    }

    /**
     * Maps a Product entity to ProductPricingDTO
     */
    private ProductPricingDTO mapProductToPricingDTO(Product product) {
        ProductPricingDTO dto = new ProductPricingDTO();
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setSku(product.getSku());
        dto.setPrice(product.getPrice());
        dto.setCompareAtPrice(product.getCompareAtPrice());
        dto.setCostPrice(product.getCostPrice());
        dto.setCurrency("USD"); // Default currency, can be made configurable

        // Calculate profit margin and percentage
        if (product.getPrice() != null && product.getCostPrice() != null) {
            BigDecimal profitMargin = product.getPrice().subtract(product.getCostPrice());
            dto.setProfitMargin(profitMargin);

            if (product.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal profitPercentage = profitMargin.divide(product.getCostPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                dto.setProfitPercentage(profitPercentage);
            } else {
                dto.setProfitPercentage(BigDecimal.ZERO);
            }
        } else {
            dto.setProfitMargin(BigDecimal.ZERO);
            dto.setProfitPercentage(BigDecimal.ZERO);
        }

        return dto;
    }

    @Override
    public List<ProductMediaDTO> getProductImages(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        return product.getImages().stream()
                .map(this::mapProductImageToMediaDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductVideoDTO> getProductVideos(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        return product.getVideos().stream()
                .map(this::mapProductVideoToVideoDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProductImage(UUID productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with ID: " + imageId));

        if (!image.getProduct().getProductId().equals(productId)) {
            throw new EntityNotFoundException("Image does not belong to the specified product");
        }

        try {
            if (image.getImageUrl() != null && !image.getImageUrl().isEmpty()) {
                cloudinaryService.deleteImage(image.getImageUrl());
            }
        } catch (Exception e) {
            log.warn("Failed to delete image from Cloudinary: {}. Error: {}", image.getImageUrl(), e.getMessage());
        }

        productImageRepository.delete(image);
        log.info("Successfully deleted product image with ID: {}", imageId);
    }

    @Override
    @Transactional
    public void deleteProductVideo(UUID productId, Long videoId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        ProductVideo video = productVideoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException("Video not found with ID: " + videoId));

        if (!video.getProduct().getProductId().equals(productId)) {
            throw new EntityNotFoundException("Video does not belong to the specified product");
        }

        try {
            if (video.getUrl() != null && !video.getUrl().isEmpty()) {
                cloudinaryService.deleteFile(video.getUrl());
            }
        } catch (Exception e) {
            log.warn("Failed to delete video from Cloudinary: {}. Error: {}", video.getUrl(), e.getMessage());
        }

        productVideoRepository.delete(video);
        log.info("Successfully deleted product video with ID: {}", videoId);
    }

    @Override
    @Transactional
    public void setPrimaryImage(UUID productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        ProductImage targetImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with ID: " + imageId));

        if (!targetImage.getProduct().getProductId().equals(productId)) {
            throw new EntityNotFoundException("Image does not belong to the specified product");
        }

        product.getImages().forEach(image -> image.setPrimary(false));
        targetImage.setPrimary(true);

        productImageRepository.saveAll(product.getImages());
        log.info("Successfully set image {} as primary for product {}", imageId, productId);
    }

    private ProductMediaDTO mapProductImageToMediaDTO(ProductImage image) {
        ProductMediaDTO dto = new ProductMediaDTO();
        dto.setImageId(image.getId());
        dto.setUrl(image.getImageUrl());
        dto.setAltText(image.getAltText());
        dto.setPrimary(image.isPrimary());
        dto.setSortOrder(image.getSortOrder());
        dto.setFileSize(image.getFileSize());
        dto.setMimeType(image.getMimeType());
        return dto;
    }

    private ProductVideoDTO mapProductVideoToVideoDTO(ProductVideo video) {
        ProductVideoDTO dto = new ProductVideoDTO();
        dto.setVideoId(video.getVideoId());
        dto.setUrl(video.getUrl());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setSortOrder(video.getSortOrder());
        return dto;
    }

    @Override
    @Transactional
    public List<ProductMediaDTO> uploadProductImages(UUID productId, List<MultipartFile> images) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("At least one image must be provided");
        }

        for (MultipartFile image : images) {
            if (image.getSize() > 50 * 1024 * 1024) {
                throw new IllegalArgumentException(
                        String.format("Image '%s' file size (%.2f MB) exceeds maximum allowed (50 MB)",
                                image.getOriginalFilename(), image.getSize() / (1024.0 * 1024.0)));
            }
        }

        List<Map<String, String>> uploadResults = cloudinaryService.uploadMultipleImages(images);
        List<ProductMediaDTO> uploadedImages = new ArrayList<>();

        for (int i = 0; i < uploadResults.size(); i++) {
            Map<String, String> uploadResult = uploadResults.get(i);

            if (uploadResult.containsKey("error")) {
                log.error("Failed to upload image {}: {}", i, uploadResult.get("error"));
                throw new RuntimeException("Failed to upload image: " + uploadResult.get("error"));
            }

            ProductImage productImage = new ProductImage();
            productImage.setProduct(product);
            productImage.setImageUrl(uploadResult.get("url"));
            productImage.setAltText(images.get(i).getOriginalFilename());

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

            productImage.setSortOrder(product.getImages().size());
            productImage.setPrimary(false);

            ProductImage savedImage = productImageRepository.save(productImage);
            uploadedImages.add(mapProductImageToMediaDTO(savedImage));
        }

        log.info("Successfully uploaded {} images for product {}", images.size(), productId);
        return uploadedImages;
    }

    @Override
    @Transactional
    public List<ProductVideoDTO> uploadProductVideos(UUID productId, List<MultipartFile> videos) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        if (videos == null || videos.isEmpty()) {
            throw new IllegalArgumentException("At least one video must be provided");
        }

        for (MultipartFile video : videos) {
            if (video.getSize() > 100 * 1024 * 1024) {
                throw new IllegalArgumentException(
                        String.format("Video '%s' file size (%.2f MB) exceeds maximum allowed (100 MB)",
                                video.getOriginalFilename(), video.getSize() / (1024.0 * 1024.0)));
            }
        }

        List<Map<String, String>> uploadResults = cloudinaryService.uploadMultipleVideos(videos);
        List<ProductVideoDTO> uploadedVideos = new ArrayList<>();

        for (int i = 0; i < uploadResults.size(); i++) {
            Map<String, String> uploadResult = uploadResults.get(i);

            if (uploadResult.containsKey("error")) {
                log.error("Failed to upload video {}: {}", i, uploadResult.get("error"));
                throw new RuntimeException("Failed to upload video: " + uploadResult.get("error"));
            }

            ProductVideo productVideo = new ProductVideo();
            productVideo.setProduct(product);
            productVideo.setUrl(uploadResult.get("url"));
            productVideo.setTitle(videos.get(i).getOriginalFilename());
            productVideo.setDescription("Uploaded video");

            productVideo.setSortOrder(product.getVideos().size());

            ProductVideo savedVideo = productVideoRepository.save(productVideo);
            uploadedVideos.add(mapProductVideoToVideoDTO(savedVideo));
        }

        log.info("Successfully uploaded {} videos for product {}", videos.size(), productId);
        return uploadedVideos;
    }

    @Override
    public ProductVariantDTO updateProductVariant(UUID productId, Long variantId, Map<String, Object> updates) {
        try {
            log.info("Updating variant {} for product {}", variantId, productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));

            if (!variant.getProduct().getProductId().equals(productId)) {
                throw new IllegalArgumentException("Variant does not belong to the specified product");
            }

            boolean hasChanges = false;

            if (updates.containsKey("variantSku")) {
                String variantSku = (String) updates.get("variantSku");
                if (variantSku != null && !variantSku.trim().isEmpty()) {
                    Optional<ProductVariant> existingVariant = productVariantRepository
                            .findByVariantSku(variantSku.trim());
                    if (existingVariant.isPresent() && !existingVariant.get().getId().equals(variantId)) {
                        throw new IllegalArgumentException("SKU already exists for another variant");
                    }
                    variant.setVariantSku(variantSku.trim());
                    hasChanges = true;
                }
            }

            if (updates.containsKey("variantBarcode")) {
                String variantBarcode = (String) updates.get("variantBarcode");
                variant.setVariantBarcode(variantBarcode != null ? variantBarcode.trim() : null);
                hasChanges = true;
            }

            if (updates.containsKey("price")) {
                Object priceObj = updates.get("price");
                if (priceObj instanceof Number) {
                    BigDecimal price = BigDecimal.valueOf(((Number) priceObj).doubleValue());
                    if (price.compareTo(BigDecimal.ZERO) >= 0) {
                        variant.setPrice(price);
                        hasChanges = true;
                    }
                }
            }

            if (updates.containsKey("salePrice")) {
                Object salePriceObj = updates.get("salePrice");
                if (salePriceObj instanceof Number) {
                    BigDecimal salePrice = BigDecimal.valueOf(((Number) salePriceObj).doubleValue());
                    variant.setCompareAtPrice(salePrice.compareTo(BigDecimal.ZERO) >= 0 ? salePrice : null);
                    hasChanges = true;
                } else if (salePriceObj == null) {
                    variant.setCompareAtPrice(null);
                    hasChanges = true;
                }
            }

            if (updates.containsKey("costPrice")) {
                Object costPriceObj = updates.get("costPrice");
                if (costPriceObj instanceof Number) {
                    BigDecimal costPrice = BigDecimal.valueOf(((Number) costPriceObj).doubleValue());
                    variant.setCostPrice(costPrice.compareTo(BigDecimal.ZERO) >= 0 ? costPrice : null);
                    hasChanges = true;
                } else if (costPriceObj == null) {
                    variant.setCostPrice(null);
                    hasChanges = true;
                }
            }

            if (updates.containsKey("isActive")) {
                Boolean isActive = (Boolean) updates.get("isActive");
                if (isActive != null) {
                    variant.setActive(isActive);
                    hasChanges = true;
                }
            }

            if (hasChanges) {
                variant.setUpdatedAt(LocalDateTime.now());
                ProductVariant savedVariant = productVariantRepository.save(variant);
                log.info("Successfully updated variant {} for product {}", variantId, productId);
                return mapProductVariantToDTO(savedVariant);
            } else {
                log.info("No changes detected for variant {} of product {}", variantId, productId);
                return mapProductVariantToDTO(variant);
            }

        } catch (Exception e) {
            log.error("Error updating variant {} for product {}: {}", variantId, productId, e.getMessage(), e);
            throw new RuntimeException("Failed to update product variant: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteVariantImage(UUID productId, Long variantId, Long imageId) {
        try {
            log.info("Deleting image {} from variant {} of product {}", imageId, variantId, productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));

            if (!variant.getProduct().getProductId().equals(productId)) {
                throw new IllegalArgumentException("Variant does not belong to the specified product");
            }

            ProductVariantImage variantImage = variant.getImages().stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Image not found for this variant"));

            variant.getImages().remove(variantImage);
            productVariantImageRepository.delete(variantImage);
            productVariantRepository.save(variant);

            log.info("Successfully deleted image {} from variant {} of product {}", imageId, variantId, productId);

        } catch (Exception e) {
            log.error("Error deleting image {} from variant {} of product {}: {}", imageId, variantId, productId,
                    e.getMessage(), e);
            throw new RuntimeException("Failed to delete variant image: " + e.getMessage(), e);
        }
    }

    @Override
    public void setPrimaryVariantImage(UUID productId, Long variantId, Long imageId) {
        try {
            log.info("Setting image {} as primary for variant {} of product {}", imageId, variantId, productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));

            if (!variant.getProduct().getProductId().equals(productId)) {
                throw new IllegalArgumentException("Variant does not belong to the specified product");
            }

            ProductVariantImage targetImage = variant.getImages().stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Image not found for this variant"));

            variant.getImages().forEach(img -> img.setPrimary(false));
            targetImage.setPrimary(true);

            productVariantRepository.save(variant);

            log.info("Successfully set image {} as primary for variant {} of product {}", imageId, variantId,
                    productId);

        } catch (Exception e) {
            log.error("Error setting image {} as primary for variant {} of product {}: {}", imageId, variantId,
                    productId, e.getMessage(), e);
            throw new RuntimeException("Failed to set primary variant image: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProductVariantImageDTO> uploadVariantImages(UUID productId, Long variantId,
            List<MultipartFile> images) {
        try {
            log.info("Uploading {} images for variant {} of product {}", images.size(), variantId, productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));

            if (!variant.getProduct().getProductId().equals(productId)) {
                throw new IllegalArgumentException("Variant does not belong to the specified product");
            }

            List<ProductVariantImage> variantImages = variant.getImages();
            if (variantImages == null) {
                variantImages = new ArrayList<>();
            }

            int currentImageCount = variantImages.size();
            int newImageCount = images.size();

            if (currentImageCount + newImageCount > 10) {
                throw new IllegalArgumentException("Variant cannot have more than 10 images. Current: "
                        + currentImageCount + ", Attempting to add: " + newImageCount);
            }

            List<ProductVariantImageDTO> uploadedImages = new ArrayList<>();

            for (MultipartFile image : images) {
                if (image.isEmpty()) {
                    continue;
                }

                Map<String, String> uploadResult = cloudinaryService.uploadImage(image);

                if (uploadResult.containsKey("error")) {
                    log.error("Failed to upload image: {}", uploadResult.get("error"));
                    throw new RuntimeException("Failed to upload image: " + uploadResult.get("error"));
                }

                ProductVariantImage variantImage = new ProductVariantImage();
                variantImage.setProductVariant(variant);
                variantImage.setImageUrl(uploadResult.get("url"));
                variantImage.setAltText(image.getOriginalFilename());
                variantImage.setPrimary(variantImages.isEmpty());
                variantImage.setSortOrder(variantImages.size());

                ProductVariantImage savedImage = productVariantImageRepository.save(variantImage);
                uploadedImages.add(mapProductVariantImageToDTO(savedImage));
            }

            log.info("Successfully uploaded {} images for variant {} of product {}", uploadedImages.size(), variantId,
                    productId);
            return uploadedImages;

        } catch (Exception e) {
            log.error("Error uploading images for variant {} of product {}: {}", variantId, productId, e.getMessage(),
                    e);
            throw new RuntimeException("Failed to upload variant images: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeVariantAttribute(UUID productId, Long variantId, Long attributeValueId) {
        try {
            log.info("Removing attribute {} from variant {} of product {}", attributeValueId, variantId, productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));

            if (!variant.getProduct().getProductId().equals(productId)) {
                throw new IllegalArgumentException("Variant does not belong to the specified product");
            }

            if (variant.getAttributeValues().size() <= 1) {
                throw new IllegalArgumentException("Product variant must have at least one attribute");
            }

            VariantAttributeValue variantAttribute = variant.getAttributeValues().stream()
                    .filter(attr -> attr.getAttributeValue().getAttributeValueId().equals(attributeValueId))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Attribute not found for this variant"));

            variant.getAttributeValues().remove(variantAttribute);
            variantAttributeValueRepository.delete(variantAttribute);
            productVariantRepository.save(variant);

            log.info("Successfully removed attribute {} from variant {} of product {}", attributeValueId, variantId,
                    productId);

        } catch (Exception e) {
            log.error("Error removing attribute {} from variant {} of product {}: {}", attributeValueId, variantId,
                    productId, e.getMessage(), e);
            throw new RuntimeException("Failed to remove variant attribute: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProductVariantAttributeDTO> addVariantAttributes(UUID productId, Long variantId,
            List<VariantAttributeRequest> attributeRequests) {
        try {
            log.info("Adding {} attributes to variant {} of product {}", attributeRequests.size(), variantId,
                    productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + variantId));

            if (!variant.getProduct().getProductId().equals(productId)) {
                throw new IllegalArgumentException("Variant does not belong to the specified product");
            }

            List<ProductVariantAttributeDTO> addedAttributes = new ArrayList<>();

            for (VariantAttributeRequest request : attributeRequests) {
                String attributeTypeName = request.getAttributeTypeName().trim();
                String attributeValue = request.getAttributeValue().trim();

                ProductAttributeType attributeType = productAttributeTypeRepository
                        .findByNameIgnoreCase(attributeTypeName)
                        .orElse(null);

                if (attributeType == null) {
                    attributeType = new ProductAttributeType();
                    attributeType.setName(attributeTypeName);
                    attributeType = productAttributeTypeRepository.save(attributeType);
                    log.info("Created new attribute type: {}", attributeTypeName);
                }

                final ProductAttributeValue attributeValueEntity;
                ProductAttributeValue existingValue = attributeType.getAttributeValues().stream()
                        .filter(val -> val.getValue().equalsIgnoreCase(attributeValue))
                        .findFirst()
                        .orElse(null);

                if (existingValue == null) {
                    ProductAttributeValue newAttributeValue = new ProductAttributeValue();
                    newAttributeValue.setAttributeType(attributeType);
                    newAttributeValue.setValue(attributeValue);
                    attributeValueEntity = productAttributeValueRepository.save(newAttributeValue);
                    log.info("Created new attribute value: {} for type: {}", attributeValue, attributeTypeName);
                } else {
                    attributeValueEntity = existingValue;
                }

                List<VariantAttributeValue> attributeValues = variant.getAttributeValues();
                if (attributeValues == null) {
                    attributeValues = new ArrayList<>();
                }

                boolean attributeExists = attributeValues.stream()
                        .anyMatch(attr -> attr.getAttributeValue().getAttributeValueId()
                                .equals(attributeValueEntity.getAttributeValueId()));

                if (!attributeExists) {
                    VariantAttributeValue variantAttribute = new VariantAttributeValue();

                    // Set the composite key explicitly
                    VariantAttributeValue.VariantAttributeValueId compositeId = new VariantAttributeValue.VariantAttributeValueId();
                    compositeId.setVariantId(variant.getId());
                    compositeId.setAttributeValueId(attributeValueEntity.getAttributeValueId());
                    variantAttribute.setId(compositeId);

                    variantAttribute.setProductVariant(variant);
                    variantAttribute.setAttributeValue(attributeValueEntity);

                    VariantAttributeValue savedAttribute = variantAttributeValueRepository.save(variantAttribute);
                    addedAttributes.add(mapVariantAttributeToDTO(savedAttribute));
                    log.info("Added attribute {}:{} to variant {}", attributeTypeName, attributeValue, variantId);
                } else {
                    log.info("Attribute {}:{} already exists for variant {}", attributeTypeName, attributeValue,
                            variantId);
                }
            }

            log.info("Successfully added {} attributes to variant {} of product {}", addedAttributes.size(), variantId,
                    productId);
            return addedAttributes;

        } catch (Exception e) {
            log.error("Error adding attributes to variant {} of product {}: {}", variantId, productId, e.getMessage(),
                    e);
            throw new RuntimeException("Failed to add variant attributes: " + e.getMessage(), e);
        }
    }

    @Override
    public ProductVariantDTO createProductVariant(UUID productId, CreateVariantRequest request) {
        try {
            log.info("Creating new variant for product {}", productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            Optional<ProductVariant> existingVariant = productVariantRepository
                    .findByVariantSku(request.getVariantSku());
            if (existingVariant.isPresent()) {
                throw new IllegalArgumentException("SKU already exists for another variant");
            }

            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setVariantSku(request.getVariantSku());
            variant.setVariantBarcode(request.getVariantBarcode());
            variant.setPrice(request.getPrice());
            variant.setCompareAtPrice(request.getSalePrice());
            variant.setCostPrice(request.getCostPrice());
            variant.setActive(request.getIsActive());
            variant.setCreatedAt(LocalDateTime.now());

            ProductVariant savedVariant = productVariantRepository.save(variant);
            log.info("Created variant {} for product {}", savedVariant.getId(), productId);

            if (request.getImages() != null && !request.getImages().isEmpty()) {
                if (request.getImages().size() > 10) {
                    throw new IllegalArgumentException("Maximum 10 images allowed per variant");
                }

                List<ProductVariantImageDTO> uploadedImages = uploadVariantImages(productId, savedVariant.getId(),
                        request.getImages());
                log.info("Uploaded {} images for variant {}", uploadedImages.size(), savedVariant.getId());
            }

            if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
                List<ProductVariantAttributeDTO> addedAttributes = addVariantAttributes(productId, savedVariant.getId(),
                        request.getAttributes());
                log.info("Added {} attributes to variant {}", addedAttributes.size(), savedVariant.getId());
            }

            if (request.getWarehouseStocks() != null && !request.getWarehouseStocks().isEmpty()) {
                for (WarehouseStockRequest stockRequest : request.getWarehouseStocks()) {
                    Warehouse warehouse = warehouseRepository.findById(stockRequest.getWarehouseId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Warehouse not found with ID: " + stockRequest.getWarehouseId()));

                    Stock stock = new Stock();
                    stock.setProductVariant(savedVariant);
                    stock.setWarehouse(warehouse);
                    stock.setLowStockThreshold(stockRequest.getLowStockThreshold());
                    stock.setCreatedAt(LocalDateTime.now());
                    stockRepository.save(stock);
                }
                log.info("Added {} warehouse stocks for variant {}", request.getWarehouseStocks().size(),
                        savedVariant.getId());
            }

            ProductVariantDTO result = mapProductVariantToDTO(savedVariant);
            log.info("Successfully created variant {} for product {}", savedVariant.getId(), productId);
            return result;

        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating variant for product {}: {}", productId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating variant for product {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Failed to create product variant: " + e.getMessage(), e);
        }
    }

    private ProductVariantDTO.VariantAttributeDTO mapVariantAttributeToInnerDTO(
            VariantAttributeValue variantAttribute) {
        ProductAttributeValue attributeValue = variantAttribute.getAttributeValue();
        ProductAttributeType attributeType = attributeValue.getAttributeType();

        return ProductVariantDTO.VariantAttributeDTO.builder()
                .attributeValueId(attributeValue.getAttributeValueId())
                .attributeValue(attributeValue.getValue())
                .attributeTypeId(attributeType.getAttributeTypeId())
                .attributeType(attributeType.getName())
                .build();
    }

    private ProductVariantImageDTO mapProductVariantImageToDTO(ProductVariantImage image) {
        return ProductVariantImageDTO.builder()
                .imageId(image.getId())
                .url(image.getImageUrl())
                .altText(image.getAltText())
                .isPrimary(image.isPrimary())
                .sortOrder(image.getSortOrder())
                .createdAt(image.getCreatedAt())
                .build();
    }

    private ProductVariantAttributeDTO mapVariantAttributeToDTO(VariantAttributeValue variantAttribute) {
        ProductAttributeValue attributeValue = variantAttribute.getAttributeValue();
        ProductAttributeType attributeType = attributeValue.getAttributeType();

        return ProductVariantAttributeDTO.builder()
                .attributeValueId(attributeValue.getAttributeValueId())
                .attributeValue(attributeValue.getValue())
                .attributeTypeId(attributeType.getAttributeTypeId())
                .attributeType(attributeType.getName())
                .build();
    }

    @Override
    public ProductDetailsDTO getProductDetails(UUID productId) {
        try {
            log.info("Getting product details for product ID: {}", productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            ProductDetail productDetail = product.getProductDetail();
            if (productDetail == null) {
                productDetail = new ProductDetail();
                productDetail.setProduct(product);
                productDetail = productDetailRepository.save(productDetail);
                product.setProductDetail(productDetail);
            }

            return ProductDetailsDTO.builder()
                    .description(productDetail.getDescription())
                    .metaTitle(productDetail.getMetaTitle())
                    .metaDescription(productDetail.getMetaDescription())
                    .metaKeywords(productDetail.getMetaKeywords())
                    .searchKeywords(productDetail.getSearchKeywords())
                    .dimensionsCm(productDetail.getDimensionsCm())
                    .weightKg(productDetail.getWeightKg())
                    .material(productDetail.getMaterial())
                    .careInstructions(productDetail.getCareInstructions())
                    .warrantyInfo(productDetail.getWarrantyInfo())
                    .shippingInfo(productDetail.getShippingInfo())
                    .returnPolicy(productDetail.getReturnPolicy())
                    .maximumDaysForReturn(product.getMaximumDaysForReturn())
                    .displayToCustomers(product.getDisplayToCustomers())
                    .build();

        } catch (Exception e) {
            log.error("Error getting product details for product ID {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Failed to get product details: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ProductDetailsDTO updateProductDetails(UUID productId, ProductDetailsUpdateDTO updateDTO) {
        try {
            log.info("Updating product details for product ID: {}", productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

            ProductDetail productDetail = product.getProductDetail();
            if (productDetail == null) {
                productDetail = new ProductDetail();
                productDetail.setProduct(product);
            }

            boolean hasChanges = false;

            if (updateDTO.getDescription() != null) {
                productDetail.setDescription(updateDTO.getDescription());
                hasChanges = true;
            }

            if (updateDTO.getMetaTitle() != null) {
                productDetail.setMetaTitle(updateDTO.getMetaTitle());
                hasChanges = true;
            }

            if (updateDTO.getMetaDescription() != null) {
                productDetail.setMetaDescription(updateDTO.getMetaDescription());
                hasChanges = true;
            }

            if (updateDTO.getMetaKeywords() != null) {
                productDetail.setMetaKeywords(updateDTO.getMetaKeywords());
                hasChanges = true;
            }

            if (updateDTO.getSearchKeywords() != null) {
                productDetail.setSearchKeywords(updateDTO.getSearchKeywords());
                hasChanges = true;
            }

            if (updateDTO.getDimensionsCm() != null) {
                productDetail.setDimensionsCm(updateDTO.getDimensionsCm());
                hasChanges = true;
            }

            if (updateDTO.getWeightKg() != null) {
                productDetail.setWeightKg(updateDTO.getWeightKg());
                hasChanges = true;
            }

            if (updateDTO.getMaterial() != null) {
                productDetail.setMaterial(updateDTO.getMaterial());
                hasChanges = true;
            }

            if (updateDTO.getCareInstructions() != null) {
                productDetail.setCareInstructions(updateDTO.getCareInstructions());
                hasChanges = true;
            }

            if (updateDTO.getWarrantyInfo() != null) {
                productDetail.setWarrantyInfo(updateDTO.getWarrantyInfo());
                hasChanges = true;
            }

            if(updateDTO.getDisplayToCustomers() != null){
                product.setDisplayToCustomers(updateDTO.getDisplayToCustomers());
                log.info("Display to customers: {}", updateDTO.getDisplayToCustomers());
                hasChanges = true;
            }
            if (updateDTO.getShippingInfo() != null) {
                productDetail.setShippingInfo(updateDTO.getShippingInfo());
                hasChanges = true;
            }

            if (updateDTO.getReturnPolicy() != null) {
                productDetail.setReturnPolicy(updateDTO.getReturnPolicy());
                hasChanges = true;
            }

            if (updateDTO.getMaximumDaysForReturn() != null) {
                product.setMaximumDaysForReturn(updateDTO.getMaximumDaysForReturn());
                hasChanges = true;
            }

            if (!hasChanges) {
                throw new IllegalArgumentException("At least one field must be provided for update");
            }

            productDetailRepository.save(productDetail);
            productRepository.save(product);

            log.info("Successfully updated product details for product ID: {}", productId);

            return ProductDetailsDTO.builder()
                    .description(productDetail.getDescription())
                    .metaTitle(productDetail.getMetaTitle())
                    .metaDescription(productDetail.getMetaDescription())
                    .metaKeywords(productDetail.getMetaKeywords())
                    .searchKeywords(productDetail.getSearchKeywords())
                    .dimensionsCm(productDetail.getDimensionsCm())
                    .weightKg(productDetail.getWeightKg())
                    .material(productDetail.getMaterial())
                    .careInstructions(productDetail.getCareInstructions())
                    .warrantyInfo(productDetail.getWarrantyInfo())
                    .shippingInfo(productDetail.getShippingInfo())
                    .returnPolicy(productDetail.getReturnPolicy())
                    .maximumDaysForReturn(product.getMaximumDaysForReturn())
                    .build();

        } catch (Exception e) {
            log.error("Error updating product details for product ID {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Failed to update product details: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ManyProductsDto> getAllProductsForCustomers(Pageable pageable) {
        try {
            Page<Product> products = productRepository.findAvailableForCustomersWithStock(pageable);
            return products.map(this::convertToManyProductsDto);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get products for customers: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ManyProductsDto> getAllProductsForAdmins(Pageable pageable) {
        try {
            log.info("Getting all products for admins with pagination: {}", pageable);
            Page<Product> products = productRepository.findAvailableForAdmins(pageable);
            return products.map(this::convertToManyProductsDto);
        } catch (Exception e) {
            log.error("Error getting products for admins: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get products for admins: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ManyProductsDto> searchProductsForCustomers(ProductSearchDTO searchDTO) {
        try {
            
            Page<ManyProductsDto> searchResults = searchProducts(searchDTO);

            List<ManyProductsDto> availableProducts = searchResults.getContent().stream()
                .filter(product -> {
                    try {
                        UUID productUuid = product.getProductId();
                        Product productEntity = productRepository.findById(productUuid).orElse(null);
                        if (productEntity == null) {
                            return false;
                        }
                        return productAvailabilityService.isProductAvailableForCustomers(productEntity);
                    } catch (Exception e) {
                        log.warn("Error checking availability for product {}: {}", product.getProductId(), e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
            
            Page<ManyProductsDto> filteredResults = new PageImpl<>(
                availableProducts,
                searchResults.getPageable(),
                availableProducts.size()
            );
            
            return filteredResults;
        } catch (Exception e) {
            log.error("Error searching products for customers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search products for customers: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ManyProductsDto> searchProductsForAdmins(ProductSearchDTO searchDTO) {
        try {
            log.info("Searching products for admins with criteria: {}", searchDTO);
            return searchProducts(searchDTO);
        } catch (Exception e) {
            log.error("Error searching products for admins: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search products for admins: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ManyProductsDto> getFeaturedProductsForCustomers(Pageable pageable) {
        try {
            log.info("Getting featured products for customers with pagination: {}", pageable);
            Page<Product> products = productRepository.findFeaturedForCustomersWithStock(pageable);
            return products.map(this::convertToManyProductsDto);
        } catch (Exception e) {
            log.error("Error getting featured products for customers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get featured products for customers: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ManyProductsDto> getBestsellerProductsForCustomers(Pageable pageable) {
        try {
            log.info("Getting bestseller products for customers with pagination: {}", pageable);
            Page<Product> products = productRepository.findBestsellersForCustomersWithStock(pageable);
            return products.map(this::convertToManyProductsDto);
        } catch (Exception e) {
            log.error("Error getting bestseller products for customers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get bestseller products for customers: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ManyProductsDto> getNewArrivalProductsForCustomers(Pageable pageable) {
        try {
            log.info("Getting new arrival products for customers with pagination: {}", pageable);
            Page<Product> products = productRepository.findNewArrivalsForCustomersWithStock(pageable);
            return products.map(this::convertToManyProductsDto);
        } catch (Exception e) {
            log.error("Error getting new arrival products for customers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get new arrival products for customers: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ManyProductsDto> getProductsByCategoryForCustomers(Long categoryId, Pageable pageable) {
        try {
            log.info("Getting products by category {} for customers with pagination: {}", categoryId, pageable);
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + categoryId));
            
            Page<Product> products = productRepository.findByCategoryForCustomersWithStock(category, pageable);
            return products.map(this::convertToManyProductsDto);
        } catch (Exception e) {
            log.error("Error getting products by category for customers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get products by category for customers: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<ManyProductsDto> getProductsByBrandForCustomers(UUID brandId, Pageable pageable) {
        try {
            log.info("Getting products by brand {} for customers with pagination: {}", brandId, pageable);
            Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found with ID: " + brandId));
            
            Page<Product> products = productRepository.findByBrandForCustomersWithStock(brand, pageable);
            return products.map(this::convertToManyProductsDto);
        } catch (Exception e) {
            log.error("Error getting products by brand for customers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get products by brand for customers: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isProductAvailableForCustomers(UUID productId) {
        try {
            log.info("Checking if product {} is available for customers", productId);
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));
            
            return productAvailabilityService.isProductAvailableForCustomers(product);
        } catch (Exception e) {
            log.error("Error checking product availability for customers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check product availability for customers: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProductVariantDTO> getAvailableVariantsForCustomers(UUID productId) {
        try {
            log.info("Getting available variants for product {} for customers", productId);
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));
            
            List<ProductVariant> availableVariants = productAvailabilityService.getAvailableVariants(product);
            return availableVariants.stream()
                .map(this::convertToProductVariantDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting available variants for customers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get available variants for customers: " + e.getMessage(), e);
        }
    }

    private ManyProductsDto convertToManyProductsDto(Product product) {
        try {
            ManyProductsDto.SimpleCategoryDto categoryDto = null;
            if (product.getCategory() != null) {
                categoryDto = ManyProductsDto.SimpleCategoryDto.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .description(product.getCategory().getDescription())
                    .slug(product.getCategory().getSlug())
                    .build();
            }

            ManyProductsDto.SimpleBrandDto brandDto = null;
            if (product.getBrand() != null) {
                brandDto = ManyProductsDto.SimpleBrandDto.builder()
                    .brandId(product.getBrand().getBrandId())
                    .brandName(product.getBrand().getBrandName())
                    .description(product.getBrand().getDescription())
                    .logoUrl(product.getBrand().getLogoUrl())
                    .build();
            }

            ManyProductsDto.SimpleProductImageDto primaryImageDto = null;
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                ProductImage primaryImage = product.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .orElse(product.getImages().get(0));
                
                primaryImageDto = ManyProductsDto.SimpleProductImageDto.builder()
                    .id(primaryImage.getId())
                    .imageUrl(primaryImage.getImageUrl())
                    .altText(primaryImage.getAltText())
                    .isPrimary(primaryImage.isPrimary())
                    .sortOrder(primaryImage.getSortOrder())
                    .build();
            }

            ManyProductsDto.SimpleDiscountDto discountDto = null;
            boolean hasActiveDiscount = false;
            if (product.getDiscount() != null) {
                Discount discount = product.getDiscount();
                LocalDateTime now = LocalDateTime.now();
                boolean isActive = discount.isActive() && 
                    (discount.getStartDate() == null || !discount.getStartDate().isAfter(now)) &&
                    (discount.getEndDate() == null || !discount.getEndDate().isBefore(now));
                
                if (isActive) {
                    hasActiveDiscount = true;
                    discountDto = ManyProductsDto.SimpleDiscountDto.builder()
                        .discountId(discount.getDiscountId())
                        .name(discount.getName())
                        .percentage(discount.getPercentage())
                        .startDate(discount.getStartDate() != null ? discount.getStartDate().toString() : null)
                        .endDate(discount.getEndDate() != null ? discount.getEndDate().toString() : null)
                        .active(discount.isActive())
                        .isValid(isActive)
                        .discountCode(discount.getDiscountCode())
                        .build();
                }
            }

            BigDecimal discountedPrice = null;
            if (hasActiveDiscount && product.getDiscount() != null) {
                BigDecimal discountAmount = product.getPrice()
                    .multiply(product.getDiscount().getPercentage())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                discountedPrice = product.getPrice().subtract(discountAmount);
            }

            int totalStock = productAvailabilityService.getTotalAvailableStock(product);

            return ManyProductsDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .discountedPrice(discountedPrice)
                .stockQuantity(totalStock)
                .category(categoryDto)
                .brand(brandDto)
                .isBestSeller(product.isBestseller())
                .isNew(product.isNewArrival())
                .isFeatured(product.isFeatured())
                .discountInfo(discountDto)
                .primaryImage(primaryImageDto)
                .averageRating(calculateAverageRating(product))
                .reviewCount(product.getReviews() != null ? product.getReviews().size() : 0)
                .hasActiveDiscount(hasActiveDiscount)
                .build();
        } catch (Exception e) {
            log.error("Error converting product to ManyProductsDto: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert product to DTO: " + e.getMessage(), e);
        }
    }

    private Double calculateAverageRating(Product product) {
        if (product.getReviews() == null || product.getReviews().isEmpty()) {
            return 0.0;
        }
        
        double sum = product.getReviews().stream()
            .mapToDouble(Review::getRating)
            .sum();
        
        return sum / product.getReviews().size();
    }

    private ProductVariantDTO convertToProductVariantDTO(ProductVariant variant) {
        try {
            List<ProductVariantDTO.VariantWarehouseStockDTO> warehouseStocks = new ArrayList<>();
            List<Stock> stocks = stockRepository.findByProductVariant(variant);
            
            for (Stock stock : stocks) {
                Integer totalQuantity = stockBatchRepository.getTotalActiveQuantityByStock(stock);
                if (totalQuantity == null) totalQuantity = 0;
                
                ProductVariantDTO.VariantWarehouseStockDTO stockDTO = ProductVariantDTO.VariantWarehouseStockDTO.builder()
                    .warehouseId(stock.getWarehouse().getId())
                    .warehouseName(stock.getWarehouse().getName())
                    .warehouseLocation(stock.getWarehouse().getAddress())
                    .stockQuantity(totalQuantity)
                    .lowStockThreshold(stock.getLowStockThreshold())
                    .isLowStock(totalQuantity <= stock.getLowStockThreshold())
                    .lastUpdated(stock.getUpdatedAt())
                    .build();
                
                warehouseStocks.add(stockDTO);
            }

            DiscountDTO discountDTO = null;
            boolean hasActiveDiscount = false;
            BigDecimal discountedPrice = null;
            
            if (variant.getDiscount() != null) {
                Discount discount = variant.getDiscount();
                LocalDateTime now = LocalDateTime.now();
                boolean isActive = discount.isActive() && 
                    (discount.getStartDate() == null || !discount.getStartDate().isAfter(now)) &&
                    (discount.getEndDate() == null || !discount.getEndDate().isBefore(now));
                
                if (isActive) {
                    hasActiveDiscount = true;
                    discountDTO = DiscountDTO.builder()
                        .discountId(discount.getDiscountId())
                        .name(discount.getName())
                        .description(discount.getDescription())
                        .percentage(discount.getPercentage())
                        .discountCode(discount.getDiscountCode())
                        .startDate(discount.getStartDate())
                        .endDate(discount.getEndDate())
                        .active(discount.isActive())
                        .valid(isActive)
                        .createdAt(discount.getCreatedAt())
                        .updatedAt(discount.getUpdatedAt())
                        .build();
                    
                    BigDecimal discountAmount = variant.getPrice()
                        .multiply(discount.getPercentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    discountedPrice = variant.getPrice().subtract(discountAmount);
                }
            }

            return ProductVariantDTO.builder()
                .variantId(variant.getId())
                .variantSku(variant.getVariantSku())
                .variantName(variant.getVariantName())
                .variantBarcode(variant.getVariantBarcode())
                .price(variant.getPrice())
                .salePrice(variant.getCompareAtPrice())
                .costPrice(variant.getCostPrice())
                .isActive(variant.isActive())
                .isInStock(productAvailabilityService.isVariantAvailableForCustomers(variant))
                .isLowStock(productAvailabilityService.isVariantLowStock(variant))
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .warehouseStocks(warehouseStocks)
                .discount(discountDTO)
                .discountedPrice(discountedPrice)
                .hasActiveDiscount(hasActiveDiscount)
                .build();
        } catch (Exception e) {
            log.error("Error converting variant to ProductVariantDTO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert variant to DTO: " + e.getMessage(), e);
        }
    }
}