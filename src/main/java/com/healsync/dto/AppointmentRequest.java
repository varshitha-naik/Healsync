package com.healsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentRequest {

    @NotNull(message = "Clinic ID is required")
    private Long clinicId;

    private Long doctorId;

    private String specialization;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotBlank(message = "Start time is required")
    private String start;

    @NotBlank(message = "End time is required")
    private String end;

    private String reason;
}
