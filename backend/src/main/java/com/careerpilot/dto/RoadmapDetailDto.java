package com.careerpilot.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoadmapDetailDto(
        UUID id,
        UUID userId,
        String targetRole,
        String currentLevel,
        int overallProgress,
        String status,
        String summary,
        SkillGapSummaryDto skillGapSummary,
        List<MilestoneDto> milestones,
        String modelIdentifier,
        Instant createdAt,
        Instant updatedAt
) {}
