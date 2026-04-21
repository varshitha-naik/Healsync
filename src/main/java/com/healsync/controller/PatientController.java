package com.healsync.controller;

import com.healsync.dto.DoctorSummaryDTO;
import com.healsync.entity.Appointment;
import com.healsync.entity.PatientProfile;
import com.healsync.entity.User;
import com.healsync.enums.AppointmentStatus;
import com.healsync.repository.ClinicRepository;
import com.healsync.repository.PatientProfileRepository;
import com.healsync.repository.PrescriptionRepository;
import com.healsync.repository.UserRepository;
import com.healsync.service.AppointmentService;
import com.healsync.service.DoctorService;
import com.healsync.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final UserRepository userRepository;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final PatientProfileRepository patientProfileRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ClinicRepository clinicRepository;
    private final FileStorageService fileStorageService;

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

    @GetMapping("/dashboard-summary")
    public ResponseEntity<?> getDashboardSummary(org.springframework.security.core.Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        PatientProfile profile = patientProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        List<Appointment> appointments = appointmentService.getAppointmentsByPatient(user.getId());
        LocalDateTime now = LocalDateTime.now();
        Appointment next = appointments.stream()
                .filter(a -> a.getStartDateTime() != null && a.getStartDateTime().isAfter(now))
                .filter(a -> a.getStatus() == AppointmentStatus.REQUESTED || a.getStatus() == AppointmentStatus.CONFIRMED)
                .min(Comparator.comparing(Appointment::getStartDateTime))
                .orElse(null);

        Map<String, Object> nextAppointment = null;
        if (next != null) {
            nextAppointment = new LinkedHashMap<>();
            nextAppointment.put("id", next.getId());
            nextAppointment.put("doctorName", next.getDoctorName());
            nextAppointment.put("startDateTime", next.getStartDateTime());
            nextAppointment.put("status", next.getStatus() != null ? next.getStatus().name() : null);
            nextAppointment.put("clinicName", clinicRepository.findById(next.getClinicId()).map(c -> c.getName()).orElse("Clinic"));
        }

        long upcomingCount = appointments.stream()
                .filter(a -> a.getStartDateTime() != null && a.getStartDateTime().isAfter(now))
                .filter(a -> a.getStatus() == AppointmentStatus.REQUESTED || a.getStatus() == AppointmentStatus.CONFIRMED)
                .count();
        long prescriptionCount = prescriptionRepository.findByPatientId(profile.getId()).size();
        long familyCount = profile.getEmergencyContactName() != null && !profile.getEmergencyContactName().isBlank() ? 1 : 0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("upcomingAppointments", upcomingCount);
        response.put("prescriptionsCount", prescriptionCount);
        response.put("familyMembersCount", familyCount);
        response.put("nextAppointment", nextAppointment);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(org.springframework.security.core.Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        PatientProfile profile = patientProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", profile.getId());
        response.put("userId", profile.getUserId());
        response.put("fullName", profile.getFullName());
        response.put("email", user.getEmail());
        response.put("phone", profile.getPhone());
        response.put("dob", profile.getDob());
        response.put("gender", profile.getGender());
        response.put("bloodGroup", profile.getBloodGroup());
        response.put("address", profile.getAddress());
        response.put("emergencyContactName", profile.getEmergencyContactName());
        response.put("emergencyContactPhone", profile.getEmergencyContactPhone());
        response.put("profilePhotoUrl", profile.getProfilePhotoUrl());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            org.springframework.security.core.Authentication authentication,
            @RequestBody Map<String, Object> request) {
        User user = getAuthenticatedUser(authentication);
        PatientProfile profile = patientProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        if (request.get("phone") instanceof String phone && !phone.isBlank()) {
            profile.setPhone(phone.trim());
        }
        if (request.get("address") instanceof String address) {
            profile.setAddress(address.trim().isEmpty() ? null : address.trim());
        }
        if (request.get("emergencyContactName") instanceof String emergencyContactName) {
            profile.setEmergencyContactName(emergencyContactName.trim().isEmpty() ? null : emergencyContactName.trim());
        }
        if (request.get("emergencyContactPhone") instanceof String emergencyContactPhone) {
            profile.setEmergencyContactPhone(emergencyContactPhone.trim().isEmpty() ? null : emergencyContactPhone.trim());
        }

        patientProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @PostMapping("/profile/photo")
    public ResponseEntity<?> uploadProfilePhoto(
            org.springframework.security.core.Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        User user = getAuthenticatedUser(authentication);
        PatientProfile profile = patientProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));
        String fileUrl = fileStorageService.storeFile(file);
        profile.setProfilePhotoUrl(fileUrl);
        patientProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "Profile photo updated", "url", fileUrl));
    }

    private User getAuthenticatedUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + authentication.getName()));
    }
}
