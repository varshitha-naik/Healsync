package com.healsync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPatientAppointmentDTO {
    private Long patientId;
    private String patientName;
    private String email;
    private LocalDateTime lastAppointmentDate;
    private String appointmentStatus;
}
