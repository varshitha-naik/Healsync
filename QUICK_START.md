# 🚀 Quick Start - Run HealSync Platform

## Database: `clinics` ✅

Your configuration is correct!

---

## Step 1: Clean Database (30 seconds)

**Open MySQL Workbench** → Connect to localhost → Run:

```sql
USE clinics;

DROP TABLE IF EXISTS flyway_schema_history;
DROP TABLE IF EXISTS audit_logs, notifications, prescription_items, 
     prescriptions, report_attachments, medical_reports, appointments, 
     appointment_slots, patient_profiles, doctor_profiles, users;

-- Verify clean
SHOW TABLES;  -- Should be empty or only show 'clinics'
```

---

## Step 2: Start Application (1 minute)

**Open PowerShell** in `c:/Users/shaik/OneDrive/Desktop/HealSync`:

```powershell
& "C:\Program Files\apache-maven-3.9.12\bin\mvn.cmd" spring-boot:run
```

**Wait for:**
```
Started HealSyncApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

**✅ Flyway will automatically:**
- Create 12 tables
- Insert 5 users (1 admin, 2 doctors, 2 patients)
- Add sample data (appointments, reports, etc.)

---

## Step 3: Test Application (1 minute)

### Open Browser

http://localhost:8080

### Login with Demo Credentials

| Role | Email | Password |
|------|-------|----------|
| **Admin** | admin@healsync.com | Password123! |
| **Doctor** | dr.smith@healsync.com | Password123! |
| **Patient** | patient.john@example.com | Password123! |

### Test REST API

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@healsync.com\",\"password\":\"Password123!\"}"
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "admin@healsync.com",
  "role": "ADMIN",
  "userId": 1
}
```

---

## Verify Database Tables

In MySQL Workbench:

```sql
USE clinics;

-- Should show 13 tables
SHOW TABLES;

-- Should show 3 migrations
SELECT * FROM flyway_schema_history;

-- Should show 5 users
SELECT id, email, role FROM users;

-- Should show 2 doctors
SELECT id, full_name, specialization FROM doctor_profiles;

-- Should show 2 patients
SELECT id, full_name FROM patient_profiles;

-- Should show appointment slots
SELECT * FROM appointment_slots;
```

---

## ✅ Success Checklist

- [ ] Database `clinics` exists
- [ ] All tables dropped (clean slate)
- [ ] Application started without errors
- [ ] Login page loads at http://localhost:8080
- [ ] Can login with demo credentials
- [ ] JWT token is returned from login API
- [ ] 13 tables exist in database
- [ ] 5 users in database

---

## Common Issues & Fixes

### Issue: "Table already exists"
**Fix:** Run the DROP TABLE commands in Step 1 again

### Issue: "Flyway validation failed"
**Fix:** Make sure you dropped `flyway_schema_history` table

### Issue: Application won't start
**Check:** 
- MySQL is running
- Database `clinics` exists
- Password in `application.properties` is `taheer123`
- Port 8080 is free

### Issue: "Cannot find symbol" compile error
**Fix:** Already fixed! JWT API updated for version 0.12.3

---

## What's Working ✅

✅ Authentication (login/register)
✅ JWT token generation & validation
✅ BCrypt password hashing
✅ Database migrations via Flyway
✅ Role-based authorization
✅ Modern login UI
✅ MySQL integration
✅ Mailtrap email configuration

---

## What's Next 📋

The foundation is complete! To build the full system:

1. **Doctor API** - Profile management, photo upload
2. **Patient API** - Profile management, view records
3. **Appointment API** - Book/manage appointments
4. **Report API** - Create/view medical reports
5. **Prescription API** - Create/view prescriptions
6. **File Upload** - Doctor photos & report attachments
7. **Email Service** - Send notifications via Mailtrap
8. **Dashboards** - Admin, Doctor, Patient UIs

See `PROJECT_STATUS.md` for detailed implementation guide.

---

## 🎉 You're All Set!

Your HealSync Platform is ready to run with:
- ✅ 12 production-ready entities
- ✅ Complete security layer (JWT + Spring Security)
- ✅ Sample data for immediate testing
- ✅ Modern UI foundation

**Now run the 3 steps above and see it live!** 🚀
