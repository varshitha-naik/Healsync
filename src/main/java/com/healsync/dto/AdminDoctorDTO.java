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
public class AdminDoctorDTO {
    private Long id; // userId
    private String fullName;
    private String specialization;
    private String email;
    private String profilePhotoUrl;
    private Integer experienceYears;
    private String clinicName;
    private String bio;
    private String status; // ACTIVE or INACTIVE
    private Boolean active;
    private Integer patientCount;
    private LocalDateTime lastAppointment;
    private String availabilityStatus;
    private String availabilitySummary;
}
