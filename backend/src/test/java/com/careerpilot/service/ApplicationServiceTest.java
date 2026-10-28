package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.Application;
import com.careerpilot.entity.ApplicationHistory;
import com.careerpilot.entity.ApplicationStatus;
import com.careerpilot.entity.Job;
import com.careerpilot.entity.Profile;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.ApplicationHistoryRepository;
import com.careerpilot.repository.ApplicationRepository;
import com.careerpilot.repository.JobRepository;
import com.careerpilot.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationHistoryRepository historyRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final UUID appId = UUID.randomUUID();

    private Profile userProfile;
    private Job testJob;
    private Application testApp;

    @BeforeEach
    void setUp() {
        userProfile = new Profile(userId);

        testJob = new Job();
        testJob.setId(jobId);
        testJob.setTitle("Lead Architect");
        testJob.setCompany("Stripe");
        testJob.setDescription("Java & Distributed Systems");
        testJob.setSourceUrl("https://stripe.com/jobs/123");

        testApp = new Application(
                userProfile,
                testJob,
                "Stripe",
                "Lead Architect",
                "Java & Distributed Systems",
                "https://stripe.com/jobs/123",
                "Alice Recruiter",
                "alice@stripe.com",
                "APPLIED",
                Instant.now(),
                LocalDate.now().plusDays(3),
                null,
                "Applied via portal"
        );
        testApp.setId(appId);
    }

    @Test
    @DisplayName("createApplication with stored job should succeed and record initial history")
    void testCreateApplication_WithStoredJob_Success() {
        CreateApplicationRequest req = new CreateApplicationRequest(
                jobId, null, null, null, null, "Alice", "alice@stripe.com", null, ApplicationStatus.APPLIED, null, null, null, "Applied"
        );

        when(profileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(applicationRepository.existsByProfileUserIdAndJobId(userId, jobId)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(appId);
            return a;
        });

        ApplicationDto result = applicationService.createApplication(userId, req);

        assertNotNull(result);
        assertEquals("Stripe", result.companyName());
        assertEquals("Lead Architect", result.jobTitle());
        assertEquals(ApplicationStatus.APPLIED, result.status());

        verify(historyRepository, times(1)).save(any(ApplicationHistory.class));
    }

    @Test
    @DisplayName("createApplication with duplicate stored job should throw IllegalStateException")
    void testCreateApplication_DuplicateJob_ThrowsException() {
        CreateApplicationRequest req = new CreateApplicationRequest(
                jobId, null, null, null, null, null, null, null, ApplicationStatus.WISHLIST, null, null, null, null
        );

        when(profileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(applicationRepository.existsByProfileUserIdAndJobId(userId, jobId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> applicationService.createApplication(userId, req));
    }

    @Test
    @DisplayName("createApplication manually without stored job should succeed")
    void testCreateApplication_ManualJob_Success() {
        CreateApplicationRequest req = new CreateApplicationRequest(
                null, "Google", "Staff SRE", "Reliability engineering", "https://google.com/careers", null, null, null, ApplicationStatus.WISHLIST, null, null, null, "Referred by John"
        );

        when(profileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        ApplicationDto result = applicationService.createApplication(userId, req);

        assertNotNull(result);
        assertEquals("Google", result.companyName());
        assertEquals("Staff SRE", result.jobTitle());
        assertEquals(ApplicationStatus.WISHLIST, result.status());
    }

    @Test
    @DisplayName("updateStatus should update application status and append to history")
    void testUpdateStatus_Success() {
        when(applicationRepository.findByIdAndProfileUserId(appId, userId)).thenReturn(Optional.of(testApp));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest(ApplicationStatus.INTERVIEW, "Round 1 scheduled");
        ApplicationDto updated = applicationService.updateStatus(appId, userId, req);

        assertNotNull(updated);
        assertEquals(ApplicationStatus.INTERVIEW, updated.status());
        verify(historyRepository, times(1)).save(any(ApplicationHistory.class));
    }

    @Test
    @DisplayName("getReminders should categorize overdue followups, due today, upcoming interviews, and stale applications")
    void testGetReminders() {
        LocalDate today = LocalDate.now();

        Application overdueApp = new Application(userProfile, null, "Overdue Co", "Engineer", null, null, null, null, "APPLIED", Instant.now(), today.minusDays(2), null, null);
        overdueApp.setId(UUID.randomUUID());

        Application dueTodayApp = new Application(userProfile, null, "Due Co", "Engineer", null, null, null, null, "APPLIED", Instant.now(), today, null, null);
        dueTodayApp.setId(UUID.randomUUID());

        Application interviewApp = new Application(userProfile, null, "Interview Co", "Engineer", null, null, null, null, "INTERVIEW", Instant.now(), null, Instant.now().plus(2, ChronoUnit.DAYS), null);
        interviewApp.setId(UUID.randomUUID());

        Application staleApp = new Application(userProfile, null, "Stale Co", "Engineer", null, null, null, null, "APPLIED", Instant.now().minus(20, ChronoUnit.DAYS), null, null, null);
        staleApp.setId(UUID.randomUUID());

        when(applicationRepository.findActiveFollowUps(userId)).thenReturn(List.of(overdueApp, dueTodayApp));
        when(applicationRepository.findUpcomingInterviews(eq(userId), any())).thenReturn(List.of(interviewApp));
        when(applicationRepository.findStaleApplications(eq(userId), any())).thenReturn(List.of(staleApp));

        RemindersSummaryDto reminders = applicationService.getReminders(userId);

        assertNotNull(reminders);
        assertEquals(1, reminders.overdue().size(), "Should have 1 overdue follow-up");
        assertEquals("Overdue Co", reminders.overdue().get(0).companyName());

        assertEquals(1, reminders.dueToday().size(), "Should have 1 due-today follow-up");
        assertEquals("Due Co", reminders.dueToday().get(0).companyName());

        assertEquals(1, reminders.upcomingInterviews().size(), "Should have 1 upcoming interview");
        assertEquals(1, reminders.stale().size(), "Should have 1 stale application");
    }

    @Test
    @DisplayName("Cross-user ownership check: unauthorized user accessing another user's application throws ResourceNotFoundException")
    void testCrossUserOwnershipViolation_ThrowsException() {
        when(applicationRepository.findByIdAndProfileUserIdWithJob(appId, otherUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.getApplicationById(appId, otherUserId)
        );
    }

    @Test
    @DisplayName("deleteApplication should remove application if user owns it")
    void testDeleteApplication_Success() {
        when(applicationRepository.findByIdAndProfileUserId(appId, userId)).thenReturn(Optional.of(testApp));
        doNothing().when(applicationRepository).delete(testApp);

        assertDoesNotThrow(() -> applicationService.deleteApplication(appId, userId));
        verify(applicationRepository, times(1)).delete(testApp);
    }
}
