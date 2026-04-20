package com.healsync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPatientsByDoctorDTO {
    private Long doctorId;
    private String doctorName;
    private String doctorEmail;
    private String specialization;
    private String profilePhotoUrl;
    private List<AdminPatientAppointmentDTO> patients;
}
