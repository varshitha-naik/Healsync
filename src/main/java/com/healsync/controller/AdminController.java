package com.healsync.controller;

import com.healsync.service.AuthService;
import com.healsync.service.AdminMedicalSummaryService;
import com.healsync.service.AppointmentService;
import com.healsync.service.DoctorService;
import com.healsync.dto.AdminDashboardSummaryDTO;
import com.healsync.dto.AdminPatientsByDoctorDTO;
import com.healsync.dto.CreateDoctorRequest;
import com.healsync.dto.UpdateDoctorRequest;
import com.healsync.dto.AdminDoctorDTO;
import com.healsync.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import com.healsync.repository.UserRepository;
import com.healsync.dto.UpdateMedicalSummaryRequest;
import com.healsync.repository.ClinicRepository;
import com.healsync.repository.DoctorProfileRepository;
import com.healsync.service.FileStorageService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;
    private final DoctorService doctorService;
    private final AdminMedicalSummaryService adminMedicalSummaryService;
    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final FileStorageService fileStorageService;

    @PostMapping("/doctors")
    public ResponseEntity<?> createDoctor(@RequestBody CreateDoctorRequest request) {
        authService.createDoctor(request);
        return ResponseEntity.ok().body("{\"message\": \"Doctor created successfully\"}");
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<AdminDoctorDTO>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctorsAdmin());
    }

    @GetMapping("/dashboard-summary")
    public ResponseEntity<AdminDashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(doctorService.getAdminDashboardSummary());
    }

    @GetMapping("/patients-by-doctor")
    public ResponseEntity<List<AdminPatientsByDoctorDTO>> getPatientsByDoctor() {
        return ResponseEntity.ok(doctorService.getPatientsGroupedByDoctorForAdmin());
    }

    @PutMapping("/doctors/{id}/status")
    public ResponseEntity<?> updateDoctorStatus(@PathVariable Long id, @RequestParam UserStatus status) {
        doctorService.updateDoctorStatusAdmin(id, status);
        return ResponseEntity.ok().body("{\"message\": \"Doctor status updated successfully\"}");
    }

    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctorAdmin(id);
        return ResponseEntity.ok().body("{\"message\": \"Doctor deleted successfully\"}");
    }

    @GetMapping("/doctors/{id}")
    public ResponseEntity<Map<String, Object>> getDoctorProfile(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorProfileForAdmin(id));
    }

    @PutMapping("/doctors/{id}")
    public ResponseEntity<Map<String, Object>> updateDoctorProfile(@PathVariable Long id, @RequestBody UpdateDoctorRequest request) {
        doctorService.updateDoctorAdmin(id, request);
        return ResponseEntity.ok(doctorService.getDoctorProfileForAdmin(id));
    }

    @PostMapping("/doctors/{id}/photo")
    public ResponseEntity<?> uploadDoctorPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        var profile = doctorProfileRepository.findByUserId(id)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        String url = fileStorageService.storeFile(file);
        profile.setProfilePhotoUrl(url);
        var saved = doctorProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of(
                "message", "Profile photo updated",
                "url", fileStorageService.withCacheBusting(saved.getProfilePhotoUrl(), saved.getUpdatedAt())));
    }

    @GetMapping("/clinics")
    public ResponseEntity<?> getClinics() {
        return ResponseEntity.ok(clinicRepository.findAll());
    }

    @GetMapping("/medical-summaries")
    public ResponseEntity<List<Map<String, Object>>> getPendingMedicalSummaries() {
        return ResponseEntity.ok(adminMedicalSummaryService.getPendingMedicalSummaries());
    }

    @GetMapping("/patients/{patientId}/history")
    public ResponseEntity<Map<String, Object>> getPatientHistoryByDoctor(
            @PathVariable Long patientId,
            @RequestParam Long doctorId) {
        return ResponseEntity.ok(appointmentService.getAdminPatientHistory(patientId, doctorId));
    }

    @GetMapping("/medical-summaries/{appointmentId}")
    public ResponseEntity<Map<String, Object>> getMedicalSummaryDetails(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(adminMedicalSummaryService.getSummaryDetails(appointmentId));
    }

    @PostMapping("/medical-summaries/{appointmentId}/generate")
    public ResponseEntity<Map<String, Object>> generateMedicalSummary(@PathVariable Long appointmentId, Authentication authentication) {
        return ResponseEntity.ok(adminMedicalSummaryService.generateSummary(appointmentId, resolveAdminUserId(authentication)));
    }

    @PutMapping("/medical-summaries/{appointmentId}")
    public ResponseEntity<Map<String, Object>> saveMedicalSummary(
            @PathVariable Long appointmentId,
            @RequestBody UpdateMedicalSummaryRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(adminMedicalSummaryService.saveSummary(
                appointmentId,
                request.getGeneratedSummary(),
                resolveAdminUserId(authentication)));
    }

    @PostMapping("/medical-summaries/{appointmentId}/send")
    public ResponseEntity<Map<String, Object>> sendMedicalSummary(
            @PathVariable Long appointmentId,
            @RequestParam(defaultValue = "false") boolean resend,
            Authentication authentication) {
        return ResponseEntity.ok(adminMedicalSummaryService.sendSummary(
                appointmentId,
                resend,
                resolveAdminUserId(authentication)));
    }

    private Long resolveAdminUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .map(u -> u.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated admin user not found"));
    }
}
