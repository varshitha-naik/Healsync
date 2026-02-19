package com.healsync.service;

import com.healsync.entity.Appointment;
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
            Long doctorId,
            Long patientId,
            LocalDateTime start,
            LocalDateTime end,
            String reason) {
        // Check for overlapping appointments
        boolean overlap = appointmentRepository.existsOverlappingAppointment(doctorId, start, end);
        if (overlap) {
            throw new RuntimeException("Doctor has an overlapping appointment at this time");
        }

        // Fetch patient profile using userId (passed as patientId)
        PatientProfile profile = patientProfileRepository.findByUserId(patientId)
                .orElseThrow(() -> new RuntimeException("Patient profile not found for user ID: " + patientId));

        // Create new appointment
        Appointment appointment = new Appointment();
        appointment.setClinicId(clinicId);
        appointment.setDoctorId(doctorId);
        appointment.setPatientId(profile.getId());
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(end);
        appointment.setReason(reason);
        appointment.setStatus(AppointmentStatus.REQUESTED);

        Appointment saved = appointmentRepository.save(appointment);
        populateNames(saved);

        // Send Email Notification
        try {
            String patientEmail = userRepository.findById(appointment.getPatientId()) // UserID !
                    .map(u -> u.getEmail())
                    .orElse(null);

            // Note: In datamodel, appointment.patientId is actually ProfileId or UserId?
            // Checking bookAppointment: appointment.setPatientId(profile.getId()); -> It
            // uses Profile ID!
            // Wait. Appointment.patientId store logic:
            // profile = patientProfileRepository.findByUserId(patientId)
            // appointment.setPatientId(profile.getId());
            // This means Appointment stores PROFILE ID.

            // CORRECTION: I need to get User from Profile.
            // But PatientProfile has userId.
            // profile already fetched above.

            String pEmail = userRepository.findById(profile.getUserId()).map(u -> u.getEmail()).orElse(null);

            // Doctor Email
            // Need doctor profile to get userId
            var docProfile = doctorProfileRepository.findByUserId(doctorId).orElse(null);
            // wait, bookAppointment takes doctorId. Is it ProfileID or UserID?
            // "Long doctorId" parameter.
            // looking at DoctorController, it usually passes UserID.
            // Check usage: appointmentRepository.existsOverlappingAppointment(doctorId,
            // ...)
            // doctorProfileRepository.findByUserId(doctorId) -> This implies doctorId IS
            // UserId.

            String dEmail = null;
            String dName = "Doctor";
            if (docProfile != null) {
                dEmail = userRepository.findById(docProfile.getUserId()).map(u -> u.getEmail()).orElse(null);
                dName = docProfile.getFullName();
            } else {
                // Try to fetch profile assuming doctorId is UserId
                docProfile = doctorProfileRepository.findByUserId(doctorId).orElse(null);
                if (docProfile != null) {
                    dName = docProfile.getFullName();
                    dEmail = userRepository.findById(docProfile.getUserId()).map(u -> u.getEmail()).orElse(null);
                }
            }

            if (pEmail != null) {
                emailService.sendAppointmentRequested(pEmail, profile.getFullName(), dName, start.toString());
            }
        } catch (Exception e) {
            log.error("Failed to send appointment email", e);
        }

        return saved;
    }

    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        populateNames(appointments);
        return appointments;
    }

    public List<Appointment> getAppointmentsByDoctorAndStatus(Long doctorId, AppointmentStatus status) {
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndStatus(doctorId, status);
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        populateNames(appointments);
        return appointments;
    }

    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        populateNames(appointments);
        return appointments;
    }

    public List<Appointment> getAppointmentsByPatientAndStatus(Long patientId, AppointmentStatus status) {
        List<Appointment> appointments = appointmentRepository.findByPatientIdAndStatus(patientId, status);
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
        // Resolve Patient
        var patientProfile = patientProfileRepository.findById(appointment.getPatientId()).orElse(null);
        if (patientProfile == null)
            return;

        var patientUser = userRepository.findById(patientProfile.getUserId()).orElse(null);
        if (patientUser == null)
            return;

        // Resolve Doctor
        // Appointment stores doctorId. Based on bookAppointment logic, this seems to be
        // passed directly.
        // Assuming doctorId in Appointment is USER ID (based on
        // doctorProfileRepository.findByUserId call in populateNames)
        var doctorProfile = doctorProfileRepository.findByUserId(appointment.getDoctorId()).orElse(null);
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

        // Populate Doctor Name (Using userId which is likely doctorId in Appointment)
        doctorProfileRepository.findByUserId(appointment.getDoctorId())
                .ifPresent(p -> appointment.setDoctorName(p.getFullName()));

        // Populate Patient Name (Using profileId which is in patientId)
        patientProfileRepository.findById(appointment.getPatientId())
                .ifPresent(p -> appointment.setPatientName(p.getFullName()));
    }

    private void populateNames(List<Appointment> appointments) {
        if (appointments != null) {
            appointments.forEach(this::populateNames);
        }
    }
}
