package com.healsync.dto;

import lombok.Data;
import java.util.List;

@Data
public class PrescriptionRequest {
    private Long doctorId;
    private Long patientId;
    private Long reportId; // Optional, links to a medical report
    private String notes;
    private List<PrescriptionItemRequest> items;

    @Data
    public static class PrescriptionItemRequest {
        private String medicineName;
        private String dosage;
        private String frequency;
        private Integer durationDays;
        private String instructions;
    }
}
