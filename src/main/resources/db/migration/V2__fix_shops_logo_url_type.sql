-- Migration to fix column types in shops table
-- This fixes the PostgreSQL error: function lower(bytea) does not exist

-- Fix all text columns that might be stored as bytea
DO $$ 
BEGIN
    -- Fix shop_name if it's bytea
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'shops' AND column_name = 'shop_name' AND data_type = 'bytea'
    ) THEN
        ALTER TABLE shops ALTER COLUMN shop_name TYPE TEXT USING convert_from(shop_name, 'UTF8');
    END IF;

    -- Fix description if it's bytea
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'shops' AND column_name = 'description' AND data_type = 'bytea'
    ) THEN
        ALTER TABLE shops ALTER COLUMN description TYPE TEXT USING convert_from(description, 'UTF8');
    END IF;

    -- Fix address if it's bytea
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'shops' AND column_name = 'address' AND data_type = 'bytea'
    ) THEN
        ALTER TABLE shops ALTER COLUMN address TYPE TEXT USING convert_from(address, 'UTF8');
    END IF;

    -- Fix logo_url if it's bytea
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'shops' AND column_name = 'logo_url' AND data_type = 'bytea'
    ) THEN
        ALTER TABLE shops ALTER COLUMN logo_url TYPE TEXT USING convert_from(logo_url, 'UTF8');
    END IF;

    -- Fix category if it's bytea
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'shops' AND column_name = 'category' AND data_type = 'bytea'
    ) THEN
        ALTER TABLE shops ALTER COLUMN category TYPE TEXT USING convert_from(category, 'UTF8');
    END IF;

    -- Fix contact_email if it's bytea
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'shops' AND column_name = 'contact_email' AND data_type = 'bytea'
    ) THEN
        ALTER TABLE shops ALTER COLUMN contact_email TYPE TEXT USING convert_from(contact_email, 'UTF8');
    END IF;

    -- Fix contact_phone if it's bytea
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'shops' AND column_name = 'contact_phone' AND data_type = 'bytea'
    ) THEN
        ALTER TABLE shops ALTER COLUMN contact_phone TYPE TEXT USING convert_from(contact_phone, 'UTF8');
    END IF;
END $$;

-- Also fix users table columns that are used in the search query
DO $$ 
BEGIN
    -- Fix first_name if it's bytea
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'first_name' AND data_type = 'bytea'
    ) THEN
        ALTER TABLE users ALTER COLUMN first_name TYPE VARCHAR(50) USING convert_from(first_name, 'UTF8');
    END IF;

    -- Fix last_name if it's bytea
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'last_name' AND data_type = 'bytea'
    ) THEN
        ALTER TABLE users ALTER COLUMN last_name TYPE VARCHAR(50) USING convert_from(last_name, 'UTF8');
    END IF;
END $$;

