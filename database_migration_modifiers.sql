-- ============================================
-- Database Migration: Menu Modifiers System
-- ============================================
-- This script adds support for menu modifiers, add-ons, and variations
-- Run this after the base schema is created
-- ============================================

-- Create ModifierGroup table
CREATE TABLE IF NOT EXISTS modifier_group (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    is_required BOOLEAN DEFAULT false,
    allow_multiple BOOLEAN DEFAULT false,
    min_selection INTEGER DEFAULT 0,
    max_selection INTEGER,
    is_active BOOLEAN DEFAULT true
);

CREATE INDEX idx_modifier_group_active ON modifier_group(is_active);

-- Create MenuModifier table
CREATE TABLE IF NOT EXISTS menu_modifier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    modifier_group_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price DOUBLE PRECISION DEFAULT 0.0,
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER,
    CONSTRAINT fk_menu_modifier_group FOREIGN KEY (modifier_group_id) 
        REFERENCES modifier_group(id) ON DELETE CASCADE
);

CREATE INDEX idx_menu_modifier_group_id ON menu_modifier(modifier_group_id);
CREATE INDEX idx_menu_modifier_active ON menu_modifier(is_active);

-- Create MenuItemModifierGroup linking table
CREATE TABLE IF NOT EXISTS menu_item_modifier_group (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id UUID NOT NULL,
    modifier_group_id UUID NOT NULL,
    display_order INTEGER,
    CONSTRAINT fk_menu_item_modifier_group_menu_item FOREIGN KEY (menu_item_id) 
        REFERENCES menu_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_menu_item_modifier_group_modifier_group FOREIGN KEY (modifier_group_id) 
        REFERENCES modifier_group(id) ON DELETE CASCADE,
    UNIQUE(menu_item_id, modifier_group_id)
);

CREATE INDEX idx_menu_item_modifier_group_menu_item ON menu_item_modifier_group(menu_item_id);
CREATE INDEX idx_menu_item_modifier_group_modifier_group ON menu_item_modifier_group(modifier_group_id);

-- Add modifiers column to order_item (via new table)
CREATE TABLE IF NOT EXISTS order_item_modifier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID NOT NULL,
    menu_modifier_id UUID NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_order_item_modifier_order_item FOREIGN KEY (order_item_id) 
        REFERENCES order_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_modifier_menu_modifier FOREIGN KEY (menu_modifier_id) 
        REFERENCES menu_modifier(id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_item_modifier_order_item ON order_item_modifier(order_item_id);
CREATE INDEX idx_order_item_modifier_menu_modifier ON order_item_modifier(menu_modifier_id);

-- ============================================
-- Migration Notes:
-- ============================================
-- 1. Existing OrderItem records will have no modifiers (backward compatible)
-- 2. OrderItem.price now stores base price per unit (not total)
-- 3. Total price calculation: (basePrice + sum(modifier prices)) * quantity
-- 4. BillingController has been updated to include modifiers in calculations
-- ============================================

