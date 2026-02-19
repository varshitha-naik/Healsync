package com.healsync.service;

import com.healsync.entity.Appointment;
import com.healsync.entity.DoctorProfile;
import com.healsync.entity.PatientProfile;
import com.healsync.enums.AppointmentStatus;
import com.healsync.repository.AppointmentRepository;
import com.healsync.repository.DoctorProfileRepository;
import com.healsync.repository.PatientProfileRepository;
import com.healsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
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

        List<Appointment> appointments = appointmentRepository.findByDoctorId(profileId);
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

        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndStatus(profileId, status);
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        populateNames(appointments);
        return appointments;
    }

    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        // patientId argument is actually the User ID. Resolve Profile ID first.
        Long profileId = patientProfileRepository.findByUserId(patientId)
                .map(PatientProfile::getId)
                .orElse(null);

        if (profileId == null) {
            return List.of();
        }

        List<Appointment> appointments = appointmentRepository.findByPatientId(profileId);
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        populateNames(appointments);
        return appointments;
    }

    public List<Appointment> getAppointmentsByPatientAndStatus(Long patientId, AppointmentStatus status) {
        // patientId argument is actually the User ID. Resolve Profile ID first.
        Long profileId = patientProfileRepository.findByUserId(patientId)
                .map(PatientProfile::getId)
                .orElse(null);

        if (profileId == null) {
            return List.of();
        }

        List<Appointment> appointments = appointmentRepository.findByPatientIdAndStatus(profileId, status);
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        populateNames(appointments);
        return appointments;
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
    public Appointment updateDoctorNotes(Long appointmentId, String notes) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        appointment.setDoctorNotes(notes);
        Appointment saved = appointmentRepository.save(appointment);
        populateNames(saved);
        return saved;
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
    }

    private void populateNames(List<Appointment> appointments) {
        if (appointments != null) {
            appointments.forEach(this::populateNames);
        }
    }
}
