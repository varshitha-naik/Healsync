package com.healsync.controller;

import com.healsync.dto.AvailabilityRequest;
import com.healsync.entity.DoctorAvailability;
import com.healsync.service.DoctorService;
import com.healsync.service.FileStorageService;
import com.healsync.repository.DoctorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import com.healsync.entity.DoctorProfile;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final FileStorageService fileStorageService;
    private final DoctorProfileRepository doctorProfileRepository;

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
        doctorService.deleteAvailability(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{doctorId}/upload-photo")
    public ResponseEntity<?> uploadProfilePhoto(
            @PathVariable Long doctorId,
            @RequestParam("file") MultipartFile file) {

        // 1. Store File
        String fileUrl = fileStorageService.storeFile(file);

        // 2. Update Database
        DoctorProfile profile = doctorProfileRepository.findByUserId(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));

        profile.setProfilePhotoUrl(fileUrl);
        doctorProfileRepository.save(profile);

        return ResponseEntity.ok(Map.of("message", "Profile photo updated", "url", fileUrl));
    }

    @GetMapping("/{doctorId}/profile")
    public ResponseEntity<?> getProfile(@PathVariable Long doctorId) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        return ResponseEntity.ok(profile);
    }
}
