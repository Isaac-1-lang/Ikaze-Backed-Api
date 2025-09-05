-- Update order_items table to support both variant and non-variant products
-- Make variant_id nullable and add product_id column

ALTER TABLE order_items ALTER COLUMN variant_id DROP NOT NULL;
ALTER TABLE order_items ADD COLUMN product_id UUID;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (product_id);
