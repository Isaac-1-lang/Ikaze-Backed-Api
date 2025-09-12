-- Add pickup_token_used column to orders table
ALTER TABLE orders ADD COLUMN pickup_token_used BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for better performance on pickup token queries
CREATE INDEX idx_orders_pickup_token_used ON orders(pickup_token_used);

-- Update existing orders to have pickup_token_used = false
UPDATE orders SET pickup_token_used = FALSE WHERE pickup_token_used IS NULL;
