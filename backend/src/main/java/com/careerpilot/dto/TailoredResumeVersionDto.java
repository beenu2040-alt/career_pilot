package com.careerpilot.dto;

import java.time.Instant;
import java.util.UUID;

public record TailoredResumeVersionDto(
        UUID id,
        UUID sessionId,
        int versionNumber,
        String versionName,
        TailoredResumeContentDto tailoredContent,
        String tailoredPlainText,
        String changeSummary,
        int appliedSuggestionsCount,
        KeywordCoverageDto keywordCoverage,
        Instant createdAt
) {}
