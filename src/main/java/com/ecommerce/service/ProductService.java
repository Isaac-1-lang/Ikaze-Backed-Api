package com.ecommerce.service;

import com.ecommerce.dto.ManyProductsDto;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.dto.CustomerProductDTO;
import com.ecommerce.dto.ProductSearchDTO;
import com.ecommerce.dto.ProductUpdateDTO;
import com.ecommerce.dto.ProductBasicInfoDTO;
import com.ecommerce.dto.ProductBasicInfoUpdateDTO;
import com.ecommerce.dto.ProductPricingDTO;
import com.ecommerce.dto.ProductPricingUpdateDTO;
import com.ecommerce.dto.ProductMediaDTO;
import com.ecommerce.dto.ProductVideoDTO;
import com.ecommerce.dto.ProductVariantDTO;
import com.ecommerce.dto.ProductVariantImageDTO;
import com.ecommerce.dto.ProductVariantAttributeDTO;
import com.ecommerce.dto.VariantAttributeRequest;
import com.ecommerce.dto.CreateVariantRequest;
import com.ecommerce.dto.SimilarProductsRequestDTO;
import com.ecommerce.dto.WarehouseStockPageResponse;
import com.ecommerce.dto.WarehouseStockRequest;
import com.ecommerce.dto.WarehouseStockWithBatchesRequest;
import com.ecommerce.dto.ProductDetailsDTO;
import com.ecommerce.dto.ProductDetailsUpdateDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProductService {

        /**
         * Create an empty product for step-by-step editing
         * 
         * @param name The initial product name
         * @param shopId The shop ID this product belongs to
         * @return Map containing product ID and status information
         */
        Map<String, Object> createEmptyProduct(String name, UUID shopId);

        /**
         * Check if product has variants
         * 
         * @param productId The product ID
         * @return true if product has variants, false otherwise
         */
        boolean productHasVariants(UUID productId);

        /**
         * Check if product has stock assigned
         * 
         * @param productId The product ID
         * @return true if product has stock, false otherwise
         */
        boolean productHasStock(UUID productId);

        /**
         * Remove all stock for a product
         * 
         * @param productId The product ID
         */
        void removeProductStock(UUID productId);

        /**
         * Assign stock to product (only when product has no variants)
         * 
         * @param productId       The product ID
         * @param warehouseStocks List of warehouse stock assignments
         * @return Map containing success status and message
         */
        Map<String, Object> assignProductStock(UUID productId, List<WarehouseStockRequest> warehouseStocks);

        /**
         * Assign stock with batches to a product
         * 
         * @param productId       The product ID
         * @param warehouseStocks List of warehouse stock assignments with batch details
         * @return Map containing success status and message
         */
        Map<String, Object> assignProductStockWithBatches(UUID productId,
                        List<WarehouseStockWithBatchesRequest> warehouseStocks);

        /**
         * Unassign warehouse from product and delete all associated batches
         * 
         * @param productId   The product ID
         * @param warehouseId The warehouse ID to unassign
         * @return Map containing success status and message
         */
        Map<String, Object> unassignWarehouseFromProduct(UUID productId, Long warehouseId);

        /**
         * Assign stock with batches to a specific product variant
         * 
         * @param productId       The product ID
         * @param variantId       The variant ID
         * @param warehouseStocks List of warehouse stock assignments with batch details
         * @return Map containing success status and message
         */
        Map<String, Object> assignVariantStockWithBatches(UUID productId, Long variantId,
                        List<WarehouseStockWithBatchesRequest> warehouseStocks);

        /**
         * Get a product by its ID
         * 
         * @param productId The product ID
         * @return The product DTO
         */
        ProductDTO getProductById(UUID productId);

        /**
         * Get a product by its ID for customers (without warehouse/batch data, with ProductDetail info)
         * 
         * @param productId The product ID
         * @return The customer product DTO
         */
        CustomerProductDTO getCustomerProductById(UUID productId);

        /**
         * Get a product by its slug for customers (without warehouse/batch data, with ProductDetail info)
         * 
         * @param slug The product slug
         * @return The customer product DTO
         */
        CustomerProductDTO getCustomerProductBySlug(String slug);

        /**
         * Get basic information of a product for update form
         * 
         * @param productId The product ID
         * @return The product basic info DTO
         */
        ProductBasicInfoDTO getProductBasicInfo(UUID productId);

        ProductBasicInfoDTO updateProductBasicInfo(UUID productId, ProductBasicInfoUpdateDTO updateDTO);

        /**
         * Get a product by its slug
         * 
         * @param slug The product slug
         * @return The product DTO
         */
        ProductDTO getProductBySlug(String slug);

        /**
         * Get all products with pagination
         * 
         * @param pageable Pagination information
         * @return Page of ManyProductsDto for card display
         */
        Page<ManyProductsDto> getAllProducts(Pageable pageable);

        /**
         * Get all products available for customers (active, displayToCustomers=true, with stock)
         * 
         * @param pageable Pagination information
         * @return Page of ManyProductsDto for customer display
         */
        Page<ManyProductsDto> getAllProductsForCustomers(Pageable pageable);

        /**
         * Get all products available for admins (all products regardless of stock or display status)
         * 
         * @param pageable Pagination information
         * @return Page of ManyProductsDto for admin display
         */
        Page<ManyProductsDto> getAllProductsForAdmins(Pageable pageable);

        /**
         * Get all products for a specific shop with pagination
         * 
         * @param shopId The shop ID to filter products by
         * @param pageable Pagination information
         * @return Page of ManyProductsDto for the shop
         */
        Page<ManyProductsDto> getAllProductsByShopId(UUID shopId, Pageable pageable);

        /**
         * Update an existing product
         * 
         * @param productId        The product ID
         * @param updateProductDTO The product update data
         * @return The updated product DTO
         */
        ProductDTO updateProduct(UUID productId, ProductUpdateDTO updateProductDTO);

        /**
         * Delete a product variant
         * 
         * @param productId The product ID
         * @param variantId The variant ID to delete
         * @return true if deletion was successful
         */
        boolean deleteProductVariant(UUID productId, Long variantId);

        /**
         * Delete a product with all its variants, images, and videos
         * Also removes the product from carts and wishlists
         * 
         * @param productId The product ID
         * @return true if deleted successfully
         * @throws ProductDeletionException if the product cannot be deleted due to
         *                                  pending orders
         */
        boolean deleteProduct(UUID productId);

        /**
         * Search products with comprehensive filtering
         * 
         * @param searchDTO The search criteria DTO
         * @return Page of ManyProductsDto for found products
         */
        Page<ManyProductsDto> searchProducts(ProductSearchDTO searchDTO);

        /**
         * Search products for customers (active, displayToCustomers=true, with stock)
         * 
         * @param searchDTO The search criteria DTO
         * @return Page of ManyProductsDto for customer search results
         */
        Page<ManyProductsDto> searchProductsForCustomers(ProductSearchDTO searchDTO);

        /**
         * Search products for admins (all products regardless of stock or display status)
         * 
         * @param searchDTO The search criteria DTO
         * @return Page of ManyProductsDto for admin search results
         */
        Page<ManyProductsDto> searchProductsForAdmins(ProductSearchDTO searchDTO);

        List<Map<String, Object>> getSearchSuggestions(String query);

        /**
         * Get warehouse stock information for a product with pagination
         * 
         * @param productId The product ID
         * @param pageable  Pagination information
         * @return Paginated warehouse stock information
         */
        WarehouseStockPageResponse getProductWarehouseStock(UUID productId, Pageable pageable);

        /**
         * Get warehouse stock information for a product variant with pagination
         * 
         * @param productId The product ID
         * @param variantId The variant ID
         * @param pageable  Pagination information
         * @return Paginated warehouse stock information for the variant
         */
        WarehouseStockPageResponse getVariantWarehouseStock(UUID productId, Long variantId, Pageable pageable);

        Page<ManyProductsDto> getSimilarProducts(SimilarProductsRequestDTO request);

        List<ManyProductsDto> getProductsByIds(List<String> productIds);

        /**
         * Get product pricing information
         * 
         * @param productId The product ID
         * @return Product pricing DTO
         */
        ProductPricingDTO getProductPricing(UUID productId);

        /**
         * Update product pricing information
         * 
         * @param productId The product ID
         * @param updateDTO The pricing update data
         * @return Updated product pricing DTO
         */
        ProductPricingDTO updateProductPricing(UUID productId, ProductPricingUpdateDTO updateDTO);

        /**
         * Get all variants for a product with pagination
         * 
         * @param productId The product ID
         * @param pageable  Pagination information
         * @return Page of ProductVariantDTO
         */
        Page<ProductVariantDTO> getProductVariants(UUID productId, Pageable pageable);

        List<ProductMediaDTO> getProductImages(UUID productId);

        List<ProductVideoDTO> getProductVideos(UUID productId);

        void deleteProductImage(UUID productId, Long imageId);

        void deleteProductVideo(UUID productId, Long videoId);

        void setPrimaryImage(UUID productId, Long imageId);

        List<ProductMediaDTO> uploadProductImages(UUID productId, List<MultipartFile> images);

        List<ProductVideoDTO> uploadProductVideos(UUID productId, List<MultipartFile> videos);

        ProductVariantDTO updateProductVariant(UUID productId, Long variantId, Map<String, Object> updates);

        void deleteVariantImage(UUID productId, Long variantId, Long imageId);

        void setPrimaryVariantImage(UUID productId, Long variantId, Long imageId);

        List<ProductVariantImageDTO> uploadVariantImages(UUID productId, Long variantId, List<MultipartFile> images);

        void removeVariantAttribute(UUID productId, Long variantId, Long attributeValueId);

        List<ProductVariantAttributeDTO> addVariantAttributes(UUID productId, Long variantId,
                        List<VariantAttributeRequest> attributeRequests);

        ProductVariantDTO createProductVariant(UUID productId, CreateVariantRequest request);

        ProductDetailsDTO getProductDetails(UUID productId);

        ProductDetailsDTO updateProductDetails(UUID productId, ProductDetailsUpdateDTO updateDTO);

        /**
         * Get featured products for customers
         * 
         * @param pageable Pagination information
         * @return Page of featured products available to customers
         */
        Page<ManyProductsDto> getFeaturedProductsForCustomers(Pageable pageable);

        /**
         * Get bestseller products for customers
         * 
         * @param pageable Pagination information
         * @return Page of bestseller products available to customers
         */
        Page<ManyProductsDto> getBestsellerProductsForCustomers(Pageable pageable);

        /**
         * Get new arrival products for customers
         * 
         * @param pageable Pagination information
         * @return Page of new arrival products available to customers
         */
        Page<ManyProductsDto> getNewArrivalProductsForCustomers(Pageable pageable);

        /**
         * Get products by category for customers
         * 
         * @param categoryId The category ID
         * @param pageable Pagination information
         * @return Page of products in the category available to customers
         */
        Page<ManyProductsDto> getProductsByCategoryForCustomers(Long categoryId, Pageable pageable);

        /**
         * Get products by brand for customers
         * 
         * @param brandId The brand ID
         * @param pageable Pagination information
         * @return Page of products by the brand available to customers
         */
        Page<ManyProductsDto> getProductsByBrandForCustomers(UUID brandId, Pageable pageable);

        /**
         * Check if a product is available for customers
         * 
         * @param productId The product ID
         * @return true if product is available for customers
         */
        boolean isProductAvailableForCustomers(UUID productId);

        /**
         * Get available variants for a product (for customers)
         * 
         * @param productId The product ID
         * @return List of available variants
         */
        List<ProductVariantDTO> getAvailableVariantsForCustomers(UUID productId);
}
