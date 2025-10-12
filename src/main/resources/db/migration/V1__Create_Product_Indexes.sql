-- =====================================================
-- Product-Related Database Indexes for Performance
-- =====================================================
-- This script creates comprehensive indexes for product-related entities
-- to optimize search, filtering, and query performance

-- =====================================================
-- PRODUCTS TABLE INDEXES
-- =====================================================

-- Primary search and filtering indexes
CREATE INDEX IF NOT EXISTS idx_products_name_search ON products USING gin(to_tsvector('english', product_name));
CREATE INDEX IF NOT EXISTS idx_products_name_lower ON products (LOWER(product_name));
CREATE INDEX IF NOT EXISTS idx_products_sku ON products (sku);
CREATE INDEX IF NOT EXISTS idx_products_slug ON products (slug);
CREATE INDEX IF NOT EXISTS idx_products_barcode ON products (barcode) WHERE barcode IS NOT NULL;

-- Category and brand filtering
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products (category_id);
CREATE INDEX IF NOT EXISTS idx_products_brand_id ON products (brand_id);
CREATE INDEX IF NOT EXISTS idx_products_category_brand ON products (category_id, brand_id);

-- Price range filtering
CREATE INDEX IF NOT EXISTS idx_products_price ON products (price);
CREATE INDEX IF NOT EXISTS idx_products_price_range ON products (price, compare_at_price);
CREATE INDEX IF NOT EXISTS idx_products_discounted_price ON products (price, is_on_sale, sale_percentage);

-- Status and visibility filtering
CREATE INDEX IF NOT EXISTS idx_products_active ON products (is_active);
CREATE INDEX IF NOT EXISTS idx_products_status ON products (status);
CREATE INDEX IF NOT EXISTS idx_products_display_customers ON products (display_to_customers);
CREATE INDEX IF NOT EXISTS idx_products_active_display ON products (is_active, display_to_customers);

-- Product flags for filtering
CREATE INDEX IF NOT EXISTS idx_products_featured ON products (is_featured) WHERE is_featured = true;
CREATE INDEX IF NOT EXISTS idx_products_bestseller ON products (is_bestseller) WHERE is_bestseller = true;
CREATE INDEX IF NOT EXISTS idx_products_new_arrival ON products (is_new_arrival) WHERE is_new_arrival = true;
CREATE INDEX IF NOT EXISTS idx_products_on_sale ON products (is_on_sale) WHERE is_on_sale = true;

