-- ============================================
-- Complete Database Schema for Petpooja Clone
-- ============================================
-- This script creates all tables as they are generated
-- when the Spring Boot application runs with Hibernate
-- 
-- Database: PostgreSQL
-- Database Name: petpooja_db
-- All IDs use UUID type
-- ============================================

-- Create database if it doesn't exist (run as superuser)
-- CREATE DATABASE petpooja_db;

-- Connect to the database
-- \c petpooja_db;

-- ============================================

-- Drop existing tables (if any) in correct order to respect foreign keys
DROP TABLE IF EXISTS task CASCADE;
DROP TABLE IF EXISTS purchase_order CASCADE;
DROP TABLE IF EXISTS menu_ingredient CASCADE;
DROP TABLE IF EXISTS bill CASCADE;
DROP TABLE IF EXISTS order_item CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS restaurant_table CASCADE;
DROP TABLE IF EXISTS menu_item CASCADE;
DROP TABLE IF EXISTS ingredient CASCADE;
DROP TABLE IF EXISTS supplier CASCADE;
DROP TABLE IF EXISTS customer CASCADE;
DROP TABLE IF EXISTS "user" CASCADE;

-- ============================================
-- 1. USER TABLE
-- ============================================
CREATE TABLE "user" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255),
    password VARCHAR(255),
    role VARCHAR(50) -- ADMIN, MANAGER, CASHIER
);

CREATE INDEX idx_user_email ON "user"(email);

-- ============================================
-- 2. MENU ITEM TABLE
-- ============================================
CREATE TABLE menu_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    price DOUBLE PRECISION NOT NULL,
    available BOOLEAN DEFAULT true,
    description VARCHAR(1000),
    hsn_code VARCHAR(50),
    tax_rate DOUBLE PRECISION
);

CREATE INDEX idx_menu_item_category ON menu_item(category);
CREATE INDEX idx_menu_item_available ON menu_item(available);

-- ============================================
-- 3. RESTAURANT TABLE
-- ============================================
CREATE TABLE restaurant_table (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_number VARCHAR(50),
    occupied BOOLEAN DEFAULT false
);

CREATE INDEX idx_restaurant_table_number ON restaurant_table(table_number);
CREATE INDEX idx_restaurant_table_occupied ON restaurant_table(occupied);

-- ============================================
-- 4. ORDERS TABLE
-- ============================================
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_id UUID,
    total_amount DOUBLE PRECISION DEFAULT 0.0,
    status VARCHAR(50) DEFAULT 'CREATED', -- CREATED, IN_PROGRESS, COMPLETED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_table FOREIGN KEY (table_id) 
        REFERENCES restaurant_table(id) ON DELETE SET NULL
);

CREATE INDEX idx_orders_table_id ON orders(table_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- ============================================
-- 5. ORDER ITEM TABLE
-- ============================================
CREATE TABLE order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    menu_item_id UUID,
    quantity INTEGER NOT NULL DEFAULT 1,
    price DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) 
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_menu_item FOREIGN KEY (menu_item_id) 
        REFERENCES menu_item(id) ON DELETE SET NULL
);

CREATE INDEX idx_order_item_order_id ON order_item(order_id);
CREATE INDEX idx_order_item_menu_item_id ON order_item(menu_item_id);

-- ============================================
-- 6. BILL TABLE
-- ============================================
CREATE TABLE bill (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID UNIQUE,
    total_amount DOUBLE PRECISION NOT NULL,
    tax DOUBLE PRECISION DEFAULT 0.0,
    discount_amount DOUBLE PRECISION DEFAULT 0.0,
    grand_total DOUBLE PRECISION NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    company_gstin VARCHAR(50),
    customer_gstin VARCHAR(50),
    cgst DOUBLE PRECISION,
    sgst DOUBLE PRECISION,
    igst DOUBLE PRECISION,
    place_of_supply VARCHAR(10),
    is_inter_state BOOLEAN,
    CONSTRAINT fk_bill_order FOREIGN KEY (order_id) 
        REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_bill_order_id ON bill(order_id);
CREATE INDEX idx_bill_generated_at ON bill(generated_at);

-- ============================================
-- 7. INGREDIENT TABLE
-- ============================================
CREATE TABLE ingredient (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    quantity DOUBLE PRECISION DEFAULT 0.0,
    unit VARCHAR(50), -- grams, ml, pcs, kg, etc.
    threshold DOUBLE PRECISION DEFAULT 0.0
);

CREATE INDEX idx_ingredient_name ON ingredient(name);
CREATE INDEX idx_ingredient_quantity ON ingredient(quantity);

-- ============================================
-- 8. MENU INGREDIENT TABLE (Many-to-Many relationship)
-- ============================================
CREATE TABLE menu_ingredient (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id UUID,
    ingredient_id UUID,
    quantity_required DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_menu_ingredient_menu_item FOREIGN KEY (menu_item_id) 
        REFERENCES menu_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_menu_ingredient_ingredient FOREIGN KEY (ingredient_id) 
        REFERENCES ingredient(id) ON DELETE CASCADE
);

CREATE INDEX idx_menu_ingredient_menu_item_id ON menu_ingredient(menu_item_id);
CREATE INDEX idx_menu_ingredient_ingredient_id ON menu_ingredient(ingredient_id);

-- ============================================
-- 9. SUPPLIER TABLE
-- ============================================
CREATE TABLE supplier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255),
    contact VARCHAR(255),
    email VARCHAR(255)
);

