# Quick Fix Guide

## Current Error
```
ERROR: null value in column "order_id" of relation "order_items" violates not-null constraint
```

## Root Cause
The database still has the old schema where `order_items` has an `order_id` column, but the application now uses `shop_order_id`.

## Solution (Choose One)

### Option 1: Quick Fix (Run SQL Directly)
Open your PostgreSQL client and run:

```sql
-- Fix order_items
ALTER TABLE order_items ALTER COLUMN order_id DROP NOT NULL;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS fk_order_items_order;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_order_id_fkey;
DROP INDEX IF EXISTS idx_order_items_order_id;
ALTER TABLE order_items DROP COLUMN IF EXISTS order_id;

-- Fix orders
ALTER TABLE orders ALTER COLUMN pickup_token DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN pickup_token_used DROP NOT NULL;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_pickup_token_key;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token_used;
```

### Option 2: Run Migration Script
```bash
cd Backend-service
psql -U your_username -d ecommerce -f src/main/resources/db/migration/complete_multi_vendor_migration.sql
```

### Option 3: Using psql Command Line
```bash
psql -U your_username -d ecommerce << 'EOF'
ALTER TABLE order_items ALTER COLUMN order_id DROP NOT NULL;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS fk_order_items_order;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_order_id_fkey;
DROP INDEX IF EXISTS idx_order_items_order_id;
ALTER TABLE order_items DROP COLUMN IF EXISTS order_id;

ALTER TABLE orders ALTER COLUMN pickup_token DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN pickup_token_used DROP NOT NULL;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_pickup_token_key;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token_used;
EOF
```

## Verify Fix Worked

After running the migration, check:

```sql
-- Should NOT show order_id column
\d order_items;

-- Should NOT show pickup_token columns
\d orders;
```

## Then Restart Application

```bash
cd Backend-service
mvn spring-boot:run
```

## Test Checkout

Try creating an order again. It should work now!

## What Changed?

### Entity Structure
```
OLD: Order → OrderItem
NEW: Order → ShopOrder → OrderItem
```

### Why?
- Multi-vendor support: one order can have items from multiple shops
- Each shop has its own `ShopOrder` with independent:
  - Pickup token
  - Status
  - Shipping cost
  - Fulfillment tracking

## Need Help?

1. Check `MULTI_VENDOR_ARCHITECTURE.md` for full documentation
2. Check `RUN_MIGRATION.md` for detailed migration guide
3. Verify database connection in `application.properties`
4. Check application logs for specific errors

## Common Issues

### "column order_id does not exist"
✅ Good! This means migration worked. Restart application.

### "relation order_items does not exist"
❌ Wrong database or connection issue. Check `application.properties`.

### "permission denied"
❌ Need database admin rights. Use superuser or owner account.

### Application still crashes
1. Stop application completely
2. Run migration
3. Clear any caches: `mvn clean`
4. Restart: `mvn spring-boot:run`

