-- Add pickup_token column to orders table
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS pickup_token VARCHAR(500) UNIQUE;

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_orders_pickup_token ON orders(pickup_token);

-- Update existing orders with pickup tokens (if any exist)
-- This will generate tokens for existing orders
UPDATE orders 
SET pickup_token = encode(
    (extract(epoch from created_at)::text || '-' || 
     replace(gen_random_uuid()::text, '-', '') || '-' || 
     order_id::text)::bytea, 
    'base64'
)
WHERE pickup_token IS NULL;
