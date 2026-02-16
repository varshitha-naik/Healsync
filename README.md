# 🏥 HealSync Platform - Doctor & Patient Management System

A production-ready, full-stack healthcare platform built with **Spring Boot 3**, **MySQL**, **JWT Authentication**, **Flyway Migrations**, and **Thymeleaf**.

## 🌟 Features

### ✅ Real-World Healthcare Workflows
- **Patient Registration & Login** with JWT authentication
- **Doctor Management** with profile photos and specializations
- **Appointment Booking System** with time slot management
- **Medical Reports** with file attachments
- **Digital Prescriptions** with structured data
- **Email Notifications** via Mailtrap
- **Role-Based Access Control** (SUPER_ADMIN, ADMIN, DOCTOR, PATIENT)
- **Audit Logging** for compliance

### ✅ Technical Highlights
- **12 Entity Models** with proper JPA relationships
- **Flyway Database Migrations** for version-controlled schema
- **JWT Security** with BCrypt password hashing
- **File Upload/Storage** on disk (doctor photos, report attachments)
- **Email Integration** with Mailtrap SMTP
- **Modern Responsive UI** with Bootstrap/Tailwind
- **Input Validation** with Jakarta Validation
- **Edge Case Handling** (double booking, authorization, etc.)

---

## 📋 Prerequisites

Before running this application, ensure you have:

