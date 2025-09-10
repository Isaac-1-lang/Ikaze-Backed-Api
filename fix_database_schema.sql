-- Manual SQL script to add missing columns to ready_for_delivery_groups table
-- Run this directly in your PostgreSQL database

-- Add the missing columns
ALTER TABLE ready_for_delivery_groups 
ADD COLUMN IF NOT EXISTS has_delivery_finished BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE ready_for_delivery_groups 
ADD COLUMN IF NOT EXISTS delivery_finished_at TIMESTAMP;

-- Verify the columns were added
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'ready_for_delivery_groups' 
AND column_name IN ('has_delivery_finished', 'delivery_finished_at');
