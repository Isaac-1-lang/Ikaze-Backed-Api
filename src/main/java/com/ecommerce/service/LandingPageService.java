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
     * Get popular categories with product counts
     * 
     * @param limit Maximum number of categories to return
     * @return List of popular categories
     */
    LandingPageDataDTO getPopularCategories(int limit);

    /**
     * Get popular brands with product counts
     * 
     * @param limit Maximum number of brands to return
     * @return List of popular brands
     */
    LandingPageDataDTO getPopularBrands(int limit);
}
