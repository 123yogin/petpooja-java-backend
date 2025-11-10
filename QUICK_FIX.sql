-- Quick Fix SQL Script
-- Run this in your PostgreSQL database to fix the missing tax_rate column

-- Connect to your database first:
-- psql -U postgres -d petpooja_db

-- Add missing columns to menu_item table
ALTER TABLE menu_item 
ADD COLUMN IF NOT EXISTS hsn_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS tax_rate DOUBLE PRECISION;

-- Set default tax rate for existing items
UPDATE menu_item SET tax_rate = 5.0 WHERE tax_rate IS NULL;

-- Verify the columns were added
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'menu_item' 
AND column_name IN ('hsn_code', 'tax_rate');

