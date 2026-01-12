-- Migration to remove order_id column from order_items table
-- OrderItem now links to ShopOrder via shop_order_id, not directly to Order
-- Run this SQL script on your PostgreSQL database

-- First, make the column nullable (if it's NOT NULL)
ALTER TABLE order_items ALTER COLUMN order_id DROP NOT NULL;

-- Drop any foreign key constraints on order_id
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS fk_order_items_order;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_order_id_fkey;

-- Drop any indexes on order_id
DROP INDEX IF EXISTS idx_order_items_order_id;

-- Drop the column
ALTER TABLE order_items DROP COLUMN IF EXISTS order_id;

