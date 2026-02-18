package com.healsync.dto;

import lombok.Data;
import java.time.LocalTime;
import java.util.List;

@Data
public class AvailabilityRequest {
    private List<String> days;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long doctorId; // Optional if derived from context
}
