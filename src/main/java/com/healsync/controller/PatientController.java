package com.healsync.controller;

import com.healsync.dto.DoctorSummaryDTO;
import com.healsync.entity.Appointment;
import com.healsync.entity.DoctorProfile;
import com.healsync.entity.PatientProfile;
import com.healsync.entity.User;
import com.healsync.enums.UserRole;
import com.healsync.repository.DoctorProfileRepository;
import com.healsync.repository.PatientProfileRepository;
import com.healsync.repository.UserRepository;
import com.healsync.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AppointmentService appointmentService;

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorSummaryDTO>> getAllDoctors() {
        List<User> doctors = userRepository.findByRole(UserRole.DOCTOR);
        List<DoctorSummaryDTO> dtos = new ArrayList<>();

        for (User u : doctors) {
            Optional<DoctorProfile> p = doctorProfileRepository.findByUserId(u.getId());
            DoctorSummaryDTO dto = new DoctorSummaryDTO();
            dto.setDoctorId(u.getId());
            dto.setEmail(u.getEmail());
            if (p.isPresent()) {
                dto.setName(p.get().getFullName());
                dto.setSpecialization(p.get().getSpecialization());
            } else {
                dto.setName("Unknown Doctor");
                dto.setSpecialization("General");
            }
            dtos.add(dto);
        }
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getHistory(@PathVariable Long userId) {
        Optional<PatientProfile> profile = patientProfileRepository.findByUserId(userId);
        if (profile.isEmpty()) {
            return ResponseEntity.badRequest().body("Patient profile not found for user ID: " + userId);
        }

        // Use profile ID to fetch appointments
        List<Appointment> history = appointmentService.getAppointmentsByPatient(profile.get().getId());
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
