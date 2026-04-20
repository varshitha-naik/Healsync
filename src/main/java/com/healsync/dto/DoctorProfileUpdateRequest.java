package com.healsync.dto;

import lombok.Data;

@Data
public class DoctorProfileUpdateRequest {
    private String fullName;
    private String specialization;
    private String email;
    private Integer experienceYears;
    private String licenseNumber;
}
