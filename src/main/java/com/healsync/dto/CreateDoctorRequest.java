package com.healsync.dto;

import lombok.Data;

@Data
public class CreateDoctorRequest {
    private String email;
    private String password;
    private String fullName;
    private String specialization;
    private String licenseNumber;
    private Integer experienceYears;
    private Long clinicId;
    private String bio;
}
