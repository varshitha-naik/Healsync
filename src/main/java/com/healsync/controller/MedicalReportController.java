package com.healsync.controller;

import com.healsync.entity.MedicalReport;
import com.healsync.entity.ReportAttachment;
import com.healsync.enums.ReportType;
import com.healsync.repository.*;
import com.healsync.service.EmailService;
import com.healsync.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> uploadReport(
            @RequestParam("patientId") Long patientId,
            @RequestParam("doctorId") Long doctorId,
            @RequestParam(value = "appointmentId", required = false) Long appointmentId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("files") MultipartFile[] files) {

        // 1. Create Report
        MedicalReport report = new MedicalReport();
        report.setPatientId(patientId);
        report.setDoctorId(doctorId);
        report.setAppointmentId(appointmentId);
        report.setTitle(title);
        report.setDescription(description);
        report.setReportType(ReportType.GENERAL); // Default

        MedicalReport savedReport = medicalReportRepository.save(report);

        // 2. Process Files
        for (MultipartFile file : files) {
            String fileUrl = fileStorageService.storeFile(file);

            ReportAttachment attachment = new ReportAttachment();
            attachment.setReportId(savedReport.getId());
            attachment.setFileName(file.getOriginalFilename());
            attachment.setFileUrl(fileUrl);
            attachment.setSize(file.getSize());
            attachment.setContentType(file.getContentType());
            reportAttachmentRepository.save(attachment);
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
