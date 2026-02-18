package com.healsync.controller;

import com.healsync.dto.AppointmentCancelRequest;
import com.healsync.dto.AppointmentDoctorNotesRequest;
import com.healsync.dto.AppointmentRequest;
import com.healsync.dto.AppointmentStatusRequest;
import com.healsync.entity.Appointment;
import com.healsync.enums.AppointmentStatus;
import com.healsync.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(@Valid @RequestBody AppointmentRequest request, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        LocalDateTime startDateTime = LocalDateTime.parse(request.getStart());
        LocalDateTime endDateTime = LocalDateTime.parse(request.getEnd());

        Appointment appointment = appointmentService.bookAppointment(
                request.getClinicId(),
                request.getDoctorId(),
                request.getPatientId(),
                startDateTime,
                endDateTime,
                request.getReason());

        return ResponseEntity.ok(appointment);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByDoctor(
            @PathVariable Long doctorId,
            @RequestParam(required = false) String status) {

        if (status != null && !status.isEmpty()) {
            try {
                AppointmentStatus apptStatus = AppointmentStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(appointmentService.getAppointmentsByDoctorAndStatus(doctorId, apptStatus));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid appointment status: " + status);
            }
        }
        return ResponseEntity.ok(appointmentService.getAppointmentsByDoctor(doctorId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByPatient(
            @PathVariable Long patientId,
            @RequestParam(required = false) String status) {

        if (status != null && !status.isEmpty()) {
            try {
                AppointmentStatus apptStatus = AppointmentStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(appointmentService.getAppointmentsByPatientAndStatus(patientId, apptStatus));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid appointment status: " + status);
            }
        }
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Appointment> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusRequest request) {

        try {
            AppointmentStatus status = AppointmentStatus.valueOf(request.getStatus().toUpperCase());
            return ResponseEntity.ok(appointmentService.updateStatus(id, status));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid appointment status: " + request.getStatus());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentCancelRequest request) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, request.getReason()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @PutMapping("/{id}/notes")
    public ResponseEntity<Appointment> updateDoctorNotes(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentDoctorNotesRequest request) {
        return ResponseEntity.ok(appointmentService.updateDoctorNotes(id, request.getDoctorNotes()));
    }
}
