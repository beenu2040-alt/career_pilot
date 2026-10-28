package com.careerpilot.dto;

import com.careerpilot.entity.TailoringSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record TailoringSessionDto(
        UUID id,
        UUID resumeId,
        String resumeFilename,
        UUID jobId,
        String jobTitle,
        String companyName,
        TailoringSessionStatus status,
        String targetRole,
        String modelIdentifier,
        Integer matchScoreBefore,
        Integer matchScoreAfter,
        int versionCount,
        Instant createdAt,
        Instant updatedAt
) {}
