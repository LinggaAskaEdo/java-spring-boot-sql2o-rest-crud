-- Migration: clean_all_data
-- Created : 2026-03-27

-- Clean all data from tables (order matters due to foreign key constraints)
DELETE FROM products_company;
DELETE FROM tutorial_details;
DELETE FROM tutorials;
DELETE FROM products;
DELETE FROM company;