-- Timestamp indexes for sorting and filtering
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_products_updated_at ON products (updated_at DESC);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_products_active_category_price ON products (is_active, category_id, price);
CREATE INDEX IF NOT EXISTS idx_products_display_status_created ON products (display_to_customers, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_products_category_active_featured ON products (category_id, is_active, is_featured);

-- Discount filtering
CREATE INDEX IF NOT EXISTS idx_products_discount_id ON products (discount_id) WHERE discount_id IS NOT NULL;

-- =====================================================
-- PRODUCT_DETAILS TABLE INDEXES
-- =====================================================

-- Full-text search on descriptions and keywords
CREATE INDEX IF NOT EXISTS idx_product_details_description_search ON product_details USING gin(to_tsvector('english', description));
CREATE INDEX IF NOT EXISTS idx_product_details_search_keywords ON product_details USING gin(to_tsvector('english', search_keywords));
CREATE INDEX IF NOT EXISTS idx_product_details_meta_keywords ON product_details USING gin(to_tsvector('english', meta_keywords));

-- Product relationship
CREATE INDEX IF NOT EXISTS idx_product_details_product_id ON product_details (product_id);

-- Material and dimensions filtering
CREATE INDEX IF NOT EXISTS idx_product_details_material ON product_details (material) WHERE material IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_product_details_weight ON product_details (weight_kg) WHERE weight_kg IS NOT NULL;

-- =====================================================
-- PRODUCT_VARIANTS TABLE INDEXES
-- =====================================================

-- Product relationship and SKU
CREATE INDEX IF NOT EXISTS idx_product_variants_product_id ON product_variants (product_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_sku ON product_variants (variant_sku);
CREATE INDEX IF NOT EXISTS idx_product_variants_barcode ON product_variants (variant_barcode) WHERE variant_barcode IS NOT NULL;

-- Price filtering for variants
CREATE INDEX IF NOT EXISTS idx_product_variants_price ON product_variants (price);
CREATE INDEX IF NOT EXISTS idx_product_variants_active ON product_variants (is_active);
CREATE INDEX IF NOT EXISTS idx_product_variants_product_active ON product_variants (product_id, is_active);

-- Sorting
CREATE INDEX IF NOT EXISTS idx_product_variants_sort_order ON product_variants (product_id, sort_order);

-- =====================================================
-- STOCKS TABLE INDEXES
-- =====================================================

-- Primary relationships
CREATE INDEX IF NOT EXISTS idx_stocks_product_id ON stocks (product_id) WHERE product_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stocks_variant_id ON stocks (variant_id) WHERE variant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stocks_warehouse_id ON stocks (warehouse_id);

-- Composite indexes for stock queries
CREATE INDEX IF NOT EXISTS idx_stocks_warehouse_product ON stocks (warehouse_id, product_id) WHERE product_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stocks_warehouse_variant ON stocks (warehouse_id, variant_id) WHERE variant_id IS NOT NULL;

-- Quantity and threshold filtering
CREATE INDEX IF NOT EXISTS idx_stocks_quantity ON stocks (quantity);
CREATE INDEX IF NOT EXISTS idx_stocks_low_threshold ON stocks (quantity, low_stock_threshold);

-- =====================================================
-- PRODUCT_IMAGES TABLE INDEXES
-- =====================================================

-- Product relationship and primary image
CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images (product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_primary ON product_images (product_id, is_primary);
CREATE INDEX IF NOT EXISTS idx_product_images_sort_order ON product_images (product_id, sort_order);

-- =====================================================
-- PRODUCT_VIDEOS TABLE INDEXES
-- =====================================================

-- Product relationship
CREATE INDEX IF NOT EXISTS idx_product_videos_product_id ON product_videos (product_id);
CREATE INDEX IF NOT EXISTS idx_product_videos_sort_order ON product_videos (product_id, sort_order);

-- =====================================================
-- REVIEWS TABLE INDEXES
-- =====================================================

-- Product relationship and rating
CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews (product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_rating ON reviews (rating);
CREATE INDEX IF NOT EXISTS idx_reviews_product_rating ON reviews (product_id, rating);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews (created_at DESC);

-- User reviews
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews (user_id) WHERE user_id IS NOT NULL;

-- =====================================================
-- CATEGORIES TABLE INDEXES
-- =====================================================

-- Name and slug for category filtering
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories (name);
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories (slug);
CREATE INDEX IF NOT EXISTS idx_categories_active ON categories (is_active);

-- Hierarchy indexes
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories (parent_id) WHERE parent_id IS NOT NULL;

-- =====================================================
-- BRANDS TABLE INDEXES
-- =====================================================

-- Name and slug for brand filtering
CREATE INDEX IF NOT EXISTS idx_brands_name ON brands (name);
CREATE INDEX IF NOT EXISTS idx_brands_slug ON brands (slug);
CREATE INDEX IF NOT EXISTS idx_brands_active ON brands (is_active);

-- =====================================================
-- COMPOSITE SEARCH INDEXES
-- =====================================================

-- Multi-table search performance
CREATE INDEX IF NOT EXISTS idx_products_search_composite ON products (
    is_active, 
    display_to_customers, 
    category_id, 
    brand_id, 
    price, 
    created_at DESC
) WHERE is_active = true AND display_to_customers = true;

-- Featured products composite
CREATE INDEX IF NOT EXISTS idx_products_featured_composite ON products (
    is_active,
    is_featured,
    category_id,
    created_at DESC
) WHERE is_active = true AND is_featured = true;

-- Sale products composite
CREATE INDEX IF NOT EXISTS idx_products_sale_composite ON products (
    is_active,
    is_on_sale,
    sale_percentage,
    price,
    created_at DESC
) WHERE is_active = true AND is_on_sale = true;

-- =====================================================
-- PERFORMANCE OPTIMIZATION INDEXES
-- =====================================================

-- Covering indexes for common SELECT queries
CREATE INDEX IF NOT EXISTS idx_products_list_covering ON products (
    product_id,
    product_name,
    price,
    compare_at_price,
    is_on_sale,
    sale_percentage,
    is_active,
    created_at
) WHERE is_active = true AND display_to_customers = true;

-- Stock availability covering index
CREATE INDEX IF NOT EXISTS idx_stocks_availability_covering ON stocks (
    product_id,
    variant_id,
    warehouse_id,
    quantity,
    low_stock_threshold
);

-- =====================================================
-- PARTIAL INDEXES FOR SPECIFIC CONDITIONS
-- =====================================================

-- Only index active products for customer-facing queries
CREATE INDEX IF NOT EXISTS idx_products_customer_facing ON products (
    category_id,
    brand_id,
    price,
    created_at DESC
) WHERE is_active = true AND display_to_customers = true;

-- Only index products with discounts
CREATE INDEX IF NOT EXISTS idx_products_with_discounts ON products (
    discount_id,
    price,
    created_at DESC
) WHERE discount_id IS NOT NULL;

-- Only index low stock items
CREATE INDEX IF NOT EXISTS idx_stocks_low_stock ON stocks (
    product_id,
    variant_id,
    warehouse_id,
    quantity
) WHERE quantity <= low_stock_threshold AND quantity > 0;

-- Only index out of stock items
CREATE INDEX IF NOT EXISTS idx_stocks_out_of_stock ON stocks (
    product_id,
    variant_id,
    warehouse_id
) WHERE quantity <= 0;

-- =====================================================
-- ANALYZE TABLES FOR STATISTICS
-- =====================================================

-- Update table statistics for query planner
ANALYZE products;
ANALYZE product_details;
ANALYZE product_variants;
ANALYZE stocks;
ANALYZE product_images;
ANALYZE product_videos;
ANALYZE reviews;
ANALYZE categories;
ANALYZE brands;
