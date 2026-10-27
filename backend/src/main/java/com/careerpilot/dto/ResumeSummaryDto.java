package com.careerpilot.dto;

import com.careerpilot.entity.ResumeProcessingStatus;
import java.time.Instant;
import java.util.UUID;

public record ResumeSummaryDto(
        UUID id,
        String originalFilename,
        Long fileSize,
        String mimeType,
        ResumeProcessingStatus processingStatus,
        Instant createdAt,
        Instant updatedAt,
        Integer latestScore,
        UUID latestAnalysisId
) {}
