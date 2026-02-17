package com.healsync.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppointmentCancelRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
