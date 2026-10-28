package com.careerpilot.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateTailoringSessionRequest(
        @NotNull(message = "Resume ID is required")
        UUID resumeId,
        UUID jobId,
        String customJobTitle,
        String customCompanyName,
        String customJobDescription
) {}
