-- ========================================
-- FINAL FIX: Clean Database and Use Correct Password
-- ========================================
-- Run this entire script in MySQL Workbench

USE clinics;

-- Step 1: Drop all tables
DROP TABLE IF EXISTS flyway_schema_history;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS prescription_items;
DROP TABLE IF EXISTS prescriptions;
DROP TABLE IF EXISTS report_attachments;
DROP TABLE IF EXISTS medical_reports;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS appointment_slots;
DROP TABLE IF EXISTS patient_profiles;
DROP TABLE IF EXISTS doctor_profiles;
DROP TABLE IF EXISTS users;

-- Step 2: Verify clean
SELECT 'Database cleaned - should show no tables below' AS Status;
SHOW TABLES;

--===============================================
-- NOW RESTART THE APPLICATION:
-- & "C:\Program Files\apache-maven-3.9.12\bin\mvn.cmd" spring-boot:run
--
-- THEN LOGIN WITH:
-- Email: admin@healsync.com
-- Password: Password123!
--===============================================
