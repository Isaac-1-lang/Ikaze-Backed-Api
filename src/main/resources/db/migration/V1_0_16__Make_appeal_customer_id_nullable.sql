-- Make customer_id nullable in return_appeals table to support guest customers
ALTER TABLE return_appeals ALTER COLUMN customer_id DROP NOT NULL;

-- Add comment to explain the change
COMMENT ON COLUMN return_appeals.customer_id IS 'Customer ID - nullable for guest customers who submit appeals';
