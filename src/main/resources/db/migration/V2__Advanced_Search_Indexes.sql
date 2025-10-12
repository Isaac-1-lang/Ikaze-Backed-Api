-- =====================================================
-- Advanced Search and Performance Indexes
-- =====================================================
-- This script creates advanced indexes for complex search scenarios,
-- full-text search optimization, and performance tuning

-- =====================================================
-- FULL-TEXT SEARCH OPTIMIZATION
-- =====================================================

-- Create custom text search configuration for products
CREATE TEXT SEARCH CONFIGURATION IF NOT EXISTS product_search (COPY = english);

-- Advanced full-text search index combining multiple fields
CREATE INDEX IF NOT EXISTS idx_products_fulltext_search ON products 
USING gin(
    to_tsvector('product_search', 
        COALESCE(product_name, '') || ' ' || 
        COALESCE(short_description, '') || ' ' || 
        COALESCE(sku, '') || ' ' ||
        COALESCE(model, '')
    )
);

-- Product details full-text search
CREATE INDEX IF NOT EXISTS idx_product_details_fulltext ON product_details 
USING gin(
    to_tsvector('product_search',
        COALESCE(description, '') || ' ' ||
        COALESCE(meta_keywords, '') || ' ' ||
        COALESCE(search_keywords, '') || ' ' ||
        COALESCE(material, '')
    )
);

-- =====================================================
-- ADVANCED FILTERING INDEXES
-- =====================================================

-- Price range with category filtering (most common e-commerce query)
CREATE INDEX IF NOT EXISTS idx_products_category_price_range ON products (
    category_id, 
    price, 
    is_active, 
    display_to_customers
) WHERE is_active = true AND display_to_customers = true;

-- Brand with price range filtering
CREATE INDEX IF NOT EXISTS idx_products_brand_price_range ON products (
    brand_id, 
    price, 
    is_active, 
    display_to_customers
) WHERE is_active = true AND display_to_customers = true;

-- Multi-attribute filtering index
CREATE INDEX IF NOT EXISTS idx_products_multi_filter ON products (
    category_id,
    brand_id,
    is_featured,
    is_on_sale,
    price,
    created_at DESC
) WHERE is_active = true AND display_to_customers = true;

-- =====================================================
-- INVENTORY AND STOCK INDEXES
-- =====================================================

-- Stock availability with location
CREATE INDEX IF NOT EXISTS idx_stocks_availability_location ON stocks (
    warehouse_id,
    quantity,
    low_stock_threshold
) WHERE quantity > 0;

-- Product stock summary index
CREATE INDEX IF NOT EXISTS idx_stocks_product_summary ON stocks (
    product_id,
    warehouse_id,
    quantity
) WHERE product_id IS NOT NULL AND quantity >= 0;

-- Variant stock summary index  
CREATE INDEX IF NOT EXISTS idx_stocks_variant_summary ON stocks (
    variant_id,
    warehouse_id,
    quantity
) WHERE variant_id IS NOT NULL AND quantity >= 0;

-- Low stock alerts index
CREATE INDEX IF NOT EXISTS idx_stocks_alerts ON stocks (
    warehouse_id,
    quantity,
    low_stock_threshold,
    updated_at DESC
) WHERE quantity <= low_stock_threshold AND quantity >= 0;

-- =====================================================
-- PRODUCT VARIANT OPTIMIZATION
-- =====================================================

-- Variant pricing and availability
CREATE INDEX IF NOT EXISTS idx_variants_pricing ON product_variants (
    product_id,
    price,
    is_active,
    sort_order
) WHERE is_active = true;

-- Variant attributes for filtering
CREATE INDEX IF NOT EXISTS idx_variants_attributes ON product_variants (
    product_id,
    variant_sku,
    is_active
);

-- =====================================================
-- REVIEW AND RATING INDEXES
-- =====================================================

-- Product rating aggregation
CREATE INDEX IF NOT EXISTS idx_reviews_rating_aggregation ON reviews (
    product_id,
    rating,
    is_verified,
    created_at DESC
) WHERE is_approved = true;

-- Recent reviews index
CREATE INDEX IF NOT EXISTS idx_reviews_recent ON reviews (
    created_at DESC,
    is_approved,
    rating
) WHERE is_approved = true;

-- User review history
CREATE INDEX IF NOT EXISTS idx_reviews_user_history ON reviews (
    user_id,
    created_at DESC,
    rating
) WHERE user_id IS NOT NULL AND is_approved = true;

-- =====================================================
-- IMAGE AND MEDIA INDEXES
-- =====================================================

-- Primary product images
CREATE INDEX IF NOT EXISTS idx_product_images_primary ON product_images (
    product_id,
    is_primary,
    sort_order
) WHERE is_primary = true;

-- All product images ordered
CREATE INDEX IF NOT EXISTS idx_product_images_ordered ON product_images (
    product_id,
    sort_order,
    created_at
);

-- Product videos
CREATE INDEX IF NOT EXISTS idx_product_videos_ordered ON product_videos (
    product_id,
    sort_order,
    is_primary
);

-- =====================================================
-- CATEGORY AND BRAND HIERARCHY
-- =====================================================

-- Category hierarchy navigation
CREATE INDEX IF NOT EXISTS idx_categories_hierarchy ON categories (
    parent_id,
    sort_order,
    is_active
) WHERE is_active = true;

