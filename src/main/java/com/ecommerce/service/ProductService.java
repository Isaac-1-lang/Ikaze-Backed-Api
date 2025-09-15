package com.ecommerce.service;

import com.ecommerce.dto.CreateProductDTO;
import com.ecommerce.dto.ManyProductsDto;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.dto.ProductSearchDTO;
import com.ecommerce.dto.ProductUpdateDTO;
import com.ecommerce.dto.SimilarProductsRequestDTO;
import com.ecommerce.dto.WarehouseStockPageResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProductService {

    /**
     * Create a new product with variants, images, and videos
     * 
     * @param createProductDTO The product creation data
     * @param productImages    List of product image files
     * @param productVideos    List of product video files
     * @return The created product DTO
     */
    ProductDTO createProduct(CreateProductDTO createProductDTO, List<MultipartFile> productImages,
            List<MultipartFile> productVideos);

    /**
     * Get a product by its ID
     * 
     * @param productId The product ID
     * @return The product DTO
     */
    ProductDTO getProductById(UUID productId);

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
}
