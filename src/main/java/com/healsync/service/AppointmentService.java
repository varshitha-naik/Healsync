package com.healsync.service;

import com.healsync.entity.Appointment;
import com.healsync.entity.PatientProfile;
import com.healsync.enums.AppointmentStatus;
import com.healsync.repository.AppointmentRepository;
import com.healsync.repository.PatientProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;

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

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        return appointments;
    }

    public List<Appointment> getAppointmentsByDoctorAndStatus(Long doctorId, AppointmentStatus status) {
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndStatus(doctorId, status);
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        return appointments;
    }

    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        return appointments;
    }

    public List<Appointment> getAppointmentsByPatientAndStatus(Long patientId, AppointmentStatus status) {
        List<Appointment> appointments = appointmentRepository.findByPatientIdAndStatus(patientId, status);
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
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
        return appointmentRepository.save(appointment);
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
        return appointmentRepository.save(appointment);
    }

    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + id));
    }

    @Transactional
    public Appointment updateDoctorNotes(Long appointmentId, String notes) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        appointment.setDoctorNotes(notes);
        return appointmentRepository.save(appointment);
    }
}
