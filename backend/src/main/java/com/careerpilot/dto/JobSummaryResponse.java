package com.careerpilot.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobSummaryResponse(
        UUID id,
        String title,
        String company,
        String location,
        String employmentType,
        String remoteType,
        String experienceLevel,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        Instant postedAt,
        boolean isSample,
        List<SkillDto> requiredSkills,
        List<SkillDto> optionalSkills,
        boolean isSaved,
        Integer matchPercentage
) {}
