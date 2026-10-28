package com.careerpilot.dto;

import com.careerpilot.entity.ApplicationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ApplicationDto(
        UUID id,
        UUID jobId,
        String companyName,
        String jobTitle,
        String jobDescription,
        String jobUrl,
        String contactName,
        String contactEmail,
        BigDecimal salaryOffered,
        ApplicationStatus status,
        Instant appliedAt,
        LocalDate nextFollowUpDate,
        Instant interviewAt,
        String notes,
        List<ApplicationHistoryDto> history,
        Instant createdAt,
        Instant updatedAt
) {}
