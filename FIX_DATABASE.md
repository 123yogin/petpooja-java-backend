# Fix Database and Compilation Errors

## Issues Found:
1. **Database Error**: `ERROR: column mi1_0.tax_rate does not exist`
2. **Compilation Error**: `The method seedEmployees() is undefined`

## Solution:

### Step 1: Run Database Migration

Connect to your PostgreSQL database and run the migration script:

```sql
-- Connect to database
psql -U postgres -d petpooja_db

-- Or use pgAdmin or any PostgreSQL client

-- Run the migration
\i database_migration_gst.sql

-- Or copy-paste the SQL commands from database_migration_gst.sql
```

**Quick SQL to fix the tax_rate column:**
```sql
ALTER TABLE menu_item 
ADD COLUMN IF NOT EXISTS tax_rate DOUBLE PRECISION;

UPDATE menu_item SET tax_rate = 5.0 WHERE tax_rate IS NULL;
```

### Step 2: Clean and Rebuild Project

```bash
cd backend/petpooja_clone
mvn clean compile
```

Or in your IDE:
- Right-click on project → Maven → Reload Project
- Then: Maven → Clean
- Then: Maven → Compile

### Step 3: Restart Application

After fixing the database and rebuilding, restart the Spring Boot application.

## Alternative: Let Hibernate Auto-Update (if database is empty)

If your database is empty or you can recreate it:
1. Set `spring.jpa.hibernate.ddl-auto=create` temporarily
2. Restart application (this will recreate all tables)
3. Set back to `spring.jpa.hibernate.ddl-auto=update`

**⚠️ Warning**: This will DELETE all existing data!

