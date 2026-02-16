-- ========================================
-- Clean Database Script
-- Run this in MySQL Workbench before starting the application
-- ========================================

USE clinics;

-- Drop flyway history
DROP TABLE IF EXISTS flyway_schema_history;

-- Drop all application tables (in correct order to respect foreign keys)
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
DROP TABLE IF EXISTS clinics;
DROP TABLE IF EXISTS users;

-- Verify all tables are dropped
SHOW TABLES;

-- You should see: Empty set (0.00 sec)
-- Now run: mvn spring-boot:run
