package com.healsync.controller;

import com.healsync.dto.PrescriptionRequest;
import com.healsync.entity.Prescription;
import com.healsync.entity.PrescriptionItem;
import com.healsync.repository.*;
import com.healsync.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> createPrescription(@RequestBody PrescriptionRequest request, Authentication authentication) {
        Long doctorUserId = userRepository.findByEmail(authentication.getName())
                .map(u -> u.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated doctor not found"));
        var doctorProfile = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        if (request.getPatientId() == null) {
            throw new RuntimeException("Patient ID is required");
        }
        boolean hasDoctorAppointmentWithPatient = appointmentRepository.findByPatientId(request.getPatientId()).stream()
                .anyMatch(app -> app.getDoctorId() != null && app.getDoctorId().equals(doctorProfile.getId()));
        if (!hasDoctorAppointmentWithPatient) {
            throw new RuntimeException("Unauthorized: patient does not belong to this doctor");
        }
        if (request.getAppointmentId() != null) {
            boolean ownsAppointment = appointmentRepository.findById(request.getAppointmentId())
                    .map(app -> app.getDoctorId() != null && app.getDoctorId().equals(doctorProfile.getId())
                            && app.getPatientId() != null && app.getPatientId().equals(request.getPatientId()))
                    .orElse(false);
            if (!ownsAppointment) {
                throw new RuntimeException("Unauthorized: appointment does not belong to this doctor");
            }
        }

        // 1. Create Prescription (no automatic medical report — admin issues patient-facing reports)
        Prescription prescription = new Prescription();
        prescription.setDoctorId(doctorProfile.getId());
        prescription.setPatientId(request.getPatientId());
        prescription.setReportId(request.getReportId());
        prescription.setAppointmentId(request.getAppointmentId());
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
            String appointmentDateTime = null;
            if (request.getAppointmentId() != null) {
                appointmentDateTime = appointmentRepository.findById(request.getAppointmentId())
                        .map(app -> app.getStartDateTime() != null ? app.getStartDateTime().toString() : null)
                        .orElse(null);
            }
            List<String> medicineLines = items.stream()
                    .map(i -> i.getMedicineName() + " - " + i.getDosage() + ", " + i.getFrequency() + ", " + i.getDurationDays() + " day(s)")
                    .collect(Collectors.toList());

            if (patientProfile != null && doctorProfile != null) {
                var user = userRepository.findById(patientProfile.getUserId()).orElse(null);
                if (user != null) {
                    emailService.sendPrescriptionSummary(
                            user.getEmail(),
                            patientProfile.getFullName(),
                            doctorProfile.getFullName(),
                            appointmentDateTime,
                            medicineLines,
                            request.getNotes());
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
