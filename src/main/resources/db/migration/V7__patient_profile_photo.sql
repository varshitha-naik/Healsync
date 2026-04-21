ALTER TABLE patient_profiles
    ADD COLUMN profile_photo_url VARCHAR(500) NULL AFTER emergency_contact_phone;