1. **Java 17** or higher
2. **Maven 3.6+**
3. **MySQL 8.0+** (running on `localhost:3306`)
4. **Mailtrap Account** (for email notifications) - [Sign up free](https://mailtrap.io/)

---

## 🚀 Quick Start

### 1. Clone or Extract the Project

```bash
cd c:/Users/shaik/OneDrive/Desktop/HealSync
```

### 2. Configure MySQL Database

Create a MySQL database (or let the app create it automatically):

```sql
CREATE DATABASE healsync_db;
```

**Update Database Credentials** (if needed) in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/healsync_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
```

### 3. Configure Mailtrap (Email Notifications)

1. Sign up at [mailtrap.io](https://mailtrap.io/)
2. Get your SMTP credentials from the **Inbox Settings**
3. Update `application.properties`:

```properties
spring.mail.username=your_mailtrap_username
spring.mail.password=your_mailtrap_password
```

### 4. Build and Run

```bash
# Clean and build
mvn clean install

# Run the application
mvn spring-boot:run
```

**Alternative**: Run from your IDE:
- Open the project in IntelliJ IDEA or Eclipse
- Run `HealSyncApplication.java`

The application will start on **http://localhost:8080**

---

## 👥 Seed User Credentials

The application comes with pre-seeded users for immediate testing:

| Role | Email | Password | Purpose |
|------|-------|----------|---------|
| **ADMIN** | `admin@healsync.com` | `Password123!` | Manage doctors, view patients |
| **DOCTOR** | `dr.smith@healsync.com` | `Password123!` | Dr. Emily Smith (Cardiology) |
| **DOCTOR** | `dr.johnson@healsync.com` | `Password123!` | Dr. Michael Johnson (Orthopedics) |
| **PATIENT** | `patient.john@example.com` | `Password123!` | John Doe |
| **PATIENT** | `patient.sarah@example.com` | `Password123!` | Sarah Williams |

---

## 🗂️ Project Structure

```
HealSync/
├── src/main/java/com/healsync/
│   ├── entity/              # JPA Entities (User, Doctor, Patient, etc.)
│   ├── repository/          # Spring Data JPA Repositories
│   ├── service/             # Business Logic Layer
│   ├── controller/          # REST API Controllers
│   ├── dto/                 # Data Transfer Objects
│   ├── security/            # JWT, Security Config, Filters
│   ├── config/              # Application Configuration
│   ├── enums/               # Enums (Role, Status, etc.)
│   └── HealSyncApplication.java
├── src/main/resources/
│   ├── application.properties
│   ├── db/migration/        # Flyway SQL Migrations
│   │   ├── V1__init_schema.sql
│   │   ├── V2__seed_data.sql
│   │   └── V3__indexes.sql
│   ├── templates/           # Thymeleaf Templates
│   │   ├── login.html
│   │   ├── admin/
│   │   ├── doctor/
│   │   └── patient/
│   └── static/              # CSS, JS, Images
├── uploads/                 # File Storage Directory
│   ├── doctors/             # Doctor profile photos
│   └── reports/             # Medical report attachments
├── pom.xml
└── README.md
```

---

## 🔐 API Endpoints

### **Authentication**

```http
POST /api/auth/register
POST /api/auth/login
```

### **Admin Endpoints** (Requires `ROLE_ADMIN`)

```http
GET  /api/admin/doctors
POST /api/admin/doctors
GET  /api/admin/patients
```

### **Doctor Endpoints** (Requires `ROLE_DOCTOR`)

```http
GET    /api/doctor/profile
PUT    /api/doctor/profile
POST   /api/doctor/profile/photo
GET    /api/doctor/appointments
PUT    /api/doctor/appointments/{id}/status
POST   /api/doctor/reports
POST   /api/doctor/reports/{id}/attachments
POST   /api/doctor/prescriptions
```

### **Patient Endpoints** (Requires `ROLE_PATIENT`)

```http
GET    /api/patient/profile
PUT    /api/patient/profile
GET    /api/patient/doctors
POST   /api/patient/appointments
GET    /api/patient/appointments
GET    /api/patient/reports
GET    /api/patient/prescriptions
```

### **Public Endpoints**

```http
GET /public/doctors/{doctorId}/photo
```

---

## 🎨 UI Access

### **Web Pages** (Thymeleaf)

- **Login Page**: http://localhost:8080/login
- **Admin Dashboard**: http://localhost:8080/admin/dashboard
- **Doctor Dashboard**: http://localhost:8080/doctor/dashboard
- **Patient Dashboard**: http://localhost:8080/patient/dashboard

**Note**: The UI is secured with role-based access. Log in with the appropriate credentials to access each portal.

---

## 🗄️ Database Schema

The application uses **12 normalized tables**:

1. **users** - User accounts with roles
2. **clinics** - Clinic information
3. **doctor_profiles** - Doctor professional details
4. **patient_profiles** - Patient personal information
5. **appointment_slots** - Doctor availability
6. **appointments** - Appointment bookings
7. **medical_reports** - Medical reports
8. **report_attachments** - Report file attachments
9. **prescriptions** - Prescription headers
10. **prescription_items** - Individual medicines
11. **notifications** - User notifications
12. **audit_logs** - System audit trail

**Migrations are automatic** via Flyway on application startup.

---

## 📧 Email Notifications

The system sends emails for:

- ✉️ Appointment requested
- ✉️ Appointment confirmed/cancelled
- ✉️ Medical report uploaded
- ✉️ Prescription created

**Check your Mailtrap inbox** to view sent emails.

---

## 🔒 Security Features

### ✅ Implemented Security

- **BCrypt Password Hashing** (10 rounds)
- **JWT Access Tokens** (24-hour expiration)
- **Role-Based Authorization** (`@PreAuthorize`)
- **Data Isolation** (patients can't see others' data)
- **CORS Configuration** (if needed for frontend)
- **SQL Injection Protection** (JPA Parameterized Queries)
- **Input Validation** (Jakarta Bean Validation)

### ✅ Edge Cases Handled

- ❌ **Double Booking Prevention** - Returns `409 Conflict`
- ❌ **Booking Outside Slots** - Returns `400 Bad Request`
- ❌ **Unauthorized Data Access** - Returns `403 Forbidden`
- ❌ **Invalid/Expired JWT** - Returns `401 Unauthorized`
- ❌ **Validation Errors** - Returns `400` with field-level errors

---

## 💾 File Storage

### **Upload Directories**

Files are stored in `./uploads/`:

```
uploads/
├── doctors/            # Doctor profile photos (max 2MB)
│   └── {uuid}.jpg
└── reports/            # Medical report attachments (max 10MB)
    └── {uuid}.pdf
```

### **Supported Formats**

- **Doctor Photos**: JPG, PNG
- **Report Attachments**: PDF, JPG, PNG

**URLs are stored in the database**, not file bytes.

---

## 🧪 Testing the Application

### 1. **Login as Admin**

```
Email: admin@healsync.com
Password: Password123!
```

- Create a new doctor account
- View all doctors and patients

### 2. **Login as Doctor**

```
Email: dr.smith@healsync.com
Password: Password123!
```

- Upload profile photo
- View appointments
- Create medical reports
- Write prescriptions

### 3. **Login as Patient**

```
Email: patient.john@example.com
Password: Password123!
```

- Browse doctors (with photos)
- Book an appointment
- View medical reports
- View prescriptions

---

## 🛠️ Development

### **Hot Reload**

Spring Boot DevTools is included for hot reload during development.

### **Database Reset**

To reset the database and re-run migrations:

```sql
DROP DATABASE healsync_db;
CREATE DATABASE healsync_db;
```

Then restart the application.

### **Flyway Clean** (Development Only)

```bash
mvn flyway:clean
mvn flyway:migrate
```

---

## 🐛 Troubleshooting

### **Issue: Database Connection Failed**

- Ensure MySQL is running on `localhost:3306`
- Verify database credentials in `application.properties`
- Check if the database `healsync_db` exists

### **Issue: Flyway Migration Failed**

- Check SQL syntax in migration files
- Ensure migrations are in `src/main/resources/db/migration/`
- Use naming convention: `V1__description.sql`

### **Issue: JWT Token Invalid**

- Ensure the JWT secret is 256-bit (64 hex characters)
- Check if the token has expired (24-hour default)
- Verify the `Authorization: Bearer <token>` header format

### **Issue: File Upload Failed**

- Ensure the `uploads/` directory exists (created automatically)
- Check file size limits in `application.properties`
- Verify file permissions

---

## 📚 Technologies Used

| Category | Technology |
|----------|------------|
| **Backend** | Spring Boot 3.2, Java 17 |
| **Security** | Spring Security, JWT (jjwt 0.12.3) |
| **Database** | MySQL 8.0, Flyway, JPA/Hibernate |
| **Frontend** | Thymeleaf, Bootstrap 5/Tailwind CSS |
| **Email** | Spring Mail, Mailtrap SMTP |
| **Build Tool** | Maven |
| **Dev Tools** | Lombok, Spring DevTools |

---

## 📄 License

This project is built for educational and portfolio purposes.

---

## 👨‍💻 Author

Built by a senior full-stack engineer as a production-ready healthcare platform demonstration.

---

## 🎯 Next Steps

1. ✅ Run the application
2. ✅ Test with seed users
3. ✅ Explore REST APIs with Postman
4. ✅ Check Mailtrap for email notifications
5. ✅ Upload doctor profile photos
6. ✅ Book and manage appointments

**Enjoy building with HealSync! 🏥**
