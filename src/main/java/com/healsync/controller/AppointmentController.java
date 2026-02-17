package com.healsync.controller;

import com.healsync.dto.AppointmentRequest;
import com.healsync.entity.Appointment;
import com.healsync.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/book")
    public ResponseEntity<Appointment> bookAppointment(@Valid @RequestBody AppointmentRequest request) {

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
}
