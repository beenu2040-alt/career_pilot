package com.careerpilot.dto;

import java.util.UUID;

public record SaveJobResponse(
        UUID jobId,
        boolean isSaved,
        String message
) {}
