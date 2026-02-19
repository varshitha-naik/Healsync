package com.healsync.controller;

import com.healsync.dto.PrescriptionRequest;
import com.healsync.entity.Prescription;
import com.healsync.entity.PrescriptionItem;
import com.healsync.repository.*;
import com.healsync.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> createPrescription(@RequestBody PrescriptionRequest request) {

        // 1. Create Prescription
        Prescription prescription = new Prescription();
        prescription.setDoctorId(request.getDoctorId());
        prescription.setPatientId(request.getPatientId());
        prescription.setReportId(request.getReportId());
        prescription.setNotes(request.getNotes());

        Prescription saved = prescriptionRepository.save(prescription);

        // 2. Create Items
        List<PrescriptionItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (PrescriptionRequest.PrescriptionItemRequest itemReq : request.getItems()) {
                PrescriptionItem item = new PrescriptionItem();
                item.setPrescriptionId(saved.getId());
                item.setMedicineName(itemReq.getMedicineName());
                item.setDosage(itemReq.getDosage());
                item.setFrequency(itemReq.getFrequency());
                item.setDurationDays(itemReq.getDurationDays());
                item.setInstructions(itemReq.getInstructions());
                items.add(item);
            }
            prescriptionItemRepository.saveAll(items);
        }

        // 3. Send Email
        try {
            var patientProfile = patientProfileRepository.findById(request.getPatientId()).orElse(null);
            var doctorProfile = doctorProfileRepository.findByUserId(request.getDoctorId()).orElse(null);

            if (patientProfile != null && doctorProfile != null) {
                var user = userRepository.findById(patientProfile.getUserId()).orElse(null);
                if (user != null) {
                    emailService.sendPrescriptionCreated(
                            user.getEmail(),
                            patientProfile.getFullName(),
                            doctorProfile.getFullName());
                }
            }
        } catch (Exception e) {
            // Log error but don't fail request
            e.printStackTrace();
        }

        return ResponseEntity.ok(Map.of("message", "Prescription created", "id", saved.getId()));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    public ResponseEntity<?> getByPatient(@PathVariable Long patientId) {
        // Simple fetch logic (extend as needed)
        return ResponseEntity.ok(prescriptionRepository.findByPatientId(patientId));
    }
}