CREATE INDEX idx_supplier_name ON supplier(name);
CREATE INDEX idx_supplier_email ON supplier(email);

-- ============================================
-- 10. PURCHASE ORDER TABLE
-- ============================================
CREATE TABLE purchase_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ingredient_id UUID,
    supplier_id UUID,
    quantity DOUBLE PRECISION NOT NULL,
    cost DOUBLE PRECISION NOT NULL,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchase_order_ingredient FOREIGN KEY (ingredient_id) 
        REFERENCES ingredient(id) ON DELETE SET NULL,
    CONSTRAINT fk_purchase_order_supplier FOREIGN KEY (supplier_id) 
        REFERENCES supplier(id) ON DELETE SET NULL
);

CREATE INDEX idx_purchase_order_ingredient_id ON purchase_order(ingredient_id);
CREATE INDEX idx_purchase_order_supplier_id ON purchase_order(supplier_id);
CREATE INDEX idx_purchase_order_date ON purchase_order(date);

-- ============================================
-- 11. CUSTOMER TABLE
-- ============================================
CREATE TABLE customer (
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

CREATE INDEX idx_customer_name ON customer(name);
CREATE INDEX idx_customer_gstin ON customer(gstin);
CREATE INDEX idx_customer_is_active ON customer(is_active);

-- ============================================
-- 12. TASK TABLE
-- ============================================
CREATE TABLE task (
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

CREATE INDEX idx_task_assigned_to_id ON task(assigned_to_id);
CREATE INDEX idx_task_status ON task(status);
CREATE INDEX idx_task_priority ON task(priority);
CREATE INDEX idx_task_due_date ON task(due_date);

-- ============================================
-- COMMENTS ON TABLES
-- ============================================
COMMENT ON TABLE "user" IS 'Stores user accounts with roles (ADMIN, MANAGER, CASHIER)';
COMMENT ON TABLE menu_item IS 'Menu items available in the restaurant';
COMMENT ON TABLE restaurant_table IS 'Restaurant tables for seating';
COMMENT ON TABLE orders IS 'Customer orders linked to tables';
COMMENT ON TABLE order_item IS 'Individual items in an order';
COMMENT ON TABLE bill IS 'Generated bills for completed orders';
COMMENT ON TABLE ingredient IS 'Inventory ingredients with quantities';
COMMENT ON TABLE menu_ingredient IS 'Mapping between menu items and required ingredients';
COMMENT ON TABLE supplier IS 'Suppliers for inventory management';
COMMENT ON TABLE purchase_order IS 'Purchase orders for restocking ingredients';
COMMENT ON TABLE customer IS 'B2B customers with GSTIN and credit limits';
COMMENT ON TABLE task IS 'Operational tasks assigned to staff members';

-- ============================================
-- SAMPLE DATA (Optional - Uncomment to insert)
-- ============================================

-- Sample Admin User
-- INSERT INTO "user" (email, username, password, role) 
-- VALUES ('admin@test.com', 'AdminUser', '$2a$10$...', 'ADMIN');

-- Sample Menu Items
-- INSERT INTO menu_item (name, category, price, available) 
-- VALUES ('Paneer Butter Masala', 'Main Course', 250.0, true);

-- Sample Table
-- INSERT INTO restaurant_table (table_number, occupied) 
-- VALUES ('T1', false);

-- ============================================
-- END OF SCHEMA
-- ============================================

