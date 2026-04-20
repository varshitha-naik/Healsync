package com.healsync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardSummaryDTO {
    private long totalDoctors;
    private long totalPatients;
    private long appointmentsToday;
    private long completedAppointmentsToday;
    private long doctorsAvailableToday;
}
