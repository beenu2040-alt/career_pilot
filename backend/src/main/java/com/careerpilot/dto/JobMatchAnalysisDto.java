package com.careerpilot.dto;

import java.util.List;
import java.util.UUID;

public record JobMatchAnalysisDto(
        UUID jobId,
        String jobTitle,
        String company,
        List<String> matchedSkills,
        List<String> missingRequiredSkills,
        List<String> missingOptionalSkills,
        Integer matchScore,
        List<String> tailoredRecommendations
) {}
