package com.careerpilot.dto;

import java.util.List;
import java.util.UUID;

public record JobMatchResponse(
        UUID jobId,
        boolean hasRequiredSkills,
        Integer matchPercentage,
        int matchedRequiredCount,
        int totalRequiredCount,
        List<String> matchedRequiredSkills,
        List<String> missingRequiredSkills,
        int matchedOptionalCount,
        int totalOptionalCount,
        List<String> matchedOptionalSkills,
        String explanation
) {}
