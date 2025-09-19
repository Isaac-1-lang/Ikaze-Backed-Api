package com.ecommerce.service;

import com.ecommerce.dto.ManyProductsDto;
import com.ecommerce.dto.ProductDTO;
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
         * @return Map containing product ID and status information
         */
        Map<String, Object> createEmptyProduct(String name);

        /**
         * Check if product has variants
         * 
         * @param productId The product ID
         * @return true if product has variants, false otherwise
         */
        boolean productHasVariants(UUID productId);

        /**
         * Assign stock to product (only when product has no variants)
         * 
         * @param productId       The product ID
         * @param warehouseStocks List of warehouse stock assignments
         * @return Map containing success status and message
         */
        Map<String, Object> assignProductStock(UUID productId, List<WarehouseStockRequest> warehouseStocks);

        /**
         * Get a product by its ID
         * 
         * @param productId The product ID
         * @return The product DTO
         */
        ProductDTO getProductById(UUID productId);

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
}
