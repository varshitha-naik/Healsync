package com.healsync.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppointmentDoctorNotesRequest {

    @NotBlank(message = "Doctor notes are required")
    private String doctorNotes;
}
