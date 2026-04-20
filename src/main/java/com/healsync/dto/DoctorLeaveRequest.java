package com.healsync.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DoctorLeaveRequest {
    private Long doctorId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
}
