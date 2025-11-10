# Solve Backend Errors - Step by Step Guide

## Errors Found:
1. ❌ `ERROR: column mi1_0.tax_rate does not exist` - Database schema issue
2. ❌ `The method seedEmployees() is undefined` - Stale compilation issue

## ✅ Solution Steps:

### Step 1: Fix Database Schema

**Option A: Run SQL Script (Recommended)**
```bash
# Connect to PostgreSQL
psql -U postgres -d petpooja_db

# Then run:
ALTER TABLE menu_item 
ADD COLUMN IF NOT EXISTS hsn_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS tax_rate DOUBLE PRECISION;

UPDATE menu_item SET tax_rate = 5.0 WHERE tax_rate IS NULL;
```

**Option B: Use the QUICK_FIX.sql file**
```bash
psql -U postgres -d petpooja_db -f QUICK_FIX.sql
```

**Option C: Use pgAdmin**
1. Open pgAdmin
2. Connect to your database
3. Open Query Tool
4. Copy-paste the SQL from `QUICK_FIX.sql`
5. Execute

### Step 2: Clean and Rebuild Project

**Using Maven Command Line:**
```bash
cd backend/petpooja_clone
mvn clean compile
```

**Using IDE (IntelliJ/Eclipse):**
1. Right-click project → Maven → Reload Project
2. Maven → Clean
3. Maven → Compile
4. Or: Build → Rebuild Project

### Step 3: Restart Application

After fixing database and rebuilding:
1. Stop the running application
2. Start it again

The `seedEmployees()` error should be resolved after a clean rebuild.

## Verification:

After fixing, check:
1. ✅ Application starts without errors
2. ✅ No `tax_rate` column errors in logs
3. ✅ Menu items load correctly
4. ✅ Employees are seeded (if database was empty)

## If Errors Persist:

1. **Check database connection** - Ensure PostgreSQL is running
2. **Check application.properties** - Verify database URL, username, password
3. **Check logs** - Look at `logs/petpooja-clone-error.log` for new errors
4. **Try full clean** - Delete `target/` folder and rebuild

## Quick Test:

After fixing, test by:
1. Starting the backend server
2. Making a GET request to `/menu` endpoint
3. Should return menu items without errors

