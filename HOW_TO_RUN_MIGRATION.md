# How to Run the Migration

## Quick Start

Run the SQL file `MIGRATE_TO_MULTI_VENDOR.sql` in your PostgreSQL database.

## Option 1: Using pgAdmin (Easiest)

1. Open **pgAdmin**
2. Connect to your PostgreSQL server
3. Right-click on your `ecommerce` database
4. Select **Query Tool**
5. Click **Open File** (or press Ctrl+O)
6. Select `MIGRATE_TO_MULTI_VENDOR.sql`
7. Click **Execute** (or press F5)
8. Wait for "Query returned successfully"

## Option 2: Using psql Command Line

```bash
# Windows PowerShell
cd Backend-service
$env:PGPASSWORD="JOVIN19"
psql -U jovin -d ecommerce -f MIGRATE_TO_MULTI_VENDOR.sql

# Or with full connection string
psql -h localhost -p 5432 -U jovin -d ecommerce -f MIGRATE_TO_MULTI_VENDOR.sql
```

## Option 3: Using DBeaver or Other SQL Client

1. Open your SQL client (DBeaver, DataGrip, etc.)
2. Connect to your `ecommerce` database
3. Open `MIGRATE_TO_MULTI_VENDOR.sql` file
4. Execute the script
5. Check the results

## Option 4: Copy-Paste SQL Directly

1. Open `MIGRATE_TO_MULTI_VENDOR.sql`
2. Copy all the SQL (Ctrl+A, Ctrl+C)
3. Open your database client
4. Paste into query editor
5. Execute

## After Running Migration

### 1. Verify It Worked

The script includes verification queries at the end. Both should return `0`:

```sql
-- Should return 0
SELECT COUNT(*) FROM order_items WHERE shop_order_id IS NULL;

-- Should return 0  
SELECT COUNT(*) FROM shop_orders WHERE order_id IS NULL;
```

### 2. Restart Your Backend

```bash
cd Backend-service
mvn spring-boot:run
```

### 3. Test Checkout

Try checking out from the frontend. It should work now!

## Troubleshooting

### Error: "column does not exist"
- This might mean the column was already removed
- The script uses `DROP COLUMN IF EXISTS` so it's safe to run multiple times
- Check if migration already completed

### Error: "permission denied"
- You need database owner or superuser privileges
- Use the database owner account (usually `postgres` or your admin user)

### Error: "relation does not exist"
- Make sure you're connected to the correct database (`ecommerce`)
- Check your database name in `application.properties`

### Error: "constraint does not exist"
- This is normal - the script uses `IF EXISTS` so it won't fail
- The migration will continue even if some constraints don't exist

## What the Migration Does

1. **Removes `order_id` from `order_items`**
   - Items now link to `shop_order_id` instead
   - This enables multi-vendor support

2. **Removes `pickup_token` from `orders`**
   - Pickup tokens are now shop-specific
   - Each `shop_order` has its own token

3. **Verifies data integrity**
   - Checks for orphaned records
   - Ensures all relationships are correct

## Database Credentials

If you need to check your database credentials, look in:
```
Backend-service/src/main/resources/application.properties
```

Default values:
- Database: `ecommerce`
- User: `jovin`
- Password: `JOVIN19`
- Host: `localhost`
- Port: `5432`

## Need Help?

If you encounter issues:
1. Check the error message carefully
2. Verify database connection
3. Ensure you have proper permissions
4. Check application logs after restarting

The migration is **safe to run multiple times** - it uses `IF EXISTS` checks.

