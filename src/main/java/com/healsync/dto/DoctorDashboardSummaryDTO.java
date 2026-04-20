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
public class DoctorDashboardSummaryDTO {
    private long todaysAppointments;
    private long pendingAppointments;
    private long completedToday;
    private long upcomingAppointments;
    private boolean missingAvailabilityNext3Days;
    private List<String> missingAvailabilityDates;
    private String nextAppointmentSummary;
}
