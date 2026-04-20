package com.healsync.service;

import com.healsync.entity.*;
import com.healsync.enums.AppointmentReviewStatus;
import com.healsync.enums.AppointmentStatus;
import com.healsync.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMedicalSummaryService {

    private final AppointmentRepository appointmentRepository;
    private final MedicalSummaryReportRepository medicalSummaryReportRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicalReportRepository medicalReportRepository;
    private final ReportAttachmentRepository reportAttachmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public List<Map<String, Object>> getPendingMedicalSummaries() {
        List<Appointment> completed = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .sorted(Comparator.comparing(Appointment::getStartDateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Appointment app : completed) {
            MedicalSummaryReport summary = medicalSummaryReportRepository.findByAppointmentId(app.getId()).orElse(null);
            AppointmentReviewStatus reviewStatus = deriveReviewStatus(app, summary);
            if (reviewStatus != AppointmentReviewStatus.READY_FOR_ADMIN) {
                continue;
            }
            PatientProfile patient = patientProfileRepository.findById(app.getPatientId()).orElse(null);
            DoctorProfile doctor = doctorProfileRepository.findById(app.getDoctorId()).orElse(null);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("appointmentId", app.getId());
            row.put("patientName", patient != null ? patient.getFullName() : "Patient");
            row.put("doctorName", doctor != null ? doctor.getFullName() : "Doctor");
            row.put("appointmentDate", app.getStartDateTime());
            row.put("status", app.getStatus().name());
            row.put("reviewStatus", reviewStatus.name());
            row.put("hasSummary", summary != null && summary.getGeneratedSummary() != null && !summary.getGeneratedSummary().isBlank());
            row.put("emailed", summary != null && summary.getEmailedAt() != null);
            row.put("emailedAt", summary != null ? summary.getEmailedAt() : null);
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> getSummaryDetails(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        PatientProfile patient = patientProfileRepository.findById(appointment.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));
        DoctorProfile doctor = doctorProfileRepository.findById(appointment.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));

        List<Prescription> prescriptions = new ArrayList<>(prescriptionRepository.findByAppointmentId(appointmentId));
        prescriptions.sort(Comparator.comparing(Prescription::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        if (prescriptions.isEmpty()) {
            prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(appointment.getPatientId()).stream()
                    .filter(p -> p.getDoctorId() != null && p.getDoctorId().equals(doctor.getId()))
                    .toList();
        }
        List<Map<String, Object>> prescriptionRows = prescriptions.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("notes", p.getNotes());
            m.put("createdAt", p.getCreatedAt());
            m.put("items", prescriptionItemRepository.findByPrescriptionId(p.getId()));
            return m;
        }).toList();

        List<MedicalReport> reports = medicalReportRepository.findByPatientIdOrderByCreatedAtDesc(appointment.getPatientId()).stream()
                .filter(r -> r.getDoctorId() != null && r.getDoctorId().equals(doctor.getId()))
                .toList();
        List<Map<String, Object>> reportRows = reports.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getTitle());
            m.put("description", r.getDescription());
            m.put("createdAt", r.getCreatedAt());
            m.put("attachments", reportAttachmentRepository.findByReportId(r.getId()));
            return m;
        }).toList();

        MedicalSummaryReport summary = medicalSummaryReportRepository.findByAppointmentId(appointmentId).orElse(null);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", appointment.getId());
        payload.put("appointmentDate", appointment.getStartDateTime());
        payload.put("status", appointment.getStatus().name());
        payload.put("patientId", patient.getId());
        payload.put("patientName", patient.getFullName());
        payload.put("doctorId", doctor.getId());
        payload.put("doctorName", doctor.getFullName());
        payload.put("diagnosis", appointment.getDiagnosis());
        payload.put("clinicalNotes", appointment.getDoctorNotes());
        payload.put("followUpInstructions", appointment.getFollowUpInstructions());
        payload.put("doctorNotes", appointment.getDoctorNotes());
        payload.put("prescriptions", prescriptionRows);
        payload.put("reports", reportRows);
        payload.put("summary", summary != null ? summary.getGeneratedSummary() : null);
        payload.put("emailedAt", summary != null ? summary.getEmailedAt() : null);
        return payload;
    }

    public Map<String, Object> generateSummary(Long appointmentId, Long adminUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Summary can only be generated for completed appointments.");
        }
        Map<String, Object> details = getSummaryDetails(appointmentId);
        String generated = buildSummaryText(details);
        MedicalSummaryReport summary = medicalSummaryReportRepository.findByAppointmentId(appointmentId)
                .orElseGet(MedicalSummaryReport::new);
        summary.setAppointmentId(appointmentId);
        summary.setPatientId((Long) details.get("patientId"));
        summary.setDoctorId((Long) details.get("doctorId"));
        summary.setGeneratedSummary(generated);
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setGeneratedByAdmin(adminUserId);
        medicalSummaryReportRepository.save(summary);
        details.put("summary", generated);
        return details;
    }

    public Map<String, Object> saveSummary(Long appointmentId, String summaryText, Long adminUserId) {
        MedicalSummaryReport summary = medicalSummaryReportRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Generate summary first."));
        summary.setGeneratedSummary(summaryText);
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setGeneratedByAdmin(adminUserId);
        medicalSummaryReportRepository.save(summary);
        return Map.of("message", "Summary saved successfully.");
    }

    public Map<String, Object> sendSummary(Long appointmentId, boolean resend, Long adminUserId) {
        MedicalSummaryReport summary = medicalSummaryReportRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Generate and save summary before emailing."));
        if (summary.getGeneratedSummary() == null || summary.getGeneratedSummary().isBlank()) {
            throw new RuntimeException("Summary text is empty.");
        }
        if (summary.getEmailedAt() != null && !resend) {
            throw new RuntimeException("Summary already emailed. Use resend.");
        }
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        PatientProfile patient = patientProfileRepository.findById(summary.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));
        User patientUser = userRepository.findById(patient.getUserId())
                .orElseThrow(() -> new RuntimeException("Patient user not found"));
        DoctorProfile doctor = doctorProfileRepository.findById(summary.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));

        emailService.sendVisitSummary(
                patientUser.getEmail(),
                patient.getFullName(),
                doctor.getFullName(),
                appointment.getStartDateTime() != null ? appointment.getStartDateTime().toString() : "N/A",
                summary.getGeneratedSummary());

        summary.setEmailedAt(LocalDateTime.now());
        summary.setGeneratedByAdmin(adminUserId);
        medicalSummaryReportRepository.save(summary);
        AppointmentReviewStatus reviewStatus = deriveReviewStatus(appointment, summary);
        return Map.of(
                "message", "Summary emailed successfully.",
                "emailedAt", summary.getEmailedAt(),
                "reviewStatus", reviewStatus.name());
    }

    private String buildSummaryText(Map<String, Object> details) {
        String diagnosisBlock = firstNonBlank(details.get("diagnosis"), "Not recorded.");
        String clinicalBlock = firstNonBlank(details.get("clinicalNotes"), "Not recorded.");
        String followBlock = firstNonBlank(details.get("followUpInstructions"), "Not recorded.");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> prescriptions = (List<Map<String, Object>>) details.getOrDefault("prescriptions", List.of());

        String medicines = prescriptions.stream()
                .flatMap(p -> {
                    @SuppressWarnings("unchecked")
                    List<PrescriptionItem> items = (List<PrescriptionItem>) p.getOrDefault("items", List.of());
                    return items.stream();
                })
                .map(i -> "* " + i.getMedicineName() + " " + i.getDosage() + " " + i.getFrequency() + " for " + i.getDurationDays() + " days")
                .collect(Collectors.joining("\n"));
        if (medicines.isBlank()) {
            medicines = "* No medicines recorded for this visit.";
        }

        return """
                Diagnosis
                %s

                Doctor notes
                %s

                Medicines prescribed

                %s

                Follow-up instructions
                %s
                """.formatted(diagnosisBlock, clinicalBlock, medicines, followBlock);
    }

    private String firstNonBlank(Object value, String placeholder) {
        String s = Objects.toString(value, "").trim();
        return s.isBlank() ? placeholder : s;
    }

    private AppointmentReviewStatus deriveReviewStatus(Appointment app, MedicalSummaryReport summary) {
        if (app.getStatus() != AppointmentStatus.COMPLETED) {
            return AppointmentReviewStatus.NOT_READY;
        }
        if (summary != null && summary.getEmailedAt() != null) {
            return AppointmentReviewStatus.SUMMARY_SENT;
        }
        boolean hasDiagnosis = app.getDiagnosis() != null && !app.getDiagnosis().isBlank();
        boolean hasClinicalNotes = app.getDoctorNotes() != null && !app.getDoctorNotes().isBlank();
        boolean hasPrescription = prescriptionRepository.existsByAppointmentId(app.getId());
        return (hasDiagnosis && hasClinicalNotes && hasPrescription)
                ? AppointmentReviewStatus.READY_FOR_ADMIN
                : AppointmentReviewStatus.NOT_READY;
    }
}
