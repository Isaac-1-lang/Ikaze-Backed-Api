# Quick Guide: Apply Database Fix for HYBRID Payment Method

## 🎯 Problem
Remote database constraint doesn't allow `HYBRID` payment method, causing this error:
```
ERROR: new row for relation "order_transactions" violates check constraint "order_transactions_payment_method_check"
```

---

## ✅ Solution: Run This SQL on Remote Database

### Option 1: Using psql Command Line

```bash
# Connect to your remote database
psql -h your-database-host.com -U your-username -d your-database-name

# Then run these commands:
ALTER TABLE order_transactions DROP CONSTRAINT IF EXISTS order_transactions_payment_method_check;

ALTER TABLE order_transactions 
ADD CONSTRAINT order_transactions_payment_method_check 
CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'POINTS', 'HYBRID'));

# Verify it worked:
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'order_transactions'::regclass 
AND conname = 'order_transactions_payment_method_check';
```

---

### Option 2: Using Database GUI (pgAdmin, DBeaver, etc.)

1. **Connect to your remote database**

2. **Open SQL Query Editor**

3. **Copy and paste this SQL:**
   ```sql
   -- Drop old constraint
   ALTER TABLE order_transactions 
   DROP CONSTRAINT IF EXISTS order_transactions_payment_method_check;

   -- Add new constraint with HYBRID
   ALTER TABLE order_transactions 
   ADD CONSTRAINT order_transactions_payment_method_check 
   CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'POINTS', 'HYBRID'));
   ```

4. **Execute the SQL**

5. **Verify with this query:**
   ```sql
   SELECT conname, pg_get_constraintdef(oid) 
   FROM pg_constraint 
   WHERE conrelid = 'order_transactions'::regclass 
   AND conname = 'order_transactions_payment_method_check';
   ```

   **Expected Result:**
   ```
   order_transactions_payment_method_check | CHECK ((payment_method)::text = ANY ((ARRAY['CREDIT_CARD'::character varying, 'DEBIT_CARD'::character varying, 'POINTS'::character varying, 'HYBRID'::character varying])::text[]))
   ```

---

### Option 3: Using Heroku CLI (if hosted on Heroku)

```bash
# Connect to Heroku database
heroku pg:psql -a your-app-name

# Run the SQL commands
ALTER TABLE order_transactions DROP CONSTRAINT IF EXISTS order_transactions_payment_method_check;

ALTER TABLE order_transactions 
ADD CONSTRAINT order_transactions_payment_method_check 
CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'POINTS', 'HYBRID'));

# Verify
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'order_transactions'::regclass 
AND conname = 'order_transactions_payment_method_check';
```

---

### Option 4: Using Railway/Render/Other Cloud Providers

Most cloud providers have a database console. Access it and run:

```sql
ALTER TABLE order_transactions DROP CONSTRAINT IF EXISTS order_transactions_payment_method_check;

ALTER TABLE order_transactions 
ADD CONSTRAINT order_transactions_payment_method_check 
CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'POINTS', 'HYBRID'));
```

---

## 🧪 Test After Applying Fix

1. **Try creating a hybrid payment again**

2. **Should succeed without constraint error**

3. **Verify in database:**
   ```sql
   SELECT order_transaction_id, payment_method, points_used, points_value, status
   FROM order_transactions 
   WHERE payment_method = 'HYBRID'
   ORDER BY created_at DESC
   LIMIT 5;
   ```

---

## ⚠️ Important Notes

- ✅ This is a **safe operation** - it only modifies the constraint, not the data
- ✅ **No downtime required** - can be applied while app is running
- ✅ **Backward compatible** - doesn't affect existing payment methods
- ✅ **Idempotent** - safe to run multiple times (uses `IF EXISTS`)

---

## 🔍 Troubleshooting

### If you get "permission denied" error:
```sql
-- Make sure you're connected as a superuser or database owner
-- Or ask your DBA to run the migration
```

### If constraint name is different:
```sql
-- First, find the actual constraint name:
SELECT conname 
FROM pg_constraint 
WHERE conrelid = 'order_transactions'::regclass 
AND contype = 'c';

-- Then drop it using the actual name:
ALTER TABLE order_transactions DROP CONSTRAINT actual_constraint_name;

-- Then add the new one:
ALTER TABLE order_transactions 
ADD CONSTRAINT order_transactions_payment_method_check 
CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'POINTS', 'HYBRID'));
```

---

## ✅ Success Indicators

After applying the fix, you should see:

1. ✅ No more constraint violation errors
2. ✅ Hybrid payments save successfully
3. ✅ Database shows `payment_method = 'HYBRID'` for hybrid orders
4. ✅ Constraint query returns the updated constraint definition

Done! 🎉
