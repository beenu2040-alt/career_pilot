package com.careerpilot.dto;

public record DimensionScoreDto(
        String dimension,
        int score,
        int maxScore,
        int weightPercentage,
        String assessmentType,
        String feedback
) {}
