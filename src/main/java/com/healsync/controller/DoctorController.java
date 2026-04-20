package com.healsync.controller;

import com.healsync.dto.AvailabilityRequest;
import com.healsync.dto.DoctorDashboardSummaryDTO;
import com.healsync.dto.DoctorLeaveRequest;
import com.healsync.dto.DoctorProfileUpdateRequest;
import com.healsync.entity.DoctorAvailability;
import com.healsync.entity.User;
import com.healsync.service.DoctorService;
import com.healsync.service.FileStorageService;
import com.healsync.repository.DoctorProfileRepository;
import com.healsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import com.healsync.entity.DoctorProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final FileStorageService fileStorageService;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;

    @GetMapping("/{doctorId}/patients")
    public ResponseEntity<?> getPatients(@PathVariable Long doctorId) {
        return ResponseEntity.ok(doctorService.getPatientsForDoctor(doctorId));
    }

    @GetMapping("/availability/{doctorId}")
    public ResponseEntity<?> getAvailability(@PathVariable Long doctorId) {
        return ResponseEntity.ok(doctorService.getAvailability(doctorId));
    }

    @PostMapping("/availability")
    public ResponseEntity<?> addAvailability(@RequestBody AvailabilityRequest req) {
        validateSelfAccess(req.getDoctorId());
        for (String day : req.getDays()) {
            DoctorAvailability slot = new DoctorAvailability();
            slot.setDoctorId(req.getDoctorId());
            slot.setDayOfWeek(day);
            slot.setStartTime(req.getStartTime());
            slot.setEndTime(req.getEndTime());
            doctorService.addAvailability(slot);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/availability/{id}")
    public ResponseEntity<?> deleteAvailability(@PathVariable Long id) {
        Long authDoctorId = getAuthenticatedUserId();
        doctorService.deleteAvailabilityForDoctor(id, authDoctorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/leave")
    public ResponseEntity<?> addLeave(@RequestBody DoctorLeaveRequest request) {
        validateSelfAccess(request.getDoctorId());
        return ResponseEntity.ok(doctorService.addLeave(
                request.getDoctorId(),
                request.getFromDate(),
                request.getToDate(),
                request.getReason()));
    }

    @GetMapping("/leave/{doctorId}")
    public ResponseEntity<?> getLeaves(@PathVariable Long doctorId) {
        validateSelfAccess(doctorId);
        return ResponseEntity.ok(doctorService.getLeaves(doctorId));
    }

    @DeleteMapping("/leave/{id}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long id) {
        Long authDoctorId = getAuthenticatedUserId();
        doctorService.deleteLeaveForDoctor(id, authDoctorId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/dashboard-summary/{doctorId}")
    public ResponseEntity<DoctorDashboardSummaryDTO> getDashboardSummary(@PathVariable Long doctorId) {
        validateSelfAccess(doctorId);
        return ResponseEntity.ok(doctorService.getDoctorDashboardSummary(doctorId));
    }

    @GetMapping("/{doctorId}/patient-history")
    public ResponseEntity<?> getPatientHistory(
            @PathVariable Long doctorId,
            @RequestParam Long patientProfileId) {
        validateSelfAccess(doctorId);
        return ResponseEntity.ok(doctorService.getPatientHistoryForDoctor(doctorId, patientProfileId));
    }

    @PostMapping("/{doctorId}/upload-photo")
    public ResponseEntity<?> uploadProfilePhoto(
            @PathVariable Long doctorId,
            @RequestParam("file") MultipartFile file) {
        validateSelfAccess(doctorId);

        // 1. Store File
        String fileUrl = fileStorageService.storeFile(file);

        // 2. Update Database
        DoctorProfile profile = doctorProfileRepository.findByUserId(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));

        profile.setProfilePhotoUrl(fileUrl);
        DoctorProfile savedProfile = doctorProfileRepository.save(profile);

        return ResponseEntity.ok(Map.of(
                "message", "Profile photo updated",
                "url", fileStorageService.withCacheBusting(savedProfile.getProfilePhotoUrl(), savedProfile.getUpdatedAt())));
    }

    @GetMapping("/{doctorId}/profile")
    public ResponseEntity<?> getProfile(@PathVariable Long doctorId) {
        validateSelfAccess(doctorId);
        DoctorProfile profile = doctorProfileRepository.findByUserId(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", profile.getId());
        response.put("userId", profile.getUserId());
        response.put("clinicId", profile.getClinicId());
        response.put("fullName", profile.getFullName());
        response.put("specialization", profile.getSpecialization());
        response.put("licenseNumber", profile.getLicenseNumber());
        response.put("experienceYears", profile.getExperienceYears());
        response.put("bio", profile.getBio());
        response.put("profilePhotoUrl",
                fileStorageService.withCacheBusting(profile.getProfilePhotoUrl(), profile.getUpdatedAt()));
        response.put("createdAt", profile.getCreatedAt());
        response.put("updatedAt", profile.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{doctorId}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable Long doctorId, @RequestBody DoctorProfileUpdateRequest request) {
        validateSelfAccess(doctorId);
        DoctorProfile profile = doctorProfileRepository.findByUserId(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        User user = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor user not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            profile.setFullName(request.getFullName().trim());
        }
        if (request.getSpecialization() != null && !request.getSpecialization().isBlank()) {
            profile.setSpecialization(request.getSpecialization().trim());
        }
        if (request.getExperienceYears() != null && request.getExperienceYears() >= 0) {
            profile.setExperienceYears(request.getExperienceYears());
        }
        if (request.getLicenseNumber() != null && !request.getLicenseNumber().isBlank()) {
            profile.setLicenseNumber(request.getLicenseNumber().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail().trim());
            userRepository.save(user);
        }

        DoctorProfile saved = doctorProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of(
                "message", "Profile updated successfully",
                "fullName", saved.getFullName(),
                "specialization", saved.getSpecialization(),
                "experienceYears", saved.getExperienceYears(),
                "licenseNumber", saved.getLicenseNumber(),
                "email", user.getEmail()));
    }

    private void validateSelfAccess(Long doctorUserId) {
        Long authUserId = getAuthenticatedUserId();
        if (!authUserId.equals(doctorUserId)) {
            throw new RuntimeException("Unauthorized access for doctor resource");
        }
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
