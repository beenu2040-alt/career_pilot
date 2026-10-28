package com.careerpilot.dto;

import java.util.List;

public record RemindersSummaryDto(
        List<ApplicationDto> overdue,
        List<ApplicationDto> dueToday,
        List<ApplicationDto> upcomingInterviews,
        List<ApplicationDto> stale
) {}
