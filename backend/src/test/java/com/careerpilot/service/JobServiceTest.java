package com.careerpilot.service;

import com.careerpilot.dto.JobMatchResponse;
import com.careerpilot.dto.JobSummaryResponse;
import com.careerpilot.dto.PageResponse;
import com.careerpilot.dto.SaveJobResponse;
import com.careerpilot.entity.*;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private JobMatchingService matchingService;

    @Mock
    private SkillNormalizer skillNormalizer;

    @InjectMocks
    private JobService jobService;

    private UUID userId;
    private UUID jobId;
    private Job testJob;
    private Profile testProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        testProfile = new Profile(userId, "Jane Doe", "Staff Engineer", "San Francisco, CA", "Staff Architect");

        testJob = new Job(
                "Senior Software Engineer",
                "Acme Cloud",
                "San Francisco, CA",
                "Build cloud distributed backends",
                "FULL_TIME",
                "https://example.com/job"
        );
        testJob.setId(jobId);
        testJob.setRemoteType("REMOTE");
        testJob.setExperienceLevel("SENIOR");
        testJob.setSalaryMin(BigDecimal.valueOf(140000));
        testJob.setSalaryMax(BigDecimal.valueOf(180000));
        testJob.setSalaryCurrency("USD");
        testJob.setPostedAt(Instant.now());
        testJob.setActive(true);
    }

    @Test
    @DisplayName("Should retrieve paginated jobs and identify saved status")
    void testGetJobs() {
        Pageable pageable = PageRequest.of(0, 10);
        when(jobRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(testJob), pageable, 1));
        when(savedJobRepository.findSavedJobIdsByUserId(userId))
                .thenReturn(Set.of(jobId));
        when(userSkillRepository.findByProfileUserId(userId))
                .thenReturn(Collections.emptyList());
        when(matchingService.calculateMatch(any(), any()))
                .thenReturn(new JobMatchResponse(jobId, true, 80, 2, 2, List.of("Java"), Collections.emptyList(), 0, 0, Collections.emptyList(), "Good match"));

        PageResponse<JobSummaryResponse> response = jobService.getJobs(
                "Senior", "San Francisco", "FULL_TIME", "REMOTE", "SENIOR", null, userId, pageable
        );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        JobSummaryResponse summary = response.content().get(0);
        assertEquals("Senior Software Engineer", summary.title());
        assertEquals("Acme Cloud", summary.company());
        assertTrue(summary.isSaved(), "Job should be flagged as saved for authenticated user");
    }

    @Test
    @DisplayName("Should successfully save job for authenticated user and remain idempotent")
    void testSaveJobIdempotent() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(savedJobRepository.existsByUserUserIdAndJobId(userId, jobId)).thenReturn(false);
        when(profileRepository.findById(userId)).thenReturn(Optional.of(testProfile));

        SaveJobResponse response = jobService.saveJob(userId, jobId);

        assertTrue(response.isSaved());
        assertEquals(jobId, response.jobId());
        verify(savedJobRepository, times(1)).save(any(SavedJob.class));

        // When already saved, should not call save again
        when(savedJobRepository.existsByUserUserIdAndJobId(userId, jobId)).thenReturn(true);
        SaveJobResponse response2 = jobService.saveJob(userId, jobId);
        assertTrue(response2.isSaved());
        verify(savedJobRepository, times(1)).save(any(SavedJob.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when saving nonexistent job")
    void testSaveJobNotFound() {
        UUID nonexistent = UUID.randomUUID();
        when(jobRepository.findById(nonexistent)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobService.saveJob(userId, nonexistent));
    }

    @Test
    @DisplayName("Should remove job from saved list")
    void testUnsaveJob() {
        when(savedJobRepository.existsByUserUserIdAndJobId(userId, jobId)).thenReturn(true);

        SaveJobResponse response = jobService.unsaveJob(userId, jobId);

        assertFalse(response.isSaved());
        verify(savedJobRepository, times(1)).deleteByUserUserIdAndJobId(userId, jobId);
    }

    @Test
    @DisplayName("Should return empty recommendations when user has no cataloged skills")
    void testRecommendationsNoUserSkills() {
        when(userSkillRepository.findByProfileUserId(userId)).thenReturn(Collections.emptyList());
        when(jobRepository.findAllActiveWithSkills()).thenReturn(List.of(testJob));
        when(savedJobRepository.findSavedJobIdsByUserId(userId)).thenReturn(Collections.emptySet());

        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<JobSummaryResponse> response = jobService.getRecommendations(userId, pageable);

        assertNotNull(response);
        assertEquals(0, response.content().size());
        assertEquals(0, response.totalElements());
    }
}
