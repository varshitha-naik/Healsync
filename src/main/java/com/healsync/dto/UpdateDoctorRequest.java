package com.healsync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDoctorRequest {
    private String fullName;
    private String specialization;
    private Integer experienceYears;
    private String bio;
}
