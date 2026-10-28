package com.careerpilot.dto;

import com.careerpilot.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(
        @NotNull(message = "Status is required")
        ApplicationStatus status,
        String notes
) {}
