package com.healsync.controller;

import com.healsync.service.AuthService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;
    private final DoctorService doctorService;

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

    @PutMapping("/doctors/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable Long id, @RequestBody UpdateDoctorRequest request) {
        doctorService.updateDoctorAdmin(id, request);
        return ResponseEntity.ok().body("{\"message\": \"Doctor updated successfully\"}");
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
}
