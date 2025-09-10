-- Comprehensive Database Fix for Delivery Agent Portal
-- This script handles all scenarios and provides a long-lasting solution

-- Step 1: Drop the problematic columns if they exist (to start clean)
ALTER TABLE ready_for_delivery_groups 
DROP COLUMN IF EXISTS has_delivery_finished;

ALTER TABLE ready_for_delivery_groups 
DROP COLUMN IF EXISTS delivery_finished_at;

-- Step 2: Add the columns with proper defaults and constraints
-- First add as nullable
ALTER TABLE ready_for_delivery_groups 
ADD COLUMN has_delivery_finished BOOLEAN DEFAULT FALSE;

ALTER TABLE ready_for_delivery_groups 
ADD COLUMN delivery_finished_at TIMESTAMP;

-- Step 3: Update all existing rows to have proper default values
UPDATE ready_for_delivery_groups 
SET has_delivery_finished = FALSE 
WHERE has_delivery_finished IS NULL;

-- Step 4: Now make the column NOT NULL (since all rows have values)
ALTER TABLE ready_for_delivery_groups 
ALTER COLUMN has_delivery_finished SET NOT NULL;

-- Step 5: Set the default value for future inserts
ALTER TABLE ready_for_delivery_groups 
ALTER COLUMN has_delivery_finished SET DEFAULT FALSE;

-- Step 6: Verify the changes
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'ready_for_delivery_groups' 
AND column_name IN ('has_delivery_finished', 'delivery_finished_at')
ORDER BY ordinal_position;

-- Step 7: Check existing data
SELECT 
    delivery_group_id,
    delivery_group_name,
    has_delivery_finished,
    delivery_finished_at,
    has_delivery_started,
    delivery_started_at
FROM ready_for_delivery_groups 
LIMIT 5;
