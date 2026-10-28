package com.careerpilot.dto;

import com.careerpilot.entity.ApplicationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreateApplicationRequest(
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
        String notes
) {}
