package com.careerpilot.service;

import com.careerpilot.dto.CareerAnalyticsDto;
import com.careerpilot.dto.TimelineDataPointDto;
import com.careerpilot.entity.Application;
import com.careerpilot.entity.ApplicationHistory;
import com.careerpilot.entity.ApplicationStatus;
import com.careerpilot.repository.ApplicationHistoryRepository;
import com.careerpilot.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CareerAnalyticsService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationHistoryRepository historyRepository;

    private static final Set<String> INTERVIEW_STATUSES = Set.of(
            "SCREENING", "INTERVIEW", "ASSESSMENT", "OFFER", "INTERVIEWING"
    );

    private static final DateTimeFormatter TIMELINE_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy")
            .withZone(ZoneId.of("UTC"));

    public CareerAnalyticsService(
            ApplicationRepository applicationRepository,
            ApplicationHistoryRepository historyRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public CareerAnalyticsDto getAnalytics(UUID userId) {
        List<Application> apps = applicationRepository.findByProfileUserId(userId);
        List<ApplicationHistory> histories = historyRepository.findByUserUserId(userId);

        int totalApplications = apps.size();

        // 1. Group by current status
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            distribution.put(status.name(), 0);
        }

        int wishlistCount = 0;
        int activeCount = 0;
        int submittedCount = 0;

        for (Application a : apps) {
            String status = a.getStatus();
            distribution.put(status, distribution.getOrDefault(status, 0) + 1);

            if ("WISHLIST".equalsIgnoreCase(status)) {
                wishlistCount++;
            } else {
                submittedCount++;
            }

            if ("APPLIED".equalsIgnoreCase(status) || "SCREENING".equalsIgnoreCase(status)
                    || "INTERVIEW".equalsIgnoreCase(status) || "ASSESSMENT".equalsIgnoreCase(status)
                    || "INTERVIEWING".equalsIgnoreCase(status)) {
                activeCount++;
            }
        }

        // 2. Distinct applications that reached interview stage
        // Count distinct application IDs from current status or history transitions
        Set<UUID> interviewAppIds = new HashSet<>();
        for (Application a : apps) {
            if (INTERVIEW_STATUSES.contains(a.getStatus())) {
                interviewAppIds.add(a.getId());
            }
        }
        for (ApplicationHistory h : histories) {
            if (INTERVIEW_STATUSES.contains(h.getNewStatus())) {
                interviewAppIds.add(h.getApplication().getId());
            }
        }
        int interviewCount = interviewAppIds.size();

        // 3. Distinct applications that reached offer stage
        Set<UUID> offerAppIds = new HashSet<>();
        for (Application a : apps) {
            if ("OFFER".equalsIgnoreCase(a.getStatus())) {
                offerAppIds.add(a.getId());
            }
        }
        for (ApplicationHistory h : histories) {
            if ("OFFER".equalsIgnoreCase(h.getNewStatus())) {
                offerAppIds.add(h.getApplication().getId());
            }
        }
        int offerCount = offerAppIds.size();

        // 4. Conversion Rates (safe zero denominators)
        double interviewRate = 0.0;
        double offerRate = 0.0;
        if (submittedCount > 0) {
            interviewRate = Math.round(((double) interviewCount / submittedCount) * 1000.0) / 10.0;
            offerRate = Math.round(((double) offerCount / submittedCount) * 1000.0) / 10.0;
        }

        // 5. Timeline of applications submitted over time
        Map<String, Integer> timelineMap = new TreeMap<>();
        for (Application a : apps) {
            Instant dateToUse = a.getAppliedAt() != null ? a.getAppliedAt() : a.getCreatedAt();
            if (dateToUse != null && !"WISHLIST".equalsIgnoreCase(a.getStatus())) {
                String label = TIMELINE_FORMATTER.format(dateToUse);
                timelineMap.put(label, timelineMap.getOrDefault(label, 0) + 1);
            }
        }

        List<TimelineDataPointDto> timeline = new ArrayList<>();
        timelineMap.forEach((label, count) -> timeline.add(new TimelineDataPointDto(label, count)));

        return new CareerAnalyticsDto(
                totalApplications,
                submittedCount,
                wishlistCount,
                activeCount,
                interviewCount,
                interviewRate,
                offerCount,
                offerRate,
                distribution,
                timeline
        );
    }
}
