package com.careerpilot.dto;

import java.time.Instant;
import java.util.UUID;

public record RoadmapSummaryDto(
        UUID id,
        String targetRole,
        String currentLevel,
        int overallProgress,
        String status,
        String summary,
        int milestonesCount,
        int completedMilestonesCount,
        int totalEstimatedWeeks,
        Instant createdAt,
        Instant updatedAt
) {}
