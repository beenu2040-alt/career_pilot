package com.careerpilot.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MilestoneDto(
        UUID id,
        UUID roadmapId,
        String title,
        String description,
        int sequenceOrder,
        String status,
        int estimatedDurationWeeks,
        Instant completedAt,
        List<MilestoneSkillDto> skills,
        List<MilestoneProjectDto> projects,
        List<MilestoneResourceDto> resources,
        List<String> completionCriteria,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
