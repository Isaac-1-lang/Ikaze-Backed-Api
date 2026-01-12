# Fix Checkout Error - Complete Guide

## The Problem

You're getting this error when trying to checkout:
```
ERROR: null value in column "order_id" of relation "order_items" violates not-null constraint
```

## Root Cause

The **database schema is out of sync with the application code**.

The application has been refactored to support multi-vendor orders:
- **OLD**: `Order` → `OrderItem`
- **NEW**: `Order` → `ShopOrder` → `OrderItem`

But the database still has the old structure where `order_items` has an `order_id` column.

## The Fix (3 Options)

### ⚡ OPTION 1: PowerShell Script (Easiest)

```powershell
cd Backend-service
.\RUN_MIGRATION.ps1
```

This will:
- Test database connection
- Run the migration
- Verify everything worked
- Give you clear feedback

**Custom database?** Provide parameters:
```powershell
.\RUN_MIGRATION.ps1 -DbHost localhost -DbPort 5432 -DbName ecommerce -DbUser jovin -DbPassword JOVIN19
```

### 📝 OPTION 2: Direct SQL (Fast)

Open PowerShell and run:

```powershell
$env:PGPASSWORD="JOVIN19"
psql -U jovin -d ecommerce -f "src\main\resources\db\migration\complete_multi_vendor_migration.sql"
```

Or manually execute these commands in psql or pgAdmin:

```sql
-- Step 1: Fix order_items table
ALTER TABLE order_items ALTER COLUMN order_id DROP NOT NULL;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS fk_order_items_order;
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_order_id_fkey;
DROP INDEX IF EXISTS idx_order_items_order_id;
ALTER TABLE order_items DROP COLUMN IF EXISTS order_id;

-- Step 2: Fix orders table
ALTER TABLE orders ALTER COLUMN pickup_token DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN pickup_token_used DROP NOT NULL;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_pickup_token_key;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token;
ALTER TABLE orders DROP COLUMN IF EXISTS pickup_token_used;
```

### 🗃️ OPTION 3: Using pgAdmin

1. Open pgAdmin
2. Connect to your `ecommerce` database
3. Open Query Tool (Tools → Query Tool)
4. Paste the SQL from Option 2
5. Execute (F5)

## After Running Migration

### 1. Verify It Worked

Run these queries to check:

```sql
-- Should NOT show order_id column
\d order_items;

-- Should NOT show pickup_token columns  
\d orders;

-- Should show proper relationships
SELECT 
    COUNT(*) as orphaned_items 
FROM order_items 
WHERE shop_order_id IS NULL;
-- Should return 0

SELECT 
    COUNT(*) as orphaned_shop_orders 
FROM shop_orders 
WHERE order_id IS NULL;
-- Should return 0
```

### 2. Restart Spring Boot Application

```powershell
cd Backend-service
mvn spring-boot:run
```

Wait for:
```
Started EcommerceApplication in X seconds
```

### 3. Test Checkout

Try checking out again from the frontend. It should now work!

## What Changed in the Architecture?

### Before (Single Vendor)
```
Order
  └── OrderItem (direct relationship)
  └── pickup_token (on Order)
```

### After (Multi-Vendor)
```
Order
  └── ShopOrder (one per shop)
      ├── OrderItem (items for that shop)
      └── pickup_token (per shop)
```

### Why?

- **Multi-vendor support**: One customer order can have items from multiple shops
- **Independent fulfillment**: Each shop manages their own portion
- **Better tracking**: Shop-specific status, shipping, pickup tokens
- **Scalability**: Easy to add new shops without cross-dependencies

## Benefits

1. **Shop Independence**: Each shop has their own:
   - Pickup token
   - Order status  
   - Shipping cost
   - Fulfillment process

2. **Customer Experience**: Customer sees:
   - Which shop is fulfilling which items
   - Per-shop tracking
   - Individual pickup tokens per shop

3. **Business Logic**: Supports:
   - Shop-specific discounts
   - Shop-specific shipping calculations
   - Shop-specific reward points
   - Independent delivery schedules

## Troubleshooting

### Error: "psql: command not found"

**Solution**: Install PostgreSQL client or add to PATH
```powershell
# Check if PostgreSQL is installed
Get-Command psql

# Add to PATH (adjust path to your PostgreSQL installation)
$env:Path += ";C:\Program Files\PostgreSQL\16\bin"
```

### Error: "password authentication failed"

**Solution**: Check credentials in `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce
spring.datasource.username=jovin
spring.datasource.password=JOVIN19
```

### Error: "database does not exist"

**Solution**: Create the database first:
```powershell
psql -U postgres -c "CREATE DATABASE ecommerce;"
```

### Error: "permission denied"

**Solution**: Use database owner or superuser account
```powershell
psql -U postgres -d ecommerce
```

### Error: "Migration runs but checkout still fails"

**Checklist**:
1. ✓ Did you restart the Spring Boot application after migration?
2. ✓ Check application logs for startup errors
3. ✓ Verify migration actually completed (run verification queries)
4. ✓ Clear any browser cache/local storage
5. ✓ Check if there are any other database constraints

## Testing Checklist

After fixing, test these scenarios:

- [ ] Guest checkout with single shop items
- [ ] Guest checkout with multiple shops items
- [ ] Authenticated checkout with single shop
- [ ] Authenticated checkout with multiple shops
- [ ] Verify pickup tokens are generated per shop
- [ ] Check order status updates correctly
- [ ] Verify email confirmations are sent
- [ ] Test cart clearing after checkout

## Need More Help?

Check these files for detailed documentation:
- `MULTI_VENDOR_ARCHITECTURE.md` - Full architecture explanation
- `RUN_MIGRATION.md` - Detailed migration guide
- `QUICK_FIX.md` - Quick reference card

## Still Having Issues?

1. **Check application logs**: Look for specific error messages in console
2. **Verify database state**: Use psql to inspect tables
3. **Test database connection**: Ensure Spring Boot can connect
4. **Review entity mappings**: Ensure JPA mappings are correct

## Success Indicators

You'll know it's fixed when:
- ✓ No database errors in logs
- ✓ Checkout completes successfully
- ✓ Order is created with ShopOrders
- ✓ Each shop has a unique pickup token
- ✓ Payment success page shows order details
- ✓ Order confirmation email is sent

## Important Notes

- **Backup First**: Always backup your database before migrations
- **Test Environment**: Run on test database first if possible
- **Rollback Plan**: Keep backup file to restore if needed
- **Documentation**: Keep this file for future reference

Good luck! 🚀