-- Category with product count (for navigation)
CREATE INDEX IF NOT EXISTS idx_categories_with_products ON categories (
    category_id,
    is_active,
    created_at
) WHERE is_active = true;

-- Brand popularity (based on product count)
CREATE INDEX IF NOT EXISTS idx_brands_popularity ON brands (
    is_active,
    created_at DESC
) WHERE is_active = true;

-- =====================================================
-- SALES AND DISCOUNT INDEXES
-- =====================================================

-- Active discounts
CREATE INDEX IF NOT EXISTS idx_products_active_discounts ON products (
    discount_id,
    price,
    created_at DESC
) WHERE discount_id IS NOT NULL AND is_active = true;

-- Sale products with percentage
CREATE INDEX IF NOT EXISTS idx_products_sale_percentage ON products (
    is_on_sale,
    sale_percentage DESC,
    price,
    created_at DESC
) WHERE is_on_sale = true AND sale_percentage > 0;

-- =====================================================
-- TEMPORAL INDEXES FOR ANALYTICS
-- =====================================================

-- Products by creation date (for trending/new products)
CREATE INDEX IF NOT EXISTS idx_products_trending ON products (
    created_at DESC,
    is_active,
    display_to_customers
) WHERE is_active = true AND display_to_customers = true;

-- Products by update date (for recently modified)
CREATE INDEX IF NOT EXISTS idx_products_recently_updated ON products (
    updated_at DESC,
    is_active
) WHERE is_active = true;

-- Stock movements by date
CREATE INDEX IF NOT EXISTS idx_stocks_temporal ON stocks (
    updated_at DESC,
    warehouse_id,
    quantity
);

-- =====================================================
-- SEARCH PERFORMANCE INDEXES
-- =====================================================

-- Trigram indexes for fuzzy search (requires pg_trgm extension)
-- Uncomment if pg_trgm extension is available
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- CREATE INDEX IF NOT EXISTS idx_products_name_trigram ON products USING gin(product_name gin_trgm_ops);
-- CREATE INDEX IF NOT EXISTS idx_products_sku_trigram ON products USING gin(sku gin_trgm_ops);

-- =====================================================
-- COVERING INDEXES FOR COMMON QUERIES
-- =====================================================

-- Product list page covering index
CREATE INDEX IF NOT EXISTS idx_products_list_page_covering ON products (
    category_id,
    is_active,
    display_to_customers,
    created_at DESC
) INCLUDE (
    product_id,
    product_name,
    price,
    compare_at_price,
    is_on_sale,
    sale_percentage,
    slug
) WHERE is_active = true AND display_to_customers = true;

-- Product search results covering index
CREATE INDEX IF NOT EXISTS idx_products_search_covering ON products (
    is_active,
    display_to_customers
) INCLUDE (
    product_id,
    product_name,
    short_description,
    price,
    compare_at_price,
    is_on_sale,
    sale_percentage,
    slug,
    category_id,
    brand_id
) WHERE is_active = true AND display_to_customers = true;

-- =====================================================
-- PARTIAL INDEXES FOR SPECIFIC USE CASES
-- =====================================================

-- Only index products that are actually visible to customers
CREATE INDEX IF NOT EXISTS idx_products_customer_visible ON products (
    product_name,
    category_id,
    brand_id,
    price,
    created_at DESC
) WHERE is_active = true 
  AND display_to_customers = true 
  AND status = 'PUBLISHED';

-- Only index products with variants
CREATE INDEX IF NOT EXISTS idx_products_with_variants ON products (
    product_id,
    category_id,
    price
) WHERE EXISTS (
    SELECT 1 FROM product_variants pv 
    WHERE pv.product_id = products.product_id 
    AND pv.is_active = true
);

-- Only index products with stock
CREATE INDEX IF NOT EXISTS idx_products_in_stock ON products (
    product_id,
    category_id,
    brand_id,
    price,
    created_at DESC
) WHERE EXISTS (
    SELECT 1 FROM stocks s 
    WHERE s.product_id = products.product_id 
    AND s.quantity > 0
);

-- =====================================================
-- EXPRESSION INDEXES
-- =====================================================

-- Calculated discount percentage
CREATE INDEX IF NOT EXISTS idx_products_discount_percentage ON products (
    (CASE 
        WHEN compare_at_price IS NOT NULL AND compare_at_price > price 
        THEN ROUND(((compare_at_price - price) / compare_at_price * 100)::numeric, 2)
        ELSE 0 
    END) DESC
) WHERE compare_at_price IS NOT NULL AND compare_at_price > price;

-- Product name length for sorting
CREATE INDEX IF NOT EXISTS idx_products_name_length ON products (length(product_name));

-- =====================================================
-- MAINTENANCE AND CLEANUP
-- =====================================================

-- Update table statistics
ANALYZE products;
ANALYZE product_details;
ANALYZE product_variants;
ANALYZE stocks;
ANALYZE product_images;
ANALYZE product_videos;
ANALYZE reviews;
ANALYZE categories;
ANALYZE brands;

-- Vacuum tables to reclaim space
VACUUM ANALYZE products;
VACUUM ANALYZE product_details;
VACUUM ANALYZE product_variants;
VACUUM ANALYZE stocks;
