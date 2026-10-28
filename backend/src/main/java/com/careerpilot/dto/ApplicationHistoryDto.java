package com.careerpilot.dto;

import java.time.Instant;
import java.util.UUID;

public record ApplicationHistoryDto(
        UUID id,
        UUID applicationId,
        String previousStatus,
        String newStatus,
        String notes,
        Instant createdAt
) {}
