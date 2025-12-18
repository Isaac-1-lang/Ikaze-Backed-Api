-- Migration to remove signup points columns from reward_system table
-- Run this SQL script on your PostgreSQL database

-- Drop the signup_points_amount column first (it has no NOT NULL constraint, so it's safer)
ALTER TABLE reward_system DROP COLUMN IF EXISTS signup_points_amount;

-- Drop the is_signup_points_enabled column (this has the NOT NULL constraint causing the error)
ALTER TABLE reward_system DROP COLUMN IF EXISTS is_signup_points_enabled;

