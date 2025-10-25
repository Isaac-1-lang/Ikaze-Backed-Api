-- Cleanup Script for Duplicate Payment Activity Logs
-- This script removes duplicate PAYMENT_COMPLETED activities, keeping only the first one

-- Step 1: Identify duplicates (for review)
SELECT 
    order_id,
    activity_type,
    COUNT(*) as duplicate_count,
    MIN(id) as keep_id,
    GROUP_CONCAT(id ORDER BY timestamp) as all_ids
FROM order_activity_logs
WHERE activity_type = 'PAYMENT_COMPLETED'
GROUP BY order_id, activity_type
HAVING COUNT(*) > 1;

-- Step 2: Delete duplicates, keeping only the first occurrence (oldest timestamp)
-- WARNING: This will permanently delete duplicate records. Review Step 1 results first!

DELETE FROM order_activity_logs
WHERE id IN (
    SELECT id FROM (
        SELECT 
            id,
            ROW_NUMBER() OVER (
                PARTITION BY order_id, activity_type 
                ORDER BY timestamp ASC, id ASC
            ) as rn
        FROM order_activity_logs
        WHERE activity_type = 'PAYMENT_COMPLETED'
    ) ranked
    WHERE rn > 1
);

-- Step 3: Verify cleanup (should return 0 rows if successful)
SELECT 
    order_id,
    activity_type,
    COUNT(*) as count
FROM order_activity_logs
WHERE activity_type = 'PAYMENT_COMPLETED'
GROUP BY order_id, activity_type
HAVING COUNT(*) > 1;

-- Step 4: Check remaining PAYMENT_COMPLETED logs (should be 1 per order)
SELECT 
    order_id,
    activity_type,
    title,
    timestamp,
    created_at
FROM order_activity_logs
WHERE activity_type = 'PAYMENT_COMPLETED'
ORDER BY order_id, timestamp;
