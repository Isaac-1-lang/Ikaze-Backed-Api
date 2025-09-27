package com.ecommerce.service;

import com.ecommerce.dto.LandingPageDataDTO;

public interface LandingPageService {

    /**
     * Get all data needed for the landing page
     * This includes top-selling products, new products, discounted products,
     * popular categories, and popular brands
     * 
     * @return LandingPageDataDTO containing all landing page data
     */
    LandingPageDataDTO getLandingPageData();

    /**
     * Get top-selling products based on sales and ratings
     * 
     * @param limit Maximum number of products to return
     * @return List of top-selling products
     */
    LandingPageDataDTO getTopSellingProducts(int limit);

    /**
     * Get newest products
     * 
     * @param limit Maximum number of products to return
     * @return List of newest products
     */
    LandingPageDataDTO getNewProducts(int limit);

    /**
     * Get discounted products
     * 
     * @param limit Maximum number of products to return
     * @return List of discounted products
     */
    LandingPageDataDTO getDiscountedProducts(int limit);

    /**
     * Get featured categories with their products
     * 
     * @param categoryLimit Maximum number of categories to return
     * @param productLimit Maximum number of products per category
     * @return List of featured categories with products
     */
    LandingPageDataDTO getFeaturedCategories(int categoryLimit, int productLimit);

    /**
     * Get featured brands with their products
     * 
     * @param brandLimit Maximum number of brands to return
     * @param productLimit Maximum number of products per brand
     * @return List of featured brands with products
     */
    LandingPageDataDTO getFeaturedBrands(int brandLimit, int productLimit);
}
