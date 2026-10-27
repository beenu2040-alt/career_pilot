package com.careerpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateMilestoneRequest(
        @NotBlank(message = "Milestone status is required")
        @Pattern(regexp = "NOT_STARTED|IN_PROGRESS|COMPLETED", message = "Status must be NOT_STARTED, IN_PROGRESS, or COMPLETED")
        String status,

        String notes
) {}
