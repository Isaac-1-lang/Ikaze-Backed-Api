-- Add new address fields to warehouses table
-- Migration: V3__add_warehouse_address_fields.sql

-- Add new address-related columns
ALTER TABLE warehouses 
ADD COLUMN description TEXT,
ADD COLUMN address VARCHAR(255),
ADD COLUMN city VARCHAR(100),
ADD COLUMN state VARCHAR(100),
ADD COLUMN zip_code VARCHAR(20),
ADD COLUMN country VARCHAR(100),
ADD COLUMN street_number VARCHAR(50);

-- Update existing records to populate address fields from location
UPDATE warehouses 
SET address = location,
    city = 'Unknown',
    state = 'Unknown', 
    zip_code = '00000',
    country = 'Unknown'
WHERE location IS NOT NULL AND address IS NULL;

-- Make address fields NOT NULL after populating them
ALTER TABLE warehouses 
ALTER COLUMN address SET NOT NULL,
ALTER COLUMN city SET NOT NULL,
ALTER COLUMN state SET NOT NULL,
ALTER COLUMN zip_code SET NOT NULL,
ALTER COLUMN country SET NOT NULL;

-- Drop the old location column
ALTER TABLE warehouses DROP COLUMN IF EXISTS location;