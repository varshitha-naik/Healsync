# 🎯 Quick User Registration Guide

## Step-by-Step: Create Your Admin User

### Step 1: Access Registration Page

Open your browser and go to:
```
http://localhost:8080/register
```

### Step 2: Fill in the Registration Form

- **Full Name:** Your Name
- **Email:** admin@yourdomain.com (or any email you want)
- **Password:** YourPassword123! (your choice)
- **Date of Birth:** Select your DOB
- **Gender:** Select gender
- **Phone:** Your phone number

Click **"Create Account"**

### Step 3: You'll Be Registered as PATIENT

✅ Account created successfully!
✅ You'll be auto-redirected to login page

### Step 4: Upgrade to ADMIN Role (30 seconds)

Open MySQL Workbench and run:

```sql
USE clinics;

-- Find your user ID
SELECT id, email, role FROM users;

-- Update your role to ADMIN (replace '6' with your actual user ID)
UPDATE users SET role = 'ADMIN' WHERE id = 6;

-- Verify
SELECT id, email, role FROM users WHERE id = 6;
```

### Step 5: Login as ADMIN

Go back to: http://localhost:8080/login

- Email: (the email you registered with)
- Password: (the password you chose)

✅ **You're now logged in as ADMIN!**

---

## Quick Reference

| Step | Action | Time |
|------|--------|------|
| 1 | Go to /register | 5s |
| 2 | Fill form & submit | 30s |
| 3 | Update role in MySQL | 30s |
| 4 | Login as ADMIN | 10s |
| **Total** | **Ready to use!** | **~1 min** |

---

## Why This Works

1. ✅ No password hash issues - you create your own password
2. ✅ BCrypt handles encryption automatically  
3. ✅ You control the credentials
4. ✅ Easy role upgrade in database

---

## Alternative: Register Multiple Users

You can register as many users as you need:

```
Doctor Account:
- Email: dr.yourname@example.com
- UPDATE users SET role = 'DOCTOR' WHERE email = 'dr.yourname@example.com';

Patient Account:
- Email: patient@example.com
- Already PATIENT role (no update needed)
```

---

**This is the easiest way to get started! No seed data issues, no password hash problems.** 🎉
