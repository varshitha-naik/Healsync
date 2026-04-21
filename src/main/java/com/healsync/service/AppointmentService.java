package com.healsync.service;

import com.healsync.entity.Appointment;
import com.healsync.entity.DoctorProfile;
import com.healsync.entity.MedicalSummaryReport;
import com.healsync.entity.PatientProfile;
import com.healsync.enums.AppointmentReviewStatus;
import com.healsync.enums.AppointmentStatus;
import com.healsync.enums.UserRole;
import com.healsync.repository.AppointmentRepository;
import com.healsync.repository.DoctorLeaveRepository;
import com.healsync.repository.DoctorProfileRepository;
import com.healsync.repository.MedicalSummaryReportRepository;
import com.healsync.repository.PatientProfileRepository;
import com.healsync.repository.PrescriptionItemRepository;
import com.healsync.repository.PrescriptionRepository;
import com.healsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorLeaveRepository doctorLeaveRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicalSummaryReportRepository medicalSummaryReportRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public Appointment bookAppointment(
            Long clinicId,
            Long doctorId, // This is User ID from frontend
            Long patientId,
            LocalDateTime start,
            LocalDateTime end,
            String reason,
            String specialization) {

        // Date-Time Validation
        if (start.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot book appointment in the past. Please select a future time slot.");
        }
        if (end.isBefore(start)) {
            throw new RuntimeException("End time must be after start time.");
        }
        if (!end.isAfter(start)) {
            throw new RuntimeException("Invalid appointment duration.");
        }

        Long finalDoctorProfileId = null;

        // Smart Doctor Assignment based on Specialization
        if (doctorId == null) {
            if (specialization == null || specialization.isBlank()) {
                throw new RuntimeException("Either a Doctor or Specialization must be selected.");
            }

            // Find all doctors with specialization
            List<com.healsync.entity.DoctorProfile> doctors = doctorProfileRepository
                    .findBySpecialization(specialization);

            for (com.healsync.entity.DoctorProfile doc : doctors) {
                boolean onLeave = doctorLeaveRepository.existsByDoctorIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        doc.getUserId(), start.toLocalDate(), start.toLocalDate());
                if (onLeave) {
                    continue;
                }
                // Check availability using Profile ID
                boolean overlap = appointmentRepository.existsOverlappingAppointment(doc.getId(), start, end);
                if (!overlap) {
                    finalDoctorProfileId = doc.getId(); // Assign Profile ID
                    break;
                }
            }

            if (finalDoctorProfileId == null) {
                throw new RuntimeException("No doctor available for selected specialization: " + specialization);
            }
        } else {
            // Specific doctor requested (doctorId is USER ID from frontend)
            // Must resolve to Profile ID
            com.healsync.entity.DoctorProfile docProfile = doctorProfileRepository.findByUserId(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor profile not found for user ID: " + doctorId));

            boolean onLeave = doctorLeaveRepository.existsByDoctorIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                    doctorId, start.toLocalDate(), start.toLocalDate());
            if (onLeave) {
                throw new RuntimeException("Doctor is on leave for the selected date.");
            }

            finalDoctorProfileId = docProfile.getId(); // Assign Profile ID

            // Check availability
            boolean overlap = appointmentRepository.existsOverlappingAppointment(finalDoctorProfileId, start, end);
            if (overlap) {
                throw new RuntimeException("Doctor has an overlapping appointment at this time");
            }
        }

        // Fetch patient profile using userId (passed as patientId)
        PatientProfile profile = patientProfileRepository.findByUserId(patientId)
                .orElseThrow(() -> new RuntimeException("Patient profile not found for user ID: " + patientId));

        // Create new appointment
        Appointment appointment = new Appointment();
        appointment.setClinicId(clinicId);
        appointment.setDoctorId(finalDoctorProfileId); // Set DoctorProfile ID
        appointment.setPatientId(profile.getId());
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(end);
        appointment.setReason(reason);
        appointment.setStatus(AppointmentStatus.REQUESTED);

        Appointment saved = appointmentRepository.save(appointment);
        populateNames(saved);

        // Send Email Notification
        try {
            String pEmail = userRepository.findById(profile.getUserId()).map(u -> u.getEmail()).orElse(null);

            // Fetch assigned doctor details for email
            // Use findById because finalDoctorProfileId is Profile ID
            var docProfile = doctorProfileRepository.findById(finalDoctorProfileId).orElse(null);
            String dName = (docProfile != null) ? docProfile.getFullName() : "Doctor";

            if (pEmail != null) {
                emailService.sendAppointmentRequested(pEmail, profile.getFullName(), dName, start.toString());
            }
        } catch (Exception e) {
            log.error("Failed to send appointment email", e);
        }

        return saved;
    }

    public List<Appointment> getAppointmentsByDoctor(Long userId) {
        // Resolve Profile ID from User ID
        Long profileId = doctorProfileRepository.findByUserId(userId)
                .map(DoctorProfile::getId)
                .orElse(null);

        if (profileId == null) {
            return List.of(); // or throw exception
        }

        List<Appointment> appointments = new ArrayList<>(appointmentRepository.findByDoctorId(profileId));
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        populateNames(appointments);
        return appointments;
    }

    public List<Appointment> getAppointmentsByDoctorAndStatus(Long userId, AppointmentStatus status) {
        // Resolve Profile ID from User ID
        Long profileId = doctorProfileRepository.findByUserId(userId)
                .map(DoctorProfile::getId)
                .orElse(null);

        if (profileId == null) {
            return List.of();
        }

        List<Appointment> appointments = new ArrayList<>(appointmentRepository.findByDoctorIdAndStatus(profileId, status));
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        populateNames(appointments);
        return appointments;
    }

    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        // patientId argument is actually the User ID. Resolve Profile ID first.
        Long profileId = resolvePatientProfileIdFromUserId(patientId);

        if (profileId == null) {
            return List.of();
        }

        List<Appointment> appointments = new ArrayList<>(appointmentRepository.findByPatientId(profileId));
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        populateNames(appointments);
        return appointments;
    }

    public List<Appointment> getAppointmentsByPatientAndStatus(Long patientId, AppointmentStatus status) {
        // patientId argument is actually the User ID. Resolve Profile ID first.
        Long profileId = resolvePatientProfileIdFromUserId(patientId);

        if (profileId == null) {
            return List.of();
        }

        List<Appointment> appointments = new ArrayList<>(appointmentRepository.findByPatientIdAndStatus(profileId, status));
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        populateNames(appointments);
        return appointments;
    }

    public List<Appointment> getCompletedAppointmentsByPatientUserId(Long userId) {
        return getAppointmentsByPatientAndStatus(userId, AppointmentStatus.COMPLETED);
    }

    public boolean isValidPatientUserId(Long userId) {
        return resolvePatientProfileIdFromUserId(userId) != null;
    }

    @Transactional
    public Appointment updateStatus(Long appointmentId, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
                appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new RuntimeException(
                    "Cannot update appointment status. Current status is: " + appointment.getStatus());
        }

        appointment.setStatus(status);
        Appointment saved = appointmentRepository.save(appointment);
        populateNames(saved);

        // Send Email if Confirmed
        if (status == AppointmentStatus.CONFIRMED) {
            try {
                sendEmailNotification(saved, "CONFIRMED");
            } catch (Exception e) {
                log.error("Failed to send verification email", e);
            }
        }

        return saved;
    }

    @Transactional
    public Appointment updateStatusForDoctor(Long appointmentId, AppointmentStatus status, Long doctorUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));
        validateDoctorOwnsAppointment(appointment, doctorUserId);
        if (status == AppointmentStatus.COMPLETED) {
            validateCompletionDocumentation(appointment);
        }
        return updateStatus(appointmentId, status);
    }

    @Transactional
    public Appointment cancelAppointment(Long appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
                appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel appointment. Current status is: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);
        Appointment saved = appointmentRepository.save(appointment);
        populateNames(saved);

        // Send Cancellation Email
        try {
            sendEmailNotification(saved, "CANCELLED");
        } catch (Exception e) {
            log.error("Failed to send cancellation email", e);
        }

        return saved;
    }

    @Transactional
    public Appointment cancelAppointmentForDoctor(Long appointmentId, String reason, Long doctorUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));
        validateDoctorOwnsAppointment(appointment, doctorUserId);
        return cancelAppointment(appointmentId, reason);
    }

    private void sendEmailNotification(Appointment appointment, String type) {
        // Resolve Patient (patientId is Profile ID)
        var patientProfile = patientProfileRepository.findById(appointment.getPatientId()).orElse(null);
        if (patientProfile == null)
            return;

        var patientUser = userRepository.findById(patientProfile.getUserId()).orElse(null);
        if (patientUser == null)
            return;

        // Resolve Doctor (doctorId is Profile ID)
        var doctorProfile = doctorProfileRepository.findById(appointment.getDoctorId()).orElse(null);
        String docName = (doctorProfile != null) ? doctorProfile.getFullName() : "Doctor";

        if ("CONFIRMED".equals(type)) {
            emailService.sendAppointmentConfirmed(
                    patientUser.getEmail(),
                    patientProfile.getFullName(),
                    docName,
                    appointment.getStartDateTime().toString());
        } else if ("CANCELLED".equals(type)) {
            emailService.sendAppointmentCancelled(
                    patientUser.getEmail(),
                    patientProfile.getFullName(),
                    docName,
                    appointment.getStartDateTime().toString(),
                    appointment.getCancellationReason());
        }
    }

    public Appointment getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + id));

        populateNames(appointment);
        return appointment;
    }

    @Transactional
    public Appointment updateClinicalFields(
            Long appointmentId, String diagnosis, String clinicalNotes, String followUpInstructions) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));
        appointment.setDiagnosis(trimToNull(diagnosis));
        appointment.setDoctorNotes(trimToNull(clinicalNotes));
        appointment.setFollowUpInstructions(trimToNull(followUpInstructions));
        Appointment saved = appointmentRepository.save(appointment);
        populateNames(saved);
        return saved;
    }

    @Transactional
    public Appointment updateClinicalFieldsForDoctor(
            Long appointmentId, String diagnosis, String clinicalNotes, String followUpInstructions, Long doctorUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));
        validateDoctorOwnsAppointment(appointment, doctorUserId);
        return updateClinicalFields(appointmentId, diagnosis, clinicalNotes, followUpInstructions);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private void populateNames(Appointment appointment) {
        if (appointment == null)
            return;

        // Populate Doctor Name (Using Profile ID)
        doctorProfileRepository.findById(appointment.getDoctorId())
                .ifPresent(p -> appointment.setDoctorName(p.getFullName()));

        // Populate Patient Name (Using Profile ID)
        patientProfileRepository.findById(appointment.getPatientId())
                .ifPresent(p -> appointment.setPatientName(p.getFullName()));

        applyReviewStatus(appointment);
    }

    private void populateNames(List<Appointment> appointments) {
        if (appointments != null) {
            appointments.forEach(this::populateNames);
        }
    }

    private Long resolvePatientProfileIdFromUserId(Long userId) {
        if (userId == null) {
            return null;
        }

        boolean isPatientUser = userRepository.findById(userId)
                .map(user -> user.getRole() == UserRole.PATIENT)
                .orElse(false);

        if (!isPatientUser) {
            return null;
        }

        return patientProfileRepository.findByUserId(userId)
                .map(PatientProfile::getId)
                .orElse(null);
    }

    private void validateDoctorOwnsAppointment(Appointment appointment, Long doctorUserId) {
        Long doctorProfileId = doctorProfileRepository.findByUserId(doctorUserId)
                .map(DoctorProfile::getId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        if (appointment.getDoctorId() == null || !appointment.getDoctorId().equals(doctorProfileId)) {
            throw new RuntimeException("Unauthorized: appointment does not belong to this doctor");
        }
    }

    private void validateCompletionDocumentation(Appointment appointment) {
        boolean hasDiagnosis = appointment.getDiagnosis() != null && !appointment.getDiagnosis().trim().isEmpty();
        boolean hasClinicalNotes = appointment.getDoctorNotes() != null && !appointment.getDoctorNotes().trim().isEmpty();
        boolean hasPrescription = prescriptionRepository.existsByAppointmentId(appointment.getId());
        if (!hasDiagnosis || !hasClinicalNotes || !hasPrescription) {
            throw new RuntimeException(
                    "Complete diagnosis, notes and prescription before this appointment can be reviewed by admin.");
        }
    }

    private void applyReviewStatus(Appointment appointment) {
        boolean hasDiagnosis = appointment.getDiagnosis() != null && !appointment.getDiagnosis().trim().isEmpty();
        boolean hasClinicalNotes = appointment.getDoctorNotes() != null && !appointment.getDoctorNotes().trim().isEmpty();
        boolean hasPrescription = appointment.getId() != null && prescriptionRepository.existsByAppointmentId(appointment.getId());
        appointment.setHasPrescriptionForAppointment(hasPrescription);
        List<String> missing = new ArrayList<>();
        if (!hasDiagnosis) {
            missing.add("diagnosis");
        }
        if (!hasClinicalNotes) {
            missing.add("clinicalNotes");
        }
        if (!hasPrescription) {
            missing.add("prescription");
        }
        appointment.setMissingDocumentation(missing);

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            appointment.setReviewStatus(AppointmentReviewStatus.NOT_READY);
            return;
        }
        MedicalSummaryReport summary = appointment.getId() != null
                ? medicalSummaryReportRepository.findByAppointmentId(appointment.getId()).orElse(null)
                : null;
        if (summary != null && summary.getEmailedAt() != null) {
            appointment.setReviewStatus(AppointmentReviewStatus.SUMMARY_SENT);
            return;
        }
        appointment.setReviewStatus(
                hasDiagnosis && hasClinicalNotes && hasPrescription
                        ? AppointmentReviewStatus.READY_FOR_ADMIN
                        : AppointmentReviewStatus.NOT_READY);
        if (appointment.getReviewStatus() != AppointmentReviewStatus.NOT_READY) {
            appointment.setMissingDocumentation(List.of());
        }
    }

    public java.util.Map<String, Object> getAdminPatientHistory(Long patientUserId, Long doctorUserId) {
        PatientProfile patient = patientProfileRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));
        DoctorProfile doctor = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        String patientEmail = userRepository.findById(patientUserId).map(u -> u.getEmail()).orElse("");

        List<Appointment> appointments = appointmentRepository.findByPatientId(patient.getId()).stream()
                .filter(a -> a.getDoctorId() != null && a.getDoctorId().equals(doctor.getId()))
                .sorted(java.util.Comparator.comparing(Appointment::getStartDateTime, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
        List<java.util.Map<String, Object>> appointmentRows = new java.util.ArrayList<>();

        for (Appointment appointment : appointments) {
            populateNames(appointment);
            var prescriptions = new java.util.ArrayList<java.util.Map<String, Object>>();
            prescriptionRepository.findByAppointmentId(appointment.getId()).forEach(p -> {
                var row = new java.util.LinkedHashMap<String, Object>();
                row.put("id", p.getId());
                row.put("notes", p.getNotes());
                row.put("items", prescriptionItemRepository.findByPrescriptionId(p.getId()));
                prescriptions.add(row);
            });

            MedicalSummaryReport summary = medicalSummaryReportRepository.findByAppointmentId(appointment.getId()).orElse(null);

            var row = new java.util.LinkedHashMap<String, Object>();
            row.put("id", appointment.getId());
            row.put("startDateTime", appointment.getStartDateTime());
            row.put("reason", appointment.getReason());
            row.put("status", appointment.getStatus() != null ? appointment.getStatus().name() : null);
            row.put("diagnosis", appointment.getDiagnosis());
            row.put("clinicalNotes", appointment.getDoctorNotes());
            row.put("followUpInstructions", appointment.getFollowUpInstructions());
            row.put("reviewStatus", appointment.getReviewStatus() != null ? appointment.getReviewStatus().name() : AppointmentReviewStatus.NOT_READY.name());
            row.put("summarySentAt", summary != null ? summary.getEmailedAt() : null);
            row.put("summaryText", summary != null ? summary.getGeneratedSummary() : null);
            row.put("prescriptions", prescriptions);
            appointmentRows.add(row);
        }

        var patientPayload = new java.util.LinkedHashMap<String, Object>();
        patientPayload.put("name", patient.getFullName());
        patientPayload.put("email", patientEmail);
        patientPayload.put("phone", patient.getPhone());
        patientPayload.put("dob", patient.getDob());
        patientPayload.put("gender", patient.getGender());
        patientPayload.put("bloodGroup", patient.getBloodGroup());

        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("patient", patientPayload);
        payload.put("appointments", appointmentRows);
        return payload;
    }
}
