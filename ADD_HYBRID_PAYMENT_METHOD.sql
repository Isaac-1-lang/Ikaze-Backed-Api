-- Migration to add HYBRID payment method to order_transactions table
-- This fixes the constraint violation error on the remote instance

-- Step 1: Drop the existing check constraint
ALTER TABLE order_transactions DROP CONSTRAINT IF EXISTS order_transactions_payment_method_check;

-- Step 2: Add the new check constraint that includes HYBRID
ALTER TABLE order_transactions 
ADD CONSTRAINT order_transactions_payment_method_check 
CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'POINTS', 'HYBRID'));

-- Verify the constraint
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'order_transactions'::regclass 
AND conname = 'order_transactions_payment_method_check';
