package com.healsync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorPatientDTO {
    private Long patientId; // Profile ID
    private Long userId; // User ID
    private String patientName;
    private String email;
    private LocalDateTime lastVisitDate;
}
