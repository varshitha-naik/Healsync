package com.healsync.controller;

import com.healsync.entity.MedicalReport;
import com.healsync.entity.ReportAttachment;
import com.healsync.enums.UserRole;
import com.healsync.enums.ReportType;
import com.healsync.repository.*;
import com.healsync.service.EmailService;
import com.healsync.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/medical-reports")
@RequiredArgsConstructor
public class MedicalReportController {

    private final MedicalReportRepository medicalReportRepository;
    private final ReportAttachmentRepository reportAttachmentRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final PatientProfileRepository patientProfileRepository;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppointmentRepository appointmentRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PATIENT')")
    public ResponseEntity<?> uploadReport(
            @RequestParam("patientId") Long patientId,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "appointmentId", required = false) Long appointmentId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) {
        Long authUserId = userRepository.findByEmail(authentication.getName())
                .map(u -> u.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        boolean isDoctor = authentication.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + UserRole.DOCTOR.name()).equals(a.getAuthority()));
        Long resolvedDoctorProfileId = doctorId;
        if (isDoctor) {
            var doctorProfile = doctorProfileRepository.findByUserId(authUserId)
                    .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
            resolvedDoctorProfileId = doctorProfile.getId();
            boolean ownsAppointment;
            if (appointmentId != null) {
                ownsAppointment = appointmentRepository.findById(appointmentId)
                        .map(app -> app.getDoctorId().equals(doctorProfile.getId()) && app.getPatientId().equals(patientId))
                        .orElse(false);
            } else {
                ownsAppointment = appointmentRepository.findByPatientId(patientId).stream()
                        .anyMatch(app -> app.getDoctorId() != null && app.getDoctorId().equals(doctorProfile.getId()));
            }
            if (!ownsAppointment) {
                throw new RuntimeException("Unauthorized: appointment does not belong to this doctor");
            }
        }

        // 1. Create Report
        MedicalReport report = new MedicalReport();
        report.setPatientId(patientId);
        report.setDoctorId(resolvedDoctorProfileId);
        report.setAppointmentId(appointmentId);
        report.setTitle(title);
        report.setDescription(description);
        report.setReportType(ReportType.GENERAL); // Default

        MedicalReport savedReport = medicalReportRepository.save(report);

        // 2. Process Files
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                String fileUrl = fileStorageService.storeFile(file);

                ReportAttachment attachment = new ReportAttachment();
                attachment.setReportId(savedReport.getId());
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFileUrl(fileUrl);
                attachment.setSize(file.getSize());
                attachment.setContentType(file.getContentType());
                reportAttachmentRepository.save(attachment);
            }
        }

        // 3. Send Email Notification
        try {
            var patientProfile = patientProfileRepository.findById(patientId).orElse(null);
            if (patientProfile != null) {
                var user = userRepository.findById(patientProfile.getUserId()).orElse(null);
                if (user != null) {
                    emailService.sendReportUploaded(
                            user.getEmail(),
                            patientProfile.getFullName(),
                            title);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(Map.of("message", "Medical report uploaded", "id", savedReport.getId()));
    }
}
