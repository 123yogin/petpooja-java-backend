-- ============================================
-- Database Migration Script for GST Compliance
-- ============================================
-- This script adds GST-related columns to existing tables
-- Run this script if you have existing data in your database
-- ============================================

-- Add GST fields to menu_item table
ALTER TABLE menu_item 
ADD COLUMN IF NOT EXISTS description VARCHAR(1000),
ADD COLUMN IF NOT EXISTS hsn_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS tax_rate DOUBLE PRECISION;

-- Update existing menu items with default tax rate if null
UPDATE menu_item SET tax_rate = 5.0 WHERE tax_rate IS NULL;

-- Add GST fields to bill table
ALTER TABLE bill 
ADD COLUMN IF NOT EXISTS discount_amount DOUBLE PRECISION DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS company_gstin VARCHAR(50),
ADD COLUMN IF NOT EXISTS customer_gstin VARCHAR(50),
ADD COLUMN IF NOT EXISTS cgst DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS sgst DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS igst DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS place_of_supply VARCHAR(10),
ADD COLUMN IF NOT EXISTS is_inter_state BOOLEAN;

-- Create customer table if it doesn't exist
CREATE TABLE IF NOT EXISTS customer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    address VARCHAR(500),
    city VARCHAR(255),
    state VARCHAR(50),
    pincode VARCHAR(20),
    gstin VARCHAR(15),
    credit_limit DOUBLE PRECISION DEFAULT 0.0,
    payment_terms VARCHAR(100),
    is_active BOOLEAN DEFAULT true
);

-- Create indexes for customer table
CREATE INDEX IF NOT EXISTS idx_customer_name ON customer(name);
CREATE INDEX IF NOT EXISTS idx_customer_gstin ON customer(gstin);
CREATE INDEX IF NOT EXISTS idx_customer_is_active ON customer(is_active);

-- Create task table if it doesn't exist
CREATE TABLE IF NOT EXISTS task (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    assigned_to_id UUID,
    created_by_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50),
    due_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    notes VARCHAR(1000),
    CONSTRAINT fk_task_assigned_to FOREIGN KEY (assigned_to_id) 
        REFERENCES "user"(id) ON DELETE SET NULL,
    CONSTRAINT fk_task_created_by FOREIGN KEY (created_by_id) 
        REFERENCES "user"(id) ON DELETE SET NULL
);

-- Create indexes for task table
CREATE INDEX IF NOT EXISTS idx_task_assigned_to_id ON task(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON task(status);
CREATE INDEX IF NOT EXISTS idx_task_priority ON task(priority);
CREATE INDEX IF NOT EXISTS idx_task_due_date ON task(due_date);

-- ============================================
-- END OF MIGRATION
-- ============================================

