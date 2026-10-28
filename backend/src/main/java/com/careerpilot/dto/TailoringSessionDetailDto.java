package com.careerpilot.dto;

import com.careerpilot.entity.TailoringSessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TailoringSessionDetailDto(
        UUID id,
        UUID resumeId,
        String resumeFilename,
        UUID jobId,
        String jobTitle,
        String companyName,
        String jobDescriptionSnapshot,
        TailoringSessionStatus status,
        String targetRole,
        String modelIdentifier,
        Integer matchScoreBefore,
        Integer matchScoreAfter,
        KeywordCoverageDto keywordCoverage,
        List<TailoringSuggestionDto> suggestions,
        List<TailoredResumeVersionDto> versions,
        Instant createdAt,
        Instant updatedAt
) {}
