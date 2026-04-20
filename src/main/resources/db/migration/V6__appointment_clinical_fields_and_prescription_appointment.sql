-- Clinical fields on appointments (doctor-entered; admin emails final summary)
ALTER TABLE appointments
    ADD COLUMN diagnosis TEXT NULL AFTER doctor_notes,
    ADD COLUMN follow_up_instructions TEXT NULL AFTER diagnosis;

-- Link prescriptions to a specific visit (optional FK for legacy rows)
ALTER TABLE prescriptions
    ADD COLUMN appointment_id BIGINT NULL AFTER patient_id,
    ADD INDEX idx_prescriptions_appointment_id (appointment_id),
    ADD CONSTRAINT fk_prescriptions_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL;

-- Allow prescriptions without a medical report record (doctors no longer create reports)
ALTER TABLE prescriptions
    MODIFY COLUMN report_id BIGINT NULL;
