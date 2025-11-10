-- ============================================
-- Database Migration Script for UUID Support
-- ============================================
-- This script drops existing tables to allow Hibernate
-- to recreate them with UUID columns instead of BIGINT
-- 
-- ⚠️ WARNING: This will DELETE ALL DATA!
-- Only run this in development environment
-- ============================================

-- Drop tables in correct order (respecting foreign key constraints)
DROP TABLE IF EXISTS order_item CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS restaurant_table CASCADE;
DROP TABLE IF EXISTS menu_item CASCADE;
DROP TABLE IF EXISTS "user" CASCADE;

-- After running this script, restart your Spring Boot application
-- Hibernate will automatically recreate all tables with UUID columns
-- using the @GeneratedValue(strategy = GenerationType.UUID) annotation

