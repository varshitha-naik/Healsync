-- ========================================
-- HealSync Platform - Additional Indexes for Performance
-- ========================================


-- Composite index for appointment queries
CREATE INDEX idx_appointment_doctor_date ON appointments(doctor_id, start_date_time);
CREATE INDEX idx_appointment_patient_date ON appointments(patient_id, start_date_time);

-- Index for medical report queries
CREATE INDEX idx_medical_report_patient_date ON medical_reports(patient_id, created_at);
CREATE INDEX idx_medical_report_doctor_date ON medical_reports(doctor_id, created_at);

-- Index for prescription queries
CREATE INDEX idx_prescription_patient ON prescriptions(patient_id, created_at);
