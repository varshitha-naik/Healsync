-- =====================================================
-- SIMPLE FIX: Create Admin User Directly
-- =====================================================
-- Just run this in MySQL Workbench and you're done!

USE clinics;

-- Create admin user directly
-- Email: admin@healsync.com
-- Password: admin123
INSERT INTO users (email, password_hash, role, status, created_at, updated_at) 
VALUES (
    'admin@healsync.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HinjWWW2wt0ktkCkF0VYy',
    'ADMIN',
    'ACTIVE',
    NOW(),
    NOW()
);

-- Verify it was created
SELECT id, email, role, status FROM users WHERE email = 'admin@healsync.com';

-- =====================================================
-- NOW YOU CAN LOGIN:
-- Email: admin@healsync.com
-- Password: admin123
-- =====================================================
