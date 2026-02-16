# Quick Fix - Flyway Migration Issue

## Problem
Flyway validation is failing. This happens when:
1. Migrations were partially executed
2. Database schema doesn't match migration history

## Solution

### Step 1: Clean Database

Copy and paste this into MySQL Workbench or command line:

```sql
USE clinics;

-- Drop flyway history
DROP TABLE IF EXISTS flyway_schema_history;

-- Drop all tables in correct order (respecting foreign keys)
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
```

### Step 2: Run Spring Boot

After clearing the database, run:

```powershell
& "C:\Program Files\apache-maven-3.9.12\bin\mvn.cmd" spring-boot:run
```

### Step 3: Verify Success

The application will:
✅ Create flyway_schema_history table
✅ Execute V1__init_schema.sql → Create 12 tables
✅ Execute V2__seed_data.sql → Insert demo data (5 users, 2 doctors, 2 patients, appointments)
✅ Execute V3__indexes.sql → Add performance indexes
✅ Start Tomcat on port 8080

You should see in the console:
```
Started HealSyncApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

### Step 4: Verify Database

Check in MySQL:

```sql
USE clinics;

-- Should show 3 migrations
SELECT version, description, success FROM flyway_schema_history;

-- Should show 13 tables
SHOW TABLES;

-- Should show 5 users
SELECT id, email, role FROM users;

-- Should show 2 doctors  
SELECT id, full_name, specialization FROM doctor_profiles;
```

## Common Issues

**Issue**: "Table already exists"
**Fix**: Run the DROP TABLE commands again

**Issue**: "Cannot drop table - foreign key constraint"
**Fix**: The DROP commands are in correct order, run them all at once

**Issue**: Application still fails
**Fix**: Check application.properties has correct MySQL password (taheer123)

## Once Migrations Complete

Visit: http://localhost:8080

You'll see the login page. Use these credentials:

- **Admin**: admin@healsync.com / Password123!
- **Doctor**: dr.smith@healsync.com / Password123!
- **Patient**: patient.john@example.com / Password123!
