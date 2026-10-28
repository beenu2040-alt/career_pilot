package com.careerpilot.dto;

import java.util.List;

public record KeywordCoverageDto(
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> addedKeywords,
        List<String> missingQualifications,
        int matchScoreBefore,
        int matchScoreAfter,
        String explanation
) {}
