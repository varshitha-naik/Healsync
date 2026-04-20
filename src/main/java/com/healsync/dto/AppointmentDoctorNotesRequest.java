package com.healsync.dto;

import lombok.Data;

@Data
public class AppointmentDoctorNotesRequest {

    private String diagnosis;

    private String clinicalNotes;

    private String followUpInstructions;

    /** Legacy clients: maps to stored clinical notes when clinicalNotes is absent. */
    private String doctorNotes;
}
