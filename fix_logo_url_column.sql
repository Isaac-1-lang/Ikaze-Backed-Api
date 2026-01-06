-- Fix logo_url column type from bytea to TEXT
ALTER TABLE shops ALTER COLUMN logo_url TYPE TEXT USING logo_url::text;
