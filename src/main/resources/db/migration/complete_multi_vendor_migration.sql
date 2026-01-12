-- ============================================================================
-- COMPLETE MULTI-VENDOR MIGRATION SCRIPT
-- ============================================================================
-- This script migrates the database schema from single-vendor to multi-vendor
-- architecture where orders are split into shop_orders
--
-- IMPORTANT: Backup your database before running this script!
-- ============================================================================

-- Step 1: Remove order_id from order_items (items now link to shop_orders)
-- ----------------------------------------------------------------------------
ALTER TABLE order_items ALTER COLUMN order_id DROP NOT NULL;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS fk_order_items_order;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_order_id_fkey;
DROP INDEX IF EXISTS idx_order_items_order_id;
ALTER TABLE order_items DROP COLUMN IF EXISTS order_id;

-- Step 2: Remove pickup tokens from orders (now on shop_orders)
-- ----------------------------------------------------------------------------
ALTER TABLE orders ALTER COLUMN pickup_token DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN pickup_token_used DROP NOT NULL;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_pickup_token_key;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token_used;

-- ============================================================================
-- VERIFICATION QUERIES (uncomment to check)
-- ============================================================================
-- Check order_items structure:
-- \d order_items;

-- Check orders structure:
-- \d orders;

-- Check shop_orders structure:
-- \d shop_orders;

-- Verify data integrity:
-- SELECT COUNT(*) FROM order_items WHERE shop_order_id IS NULL;
-- SELECT COUNT(*) FROM shop_orders WHERE order_id IS NULL;

