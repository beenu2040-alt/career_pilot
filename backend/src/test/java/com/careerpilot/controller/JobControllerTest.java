package com.careerpilot.controller;

import com.careerpilot.config.CorsConfig;
import com.careerpilot.config.CorsProperties;
import com.careerpilot.config.JwtDecoderConfig;
import com.careerpilot.config.JwtProperties;
import com.careerpilot.config.SecurityConfig;
import com.careerpilot.dto.*;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.security.RestAccessDeniedHandler;
import com.careerpilot.security.RestAuthenticationEntryPoint;
import com.careerpilot.service.JobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JobController.class)
@Import({
    SecurityConfig.class,
    CorsConfig.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    JwtDecoderConfig.class
})
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    private final UUID testJobId = UUID.randomUUID();
    private final UUID testUserId = UUID.randomUUID();

    private JobSummaryResponse createSampleSummary() {
        return new JobSummaryResponse(
                testJobId,
                "Senior Cloud Engineer",
                "Acme Cloud",
                "San Francisco, CA",
                "FULL_TIME",
                "REMOTE",
                "SENIOR",
                BigDecimal.valueOf(140000),
                BigDecimal.valueOf(180000),
                "USD",
                Instant.now(),
                true,
                List.of(new SkillDto(UUID.randomUUID(), "Java", "Backend", true)),
                Collections.emptyList(),
                false,
                80
        );
    }

    @Test
    @DisplayName("GET /api/jobs is publicly accessible and returns 200 OK")
    void testGetJobsPublic() throws Exception {
        PageResponse<JobSummaryResponse> pageResponse = PageResponse.of(
                List.of(createSampleSummary()), 0, 10, 1
        );

        when(jobService.getJobs(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/jobs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", is("Senior Cloud Engineer")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/jobs/{id} returns job details when job exists")
    void testGetJobDetail() throws Exception {
        JobDetailResponse detailResponse = new JobDetailResponse(
                testJobId,
                "Senior Cloud Engineer",
                "Acme Cloud",
                "San Francisco, CA",
                "Detailed description",
                "FULL_TIME",
                "REMOTE",
                "SENIOR",
                BigDecimal.valueOf(140000),
                BigDecimal.valueOf(180000),
                "USD",
                "https://example.com/job",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                true,
                Collections.emptyList(),
                Collections.emptyList(),
                false,
                null
        );

        when(jobService.getJobDetail(eq(testJobId), any()))
                .thenReturn(detailResponse);

        mockMvc.perform(get("/api/jobs/" + testJobId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testJobId.toString())))
                .andExpect(jsonPath("$.company", is("Acme Cloud")));
    }

    @Test
    @DisplayName("GET /api/jobs/{id} returns 404 when job does not exist")
    void testGetJobDetailNotFound() throws Exception {
        UUID nonexistent = UUID.randomUUID();
        when(jobService.getJobDetail(eq(nonexistent), any()))
                .thenThrow(new ResourceNotFoundException("Job not found"));

        mockMvc.perform(get("/api/jobs/" + nonexistent)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("GET /api/jobs/saved without JWT returns 401 Unauthorized")
    void testGetSavedJobsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/jobs/saved")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/jobs/saved with valid JWT returns 200 OK")
    void testGetSavedJobsAuthenticated() throws Exception {
        PageResponse<JobSummaryResponse> page = PageResponse.of(
                List.of(createSampleSummary()), 0, 10, 1
        );

        when(jobService.getSavedJobs(eq(testUserId), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/jobs/saved")
                .with(jwt().jwt(b -> b.subject(testUserId.toString())))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("POST /api/jobs/{id}/save without JWT returns 401 Unauthorized")
    void testSaveJobUnauthorized() throws Exception {
        mockMvc.perform(post("/api/jobs/" + testJobId + "/save")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/jobs/{id}/save with JWT saves job and returns 200 OK")
    void testSaveJobAuthenticated() throws Exception {
        when(jobService.saveJob(eq(testUserId), eq(testJobId)))
                .thenReturn(new SaveJobResponse(testJobId, true, "Job saved successfully"));

        mockMvc.perform(post("/api/jobs/" + testJobId + "/save")
                .with(jwt().jwt(b -> b.subject(testUserId.toString())))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSaved", is(true)))
                .andExpect(jsonPath("$.jobId", is(testJobId.toString())));
    }

    @Test
    @DisplayName("DELETE /api/jobs/{id}/save with JWT removes saved job")
    void testUnsaveJobAuthenticated() throws Exception {
        when(jobService.unsaveJob(eq(testUserId), eq(testJobId)))
                .thenReturn(new SaveJobResponse(testJobId, false, "Job removed from saved jobs"));

        mockMvc.perform(delete("/api/jobs/" + testJobId + "/save")
                .with(jwt().jwt(b -> b.subject(testUserId.toString())))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSaved", is(false)));
    }

    @Test
    @DisplayName("GET /api/jobs/recommendations with valid JWT returns personalized recommendations")
    void testGetRecommendationsAuthenticated() throws Exception {
        PageResponse<JobSummaryResponse> page = PageResponse.of(
                List.of(createSampleSummary()), 0, 10, 1
        );

        when(jobService.getRecommendations(eq(testUserId), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/jobs/recommendations")
                .with(jwt().jwt(b -> b.subject(testUserId.toString())))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].matchPercentage", is(80)));
    }

    @Test
    @DisplayName("GET /api/jobs/{id}/match with valid JWT returns explainable match")
    void testGetJobMatchAuthenticated() throws Exception {
        JobMatchResponse match = new JobMatchResponse(
                testJobId,
                true,
                75,
                3,
                4,
                List.of("Java", "Spring Boot", "TypeScript"),
                List.of("React"),
                1,
                1,
                List.of("Docker"),
                "Strong match: You satisfy 3 of 4 required skills (75%). Missing: React."
        );

        when(jobService.getJobMatch(eq(testJobId), eq(testUserId)))
                .thenReturn(match);

        mockMvc.perform(get("/api/jobs/" + testJobId + "/match")
                .with(jwt().jwt(b -> b.subject(testUserId.toString())))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPercentage", is(75)))
                .andExpect(jsonPath("$.matchedRequiredCount", is(3)))
                .andExpect(jsonPath("$.missingRequiredSkills[0]", is("React")));
    }
}
