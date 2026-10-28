package com.careerpilot.service;

import com.careerpilot.dto.CareerAnalyticsDto;
import com.careerpilot.entity.Application;
import com.careerpilot.entity.ApplicationHistory;
import com.careerpilot.entity.Profile;
import com.careerpilot.repository.ApplicationHistoryRepository;
import com.careerpilot.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareerAnalyticsServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationHistoryRepository historyRepository;

    @InjectMocks
    private CareerAnalyticsService analyticsService;

    private final UUID userId = UUID.randomUUID();
    private Profile userProfile;

    @BeforeEach
    void setUp() {
        userProfile = new Profile(userId);
    }

    @Test
    @DisplayName("getAnalytics should safely handle zero applications and zero denominator without division by zero")
    void testGetAnalytics_ZeroApplications_SafeDefaults() {
        when(applicationRepository.findByProfileUserId(userId)).thenReturn(List.of());
        when(historyRepository.findByUserUserId(userId)).thenReturn(List.of());

        CareerAnalyticsDto result = analyticsService.getAnalytics(userId);

        assertNotNull(result);
        assertEquals(0, result.totalApplications());
        assertEquals(0, result.submittedApplications());
        assertEquals(0, result.wishlistCount());
        assertEquals(0, result.interviewCount());
        assertEquals(0.0, result.interviewRate(), 0.001);
        assertEquals(0, result.offerCount());
        assertEquals(0.0, result.offerRate(), 0.001);
        assertNotNull(result.statusDistribution());
        assertEquals(0, result.statusDistribution().get("APPLIED"));
    }

    @Test
    @DisplayName("getAnalytics should accurately calculate interview and offer rates and count distinct applications once")
    void testGetAnalytics_WithApplicationsAndHistory() {
        // App 1: In INTERVIEW status (Submitted)
        Application app1 = new Application(userProfile, null, "Acme", "Backend Engineer", null, null, null, null, "INTERVIEW", Instant.now(), null, null, null);
        app1.setId(UUID.randomUUID());

        // App 2: In OFFER status (Submitted)
        Application app2 = new Application(userProfile, null, "Beta Corp", "Fullstack Lead", null, null, null, null, "OFFER", Instant.now(), null, null, null);
        app2.setId(UUID.randomUUID());

        // App 3: In REJECTED status, but had an INTERVIEW in history (Submitted)
        Application app3 = new Application(userProfile, null, "Gamma Inc", "DevOps Eng", null, null, null, null, "REJECTED", Instant.now(), null, null, null);
        app3.setId(UUID.randomUUID());

        // App 4: In APPLIED status (Submitted, no interview)
        Application app4 = new Application(userProfile, null, "Delta LLC", "Software Eng", null, null, null, null, "APPLIED", Instant.now(), null, null, null);
        app4.setId(UUID.randomUUID());

        // App 5: In WISHLIST (Not submitted, excluded from conversion denominators)
        Application app5 = new Application(userProfile, null, "Epsilon Co", "Junior Eng", null, null, null, null, "WISHLIST", null, null, null, null);
        app5.setId(UUID.randomUUID());

        when(applicationRepository.findByProfileUserId(userId)).thenReturn(List.of(app1, app2, app3, app4, app5));

        // Status histories for App 3 (had repeated transitions: APPLIED -> INTERVIEW -> ASSESSMENT -> REJECTED)
        ApplicationHistory h1 = new ApplicationHistory(app3, userProfile, "APPLIED", "INTERVIEW", "Interview scheduled");
        ApplicationHistory h2 = new ApplicationHistory(app3, userProfile, "INTERVIEW", "ASSESSMENT", "Coding test");
        ApplicationHistory h3 = new ApplicationHistory(app3, userProfile, "ASSESSMENT", "REJECTED", "Rejection notice");

        when(historyRepository.findByUserUserId(userId)).thenReturn(List.of(h1, h2, h3));

        CareerAnalyticsDto result = analyticsService.getAnalytics(userId);

        assertNotNull(result);
        assertEquals(5, result.totalApplications(), "Total applications should be 5");
        assertEquals(4, result.submittedApplications(), "Submitted applications should exclude wishlist (4 total)");
        assertEquals(1, result.wishlistCount(), "Wishlist count should be 1");

        // Distinct interview apps: app1 (current INTERVIEW), app2 (current OFFER), app3 (history INTERVIEW/ASSESSMENT) = 3 distinct
        assertEquals(3, result.interviewCount(), "Distinct interview applications should be 3");
        // Interview rate = 3 / 4 * 100 = 75.0%
        assertEquals(75.0, result.interviewRate(), 0.001);

        // Distinct offer apps: app2 = 1 distinct
        assertEquals(1, result.offerCount(), "Distinct offer count should be 1");
        // Offer rate = 1 / 4 * 100 = 25.0%
        assertEquals(25.0, result.offerRate(), 0.001);

        // Active pipeline count: app1 (INTERVIEW), app4 (APPLIED) = 2
        assertEquals(2, result.activePipelineCount());
    }
}
