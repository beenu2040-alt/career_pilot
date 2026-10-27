package com.careerpilot.dto;

import java.util.List;

public record ExperienceAnalysisDto(
        int totalRolesIdentified,
        String actionVerbRating, // STRONG, MODERATE, NEEDS_IMPROVEMENT
        String quantifiableMetricRating,
        List<String> roleSummaries
) {}
