-- Make zipcode column nullable in order_addresses table
ALTER TABLE order_addresses ALTER COLUMN zipcode DROP NOT NULL;
