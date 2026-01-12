# Database Migration Guide

## Overview
This migration updates the database schema to support the multi-vendor architecture where:
- `Order` contains multiple `ShopOrder` (one per shop)
- `OrderItem` links to `ShopOrder` (not directly to `Order`)
- Pickup tokens are on `ShopOrder` (shop-specific, not order-level)

## Prerequisites
1. **BACKUP YOUR DATABASE** before running any migration!
2. Ensure no active transactions are running
3. Have database admin credentials ready

## Migration Steps

### Option 1: Run Complete Migration (Recommended)
```bash
# Connect to your PostgreSQL database
psql -U your_username -d ecommerce

# Run the complete migration script
\i src/main/resources/db/migration/complete_multi_vendor_migration.sql
```

### Option 2: Run Individual Migrations
```bash
# 1. Remove order_id from order_items
psql -U your_username -d ecommerce -f src/main/resources/db/migration/remove_order_id_from_order_items.sql

# 2. Remove pickup tokens from orders
psql -U your_username -d ecommerce -f src/main/resources/db/migration/remove_order_pickup_token_columns.sql
```

### Option 3: Manual SQL Execution
If you prefer to run SQL manually, execute these commands in order:

```sql
-- 1. Fix order_items table
ALTER TABLE order_items ALTER COLUMN order_id DROP NOT NULL;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS fk_order_items_order;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_order_id_fkey;
DROP INDEX IF EXISTS idx_order_items_order_id;
ALTER TABLE order_items DROP COLUMN IF EXISTS order_id;

-- 2. Fix orders table
ALTER TABLE orders ALTER COLUMN pickup_token DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN pickup_token_used DROP NOT NULL;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_pickup_token_key;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token_used;
```

## Verification

After running the migration, verify the changes:

```sql
-- Check order_items structure (should NOT have order_id)
\d order_items;

-- Check orders structure (should NOT have pickup_token or pickup_token_used)
\d orders;

-- Check shop_orders structure (should have pickup_token and pickup_token_used)
\d shop_orders;

-- Verify data integrity
SELECT COUNT(*) FROM order_items WHERE shop_order_id IS NULL;
-- Should return 0

SELECT COUNT(*) FROM shop_orders WHERE order_id IS NULL;
-- Should return 0
```

## Rollback (If Needed)

If something goes wrong, restore from your backup:

```bash
# Restore from backup
psql -U your_username -d ecommerce < backup_file.sql
```

## After Migration

1. Restart your Spring Boot application
2. Test checkout flow (both authenticated and guest)
3. Verify orders are created correctly with shop_orders
4. Check that pickup tokens are generated on shop_orders

## Troubleshooting

### Error: "column order_id does not exist"
- This is expected after migration. Restart your application.

### Error: "null value in column order_id violates not-null constraint"
- The migration hasn't been run yet. Follow the steps above.

### Application won't start
- Check application logs for specific errors
- Ensure all migration scripts completed successfully
- Verify database connection settings

## Support

If you encounter issues:
1. Check the application logs
2. Verify database schema matches expected structure
3. Ensure all foreign key relationships are correct

