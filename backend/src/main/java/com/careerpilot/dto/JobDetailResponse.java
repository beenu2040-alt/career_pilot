package com.careerpilot.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobDetailResponse(
        UUID id,
        String title,
        String company,
        String location,
        String description,
        String employmentType,
        String remoteType,
        String experienceLevel,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String sourceUrl,
        Instant postedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean isSample,
        List<SkillDto> requiredSkills,
        List<SkillDto> optionalSkills,
        boolean isSaved,
        JobMatchResponse match
) {}
