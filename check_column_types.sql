-- Check current column types in shops and users tables
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'shops' 
ORDER BY ordinal_position;

SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'users' 
AND column_name IN ('first_name', 'last_name', 'user_email', 'phone_number')
ORDER BY ordinal_position;
