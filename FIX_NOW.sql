-- =====================================================
-- COMPLETE FIX - Run This to Fix Everything
-- =====================================================

USE clinics;

-- Step 1: Drop Flyway history (so it can run fresh)
DROP TABLE IF EXISTS flyway_schema_history;

-- Step 2: Create admin user directly (if users table exists)
-- If this fails because users table doesn't exist yet, that's fine - 
-- Flyway will create it when the app starts

INSERT IGNORE INTO users (email, password_hash, role, status, created_at, updated_at) 
VALUES (
    'admin@healsync.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HinjWWW2wt0ktkCkF0VYy',
    'ADMIN',
    'ACTIVE',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    password_hash='$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HinjWWW2wt0ktkCkF0VYy',
    role='ADMIN';

-- Verify
SELECT 'Admin user ready!' AS Status;
SELECT id, email, role FROM users WHERE email = 'admin@healsync.com';

-- =====================================================
-- NOW RESTART THE APPLICATION
-- Login: admin@healsync.com / admin123
-- =====================================================
