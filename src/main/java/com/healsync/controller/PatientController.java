package com.healsync.controller;

import com.healsync.dto.DoctorSummaryDTO;
import com.healsync.entity.Appointment;
import com.healsync.entity.User;
import com.healsync.repository.UserRepository;
import com.healsync.service.AppointmentService;
import com.healsync.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final UserRepository userRepository;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorSummaryDTO>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctorsForPatient());
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getHistory(@PathVariable Long userId) {
        if (!appointmentService.isValidPatientUserId(userId)) {
            return ResponseEntity.badRequest().body("Patient profile not found for user ID: " + userId);
        }

        List<Appointment> history = appointmentService.getCompletedAppointmentsByPatientUserId(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<Appointment>> getMyAppointments(
            org.springframework.security.core.Authentication authentication,
            @RequestParam(required = false) String status) {

        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        if (status != null && !status.isEmpty()) {
            try {
                com.healsync.enums.AppointmentStatus apptStatus = com.healsync.enums.AppointmentStatus
                        .valueOf(status.toUpperCase());
                return ResponseEntity
                        .ok(appointmentService.getAppointmentsByPatientAndStatus(user.getId(), apptStatus));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid appointment status: " + status);
            }
        }
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(user.getId()));
    }
}
