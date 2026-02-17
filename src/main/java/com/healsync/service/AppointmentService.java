package com.healsync.service;

import com.healsync.entity.Appointment;
import com.healsync.enums.AppointmentStatus;
import com.healsync.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

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

        // Create new appointment
        Appointment appointment = new Appointment();
        appointment.setClinicId(clinicId);
        appointment.setDoctorId(doctorId);
        appointment.setPatientId(patientId);
        appointment.setStartDateTime(start);
        appointment.setEndDateTime(end);
        appointment.setReason(reason);
        appointment.setStatus(AppointmentStatus.REQUESTED);

        return appointmentRepository.save(appointment);
    }
}
