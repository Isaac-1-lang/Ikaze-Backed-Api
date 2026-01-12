-- Migration to remove pickup_token and pickup_token_used columns from orders table
-- These fields have been moved to shop_orders table as they are shop-specific
-- Run this SQL script on your PostgreSQL database

-- First, make the columns nullable (if they're NOT NULL)
ALTER TABLE orders ALTER COLUMN pickup_token DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN pickup_token_used DROP NOT NULL;

-- Drop the unique constraint on pickup_token if it exists
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_pickup_token_key;

-- Drop the columns
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token_used;

