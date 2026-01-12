-- ============================================================================
-- MULTI-VENDOR MIGRATION SQL SCRIPT
-- ============================================================================
-- This script migrates your database from single-vendor to multi-vendor
-- architecture where orders are split into shop_orders
--
-- IMPORTANT: 
-- 1. BACKUP YOUR DATABASE BEFORE RUNNING THIS!
-- 2. Run this script in your PostgreSQL database
-- 3. You can run it in pgAdmin, DBeaver, or psql command line
-- ============================================================================

-- ============================================================================
-- STEP 1: Remove order_id from order_items table
-- ============================================================================
-- OrderItem now links to ShopOrder via shop_order_id, not directly to Order

-- Make the column nullable first (if it's NOT NULL)
ALTER TABLE order_items 
    ALTER COLUMN order_id DROP NOT NULL;

-- Drop any foreign key constraints on order_id
ALTER TABLE order_items 
    DROP CONSTRAINT IF EXISTS fk_order_items_order;

ALTER TABLE order_items 
    DROP CONSTRAINT IF EXISTS order_items_order_id_fkey;

-- Drop any indexes on order_id
DROP INDEX IF EXISTS idx_order_items_order_id;

-- Finally, drop the column
ALTER TABLE order_items 
    DROP COLUMN IF EXISTS order_id;

-- ============================================================================
-- STEP 2: Remove pickup_token and pickup_token_used from orders table
-- ============================================================================
-- These fields have been moved to shop_orders table as they are shop-specific

-- Make columns nullable first (if they're NOT NULL)
ALTER TABLE orders 
    ALTER COLUMN pickup_token DROP NOT NULL;

ALTER TABLE orders 
    ALTER COLUMN pickup_token_used DROP NOT NULL;

-- Drop the unique constraint on pickup_token if it exists
ALTER TABLE orders 
    DROP CONSTRAINT IF EXISTS orders_pickup_token_key;

-- Drop the columns
ALTER TABLE orders 
    DROP COLUMN IF EXISTS pickup_token;

ALTER TABLE orders 
    DROP COLUMN IF EXISTS pickup_token_used;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- Run these queries to verify the migration was successful

-- Check order_items structure (should NOT have order_id, should have shop_order_id)
-- Uncomment the line below to see the table structure:
-- \d order_items;

-- Check orders structure (should NOT have pickup_token columns)
-- Uncomment the line below to see the table structure:
-- \d orders;

-- Check shop_orders structure (should have pickup_token and pickup_token_used)
-- Uncomment the line below to see the table structure:
-- \d shop_orders;

-- Verify data integrity - should return 0 for both queries
SELECT 
    'Orphaned order items (should be 0)' AS check_name,
    COUNT(*) AS count
FROM order_items 
WHERE shop_order_id IS NULL;

SELECT 
    'Orphaned shop orders (should be 0)' AS check_name,
    COUNT(*) AS count
FROM shop_orders 
WHERE order_id IS NULL;

-- ============================================================================
-- MIGRATION COMPLETE!
-- ============================================================================
-- After running this script:
-- 1. Restart your Spring Boot application
-- 2. Test the checkout flow
-- 3. Verify orders are created correctly with shop_orders
--
-- If you see any errors, check:
-- - Application logs for specific error messages
-- - Database connection settings in application.properties
-- - That all foreign key relationships are correct
-- ============================================================================

