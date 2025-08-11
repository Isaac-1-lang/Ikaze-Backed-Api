package com.ecommerce.service;

import com.ecommerce.dto.CreateProductDTO;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.dto.ProductUpdateDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    /**
     * Create a new product with variants, images, and videos
     * 
     * @param createProductDTO The product creation data
     * @return The created product DTO
     */
    ProductDTO createProduct(CreateProductDTO createProductDTO);

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
     * @return Page of product DTOs
     */
    Page<ProductDTO> getAllProducts(Pageable pageable);

    /**
     * Get products by category ID
     * 
     * @param categoryId The category ID
     * @param pageable   Pagination information
     * @return Page of product DTOs
     */
    Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable);

    /**
     * Update an existing product
     * 
     * @param productId        The product ID
     * @param updateProductDTO The product update data
     * @return The updated product DTO
     */
    ProductDTO updateProduct(UUID productId, ProductUpdateDTO updateProductDTO);

    /**
     * Delete a product
     * 
     * @param productId The product ID
     * @return true if deleted successfully
     */
    boolean deleteProduct(UUID productId);

    /**
     * Search products by keyword
     * 
     * @param keyword  The search keyword
     * @param pageable Pagination information
     * @return Page of product DTOs
     */
    Page<ProductDTO> searchProducts(String keyword, Pageable pageable);

    /**
     * Get featured products
     * 
     * @param limit Maximum number of products to return
     * @return List of product DTOs
     */
    List<ProductDTO> getFeaturedProducts(int limit);

    /**
     * Get bestseller products
     * 
     * @param limit Maximum number of products to return
     * @return List of product DTOs
     */
    List<ProductDTO> getBestsellerProducts(int limit);

    /**
     * Get new arrival products
     * 
     * @param limit Maximum number of products to return
     * @return List of product DTOs
     */
    List<ProductDTO> getNewArrivalProducts(int limit);

    /**
     * Get on sale products
     * 
     * @param limit Maximum number of products to return
     * @return List of product DTOs
     */
    List<ProductDTO> getOnSaleProducts(int limit);
}
