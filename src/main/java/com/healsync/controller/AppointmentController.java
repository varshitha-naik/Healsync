package com.healsync.controller;

import com.healsync.dto.AppointmentCancelRequest;
import com.healsync.dto.AppointmentDoctorNotesRequest;
import com.healsync.dto.AppointmentRequest;
import com.healsync.dto.AppointmentStatusRequest;
import com.healsync.entity.Appointment;
import com.healsync.enums.AppointmentStatus;
import com.healsync.enums.UserRole;
import com.healsync.repository.UserRepository;
import com.healsync.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(@Valid @RequestBody AppointmentRequest request, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        LocalDateTime startDateTime = LocalDateTime.parse(request.getStart());
        LocalDateTime endDateTime = LocalDateTime.parse(request.getEnd());

        Appointment appointment = appointmentService.bookAppointment(
                request.getClinicId(),
                request.getDoctorId(),
                request.getPatientId(),
                startDateTime,
                endDateTime,
                request.getReason(),
                request.getSpecialization());

        return ResponseEntity.ok(appointment);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByDoctor(
            @PathVariable Long doctorId,
            @RequestParam(required = false) String status) {

        if (status != null && !status.isEmpty()) {
            try {
                AppointmentStatus apptStatus = AppointmentStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(appointmentService.getAppointmentsByDoctorAndStatus(doctorId, apptStatus));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid appointment status: " + status);
            }
        }
        return ResponseEntity.ok(appointmentService.getAppointmentsByDoctor(doctorId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByPatient(
            @PathVariable Long patientId,
            @RequestParam(required = false) String status) {

        if (status != null && !status.isEmpty()) {
            try {
                AppointmentStatus apptStatus = AppointmentStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(appointmentService.getAppointmentsByPatientAndStatus(patientId, apptStatus));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid appointment status: " + status);
            }
        }
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Appointment> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusRequest request,
            Authentication authentication) {

        try {
            AppointmentStatus status = AppointmentStatus.valueOf(request.getStatus().toUpperCase());
            Long actorUserId = resolveAuthenticatedUserId(authentication);
            boolean isDoctor = isAuthenticatedRole(authentication, UserRole.DOCTOR);
            if (isDoctor) {
                return ResponseEntity.ok(appointmentService.updateStatusForDoctor(id, status, actorUserId));
            }
            return ResponseEntity.ok(appointmentService.updateStatus(id, status));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid appointment status: " + request.getStatus());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentCancelRequest request,
            Authentication authentication) {
        Long actorUserId = resolveAuthenticatedUserId(authentication);
        boolean isDoctor = isAuthenticatedRole(authentication, UserRole.DOCTOR);
        if (isDoctor) {
            return ResponseEntity.ok(appointmentService.cancelAppointmentForDoctor(id, request.getReason(), actorUserId));
        }
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, request.getReason()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @PutMapping("/{id}/notes")
    public ResponseEntity<Appointment> updateDoctorNotes(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentDoctorNotesRequest request,
            Authentication authentication) {
        Long actorUserId = resolveAuthenticatedUserId(authentication);
        String clinicalNotes = resolveClinicalNotes(request);
        boolean isDoctor = isAuthenticatedRole(authentication, UserRole.DOCTOR);
        if (isDoctor) {
            return ResponseEntity.ok(appointmentService.updateClinicalFieldsForDoctor(
                    id, request.getDiagnosis(), clinicalNotes, request.getFollowUpInstructions(), actorUserId));
        }
        return ResponseEntity.ok(appointmentService.updateClinicalFields(
                id, request.getDiagnosis(), clinicalNotes, request.getFollowUpInstructions()));
    }

    private String resolveClinicalNotes(AppointmentDoctorNotesRequest request) {
        if (request.getClinicalNotes() != null) {
            return request.getClinicalNotes();
        }
        return request.getDoctorNotes();
    }

    private Long resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findByEmail(authentication.getName())
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private boolean isAuthenticatedRole(Authentication authentication, UserRole role) {
        if (authentication == null) return false;
        String expected = "ROLE_" + role.name();
        return authentication.getAuthorities().stream().anyMatch(a -> expected.equals(a.getAuthority()));
    }
}
