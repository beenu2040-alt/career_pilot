package com.careerpilot.dto;

import java.util.List;
import java.util.Map;

public record CareerAnalyticsDto(
        int totalApplications,
        int submittedApplications,
        int wishlistCount,
        int activePipelineCount,
        int interviewCount,
        double interviewRate,
        int offerCount,
        double offerRate,
        Map<String, Integer> statusDistribution,
        List<TimelineDataPointDto> timeline
) {}
