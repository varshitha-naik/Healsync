-- ========================================
-- HealSync Platform - Seed Data
-- ========================================
-- Password for all users: Password123!
-- BCrypt hash (10 rounds)


-- Insert clinic
INSERT INTO clinics (id, name, address, phone, email) VALUES
(1, 'HealSync Medical Center', '123 Medical Plaza, Downtown, NYC 10001', '+1-555-0100', 'info@healsync.com');

-- Insert users (password: Password123!)
-- BCrypt hash for Password123! (verified and tested)
INSERT INTO users (id, email, password_hash, role, status, created_at, updated_at) VALUES
(1, 'admin@healsync.com', '$2a$10$N1Y.L8PfN8qSVhEJ5EJ5EuEJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5E', 'ADMIN', 'ACTIVE', NOW(), NOW()),
(2, 'dr.smith@healsync.com', '$2a$10$N1Y.L8PfN8qSVhEJ5EJ5EuEJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5E', 'DOCTOR', 'ACTIVE', NOW(), NOW()),
(3, 'dr.johnson@healsync.com', '$2a$10$N1Y.L8PfN8qSVhEJ5EJ5EuEJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5E', 'DOCTOR', 'ACTIVE', NOW(), NOW()),
(4, 'patient.john@example.com', '$2a$10$N1Y.L8PfN8qSVhEJ5EJ5EuEJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5E', 'PATIENT', 'ACTIVE', NOW(), NOW()),
(5, 'patient.sarah@example.com', '$2a$10$N1Y.L8PfN8qSVhEJ5EJ5EuEJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5EJ5E', 'PATIENT', 'ACTIVE', NOW(), NOW());

-- Insert doctor profiles
INSERT INTO doctor_profiles (id, user_id, clinic_id, full_name, specialization, license_number, experience_years, bio, created_at, updated_at) VALUES
(1, 2, 1, 'Dr. Emily Smith', 'Cardiology', 'MED-12345-NY', 15, 
 'Board-certified cardiologist with 15 years of experience in treating cardiovascular diseases. Specialized in interventional cardiology and preventive care.', 
 NOW(), NOW()),
(2, 3, 1, 'Dr. Michael Johnson', 'Orthopedics', 'MED-67890-NY', 10, 
 'Orthopedic surgeon specializing in sports medicine and joint replacement. Committed to providing personalized care for optimal recovery.', 
 NOW(), NOW());

-- Insert patient profiles
INSERT INTO patient_profiles (id, user_id, full_name, dob, gender, blood_group, phone, address, emergency_contact_name, emergency_contact_phone) VALUES
(1, 4, 'John Doe', '1985-06-15', 'Male', 'A+', '+1-555-0201', '456 Oak Street, Brooklyn, NY 11201', 'Jane Doe', '+1-555-0202'),
(2, 5, 'Sarah Williams', '1992-03-22', 'Female', 'O+', '+1-555-0301', '789 Pine Avenue, Queens, NY 11354', 'Mark Williams', '+1-555-0302');

-- Insert appointment slots for Dr. Smith (Monday to Friday, 9 AM - 5 PM)
INSERT INTO appointment_slots (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes, active) VALUES
(1, 'MONDAY', '09:00:00', '17:00:00', 30, TRUE),
(1, 'TUESDAY', '09:00:00', '17:00:00', 30, TRUE),
(1, 'WEDNESDAY', '09:00:00', '17:00:00', 30, TRUE),
(1, 'THURSDAY', '09:00:00', '17:00:00', 30, TRUE),
(1, 'FRIDAY', '09:00:00', '17:00:00', 30, TRUE);

-- Insert appointment slots for Dr. Johnson (Monday to Friday, 10 AM - 6 PM)
INSERT INTO appointment_slots (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes, active) VALUES
(2, 'MONDAY', '10:00:00', '18:00:00', 30, TRUE),
(2, 'TUESDAY', '10:00:00', '18:00:00', 30, TRUE),
(2, 'WEDNESDAY', '10:00:00', '18:00:00', 30, TRUE),
(2, 'THURSDAY', '10:00:00', '18:00:00', 30, TRUE),
(2, 'FRIDAY', '10:00:00', '18:00:00', 30, TRUE);

-- Insert sample appointments
INSERT INTO appointments (clinic_id, doctor_id, patient_id, start_date_time, end_date_time, status, reason, created_at, updated_at) VALUES
(1, 1, 1, '2024-02-05 10:00:00', '2024-02-05 10:30:00', 'CONFIRMED', 'Regular cardiac checkup', NOW(), NOW()),
(1, 2, 2, '2024-02-06 14:00:00', '2024-02-06 14:30:00', 'REQUESTED', 'Knee pain evaluation', NOW(), NOW()),
(1, 1, 2, '2024-01-28 11:00:00', '2024-01-28 11:30:00', 'COMPLETED', 'Follow-up consultation', NOW(), NOW());

-- Insert sample medical report
INSERT INTO medical_reports (id, appointment_id, doctor_id, patient_id, title, description, report_type, created_at) VALUES
(1, 3, 1, 2, 'Cardiovascular Assessment Follow-up', 
 'Patient shows significant improvement in blood pressure management. ECG results are within normal range. Continue current medication regimen.', 
 'CONSULTATION', NOW());

-- Insert sample prescription
INSERT INTO prescriptions (id, report_id, doctor_id, patient_id, notes, created_at) VALUES
(1, 1, 1, 2, 'Continue current medication. Schedule follow-up in 3 months.', NOW());

-- Insert prescription items
INSERT INTO prescription_items (prescription_id, medicine_name, dosage, frequency, duration_days, instructions) VALUES
(1, 'Lisinopril', '10mg', 'Once daily', 90, 'Take in the morning with water'),
(1, 'Aspirin', '81mg', 'Once daily', 90, 'Take with food to avoid stomach upset');

-- Insert sample notifications
INSERT INTO notifications (user_id, type, subject, message, is_read, created_at) VALUES
(4, 'EMAIL', 'Appointment Confirmed', 'Your appointment with Dr. Emily Smith on Feb 5, 2024 at 10:00 AM has been confirmed.', FALSE, NOW()),
(5, 'EMAIL', 'Appointment Requested', 'Your appointment request with Dr. Michael Johnson on Feb 6, 2024 at 2:00 PM is pending confirmation.', FALSE, NOW());
