package com.careerpilot.controller;

import com.careerpilot.config.CorsConfig;
import com.careerpilot.config.CorsProperties;
import com.careerpilot.config.JwtDecoderConfig;
import com.careerpilot.config.JwtProperties;
import com.careerpilot.config.SecurityConfig;
import com.careerpilot.dto.*;
import com.careerpilot.entity.ResumeAnalysisStatus;
import com.careerpilot.entity.ResumeProcessingStatus;
import com.careerpilot.security.RestAccessDeniedHandler;
import com.careerpilot.security.RestAuthenticationEntryPoint;
import com.careerpilot.service.ResumeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ResumeController.class)
@Import({
    SecurityConfig.class,
    CorsConfig.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    JwtDecoderConfig.class
})
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeService resumeService;

    private final UUID testUserId = UUID.randomUUID();
    private final UUID testResumeId = UUID.randomUUID();
    private final UUID testAnalysisId = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/resumes without auth should return 401 Unauthorized")
    void testUploadResume_Unauthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "%PDF-sample".getBytes()
        );

        mockMvc.perform(multipart("/api/resumes").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/resumes with JWT should return 201 Created and summary DTO")
    void testUploadResume_Authenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "%PDF-sample".getBytes()
        );

        ResumeSummaryDto summary = new ResumeSummaryDto(
                testResumeId,
                "resume.pdf",
                1024L,
                "application/pdf",
                ResumeProcessingStatus.UPLOADED,
                Instant.now(),
                Instant.now(),
                null,
                null
        );

        when(resumeService.uploadResume(eq(testUserId), any())).thenReturn(summary);

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(testResumeId.toString())))
                .andExpect(jsonPath("$.originalFilename", is("resume.pdf")))
                .andExpect(jsonPath("$.processingStatus", is("UPLOADED")));
    }

    @Test
    @DisplayName("GET /api/resumes should return user resume list")
    void testGetUserResumes() throws Exception {
        ResumeSummaryDto summary = new ResumeSummaryDto(
                testResumeId,
                "resume.pdf",
                1024L,
                "application/pdf",
                ResumeProcessingStatus.COMPLETED,
                Instant.now(),
                Instant.now(),
                85,
                testAnalysisId
        );

        when(resumeService.getUserResumes(testUserId)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/resumes")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].latestScore", is(85)));
    }

    @Test
    @DisplayName("GET /api/resumes/{id} should return single resume metadata")
    void testGetResumeById() throws Exception {
        ResumeSummaryDto summary = new ResumeSummaryDto(
                testResumeId,
                "resume.pdf",
                1024L,
                "application/pdf",
                ResumeProcessingStatus.UPLOADED,
                Instant.now(),
                Instant.now(),
                null,
                null
        );

        when(resumeService.getResumeById(testResumeId, testUserId)).thenReturn(summary);

        mockMvc.perform(get("/api/resumes/" + testResumeId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testResumeId.toString())));
    }

    @Test
    @DisplayName("DELETE /api/resumes/{id} should return 204 No Content")
    void testDeleteResume() throws Exception {
        doNothing().when(resumeService).deleteResume(testResumeId, testUserId);

        mockMvc.perform(delete("/api/resumes/" + testResumeId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/resumes/{id}/analyze should return completed analysis DTO")
    void testAnalyzeResume() throws Exception {
        ResumeAnalysisDto analysisDto = new ResumeAnalysisDto(
                testAnalysisId,
                testResumeId,
                null,
                ResumeAnalysisStatus.COMPLETED,
                88,
                "Strong candidate",
                List.of(new DimensionScoreDto("Skills", 23, 25, 25, "HYBRID", "Solid")),
                new ExtractedDataDto("Candidate", "test@test.com", "555", "SF", List.of(), 600, 4, List.of(), List.of()),
                List.of(new StrengthDto("Impact", "Clear numbers", "40% faster")),
                List.of(),
                new SkillAnalysisDto(List.of(), List.of(), List.of()),
                new ExperienceAnalysisDto(2, "STRONG", "QUANTIFIED", List.of()),
                null,
                "careerpilot-deterministic-rubric-v1",
                Instant.now(),
                Instant.now()
        );

        when(resumeService.analyzeResume(eq(testResumeId), eq(testUserId), any())).thenReturn(analysisDto);

        mockMvc.perform(post("/api/resumes/" + testResumeId + "/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore", is(88)))
                .andExpect(jsonPath("$.summary", is("Strong candidate")))
                .andExpect(jsonPath("$.analysisStatus", is("COMPLETED")));
    }

    @Test
    @DisplayName("GET /api/resumes/{id}/analyses should return analysis list")
    void testGetResumeAnalyses() throws Exception {
        ResumeAnalysisDto analysisDto = new ResumeAnalysisDto(
                testAnalysisId,
                testResumeId,
                null,
                ResumeAnalysisStatus.COMPLETED,
                88,
                "Summary",
                List.of(),
                new ExtractedDataDto("Candidate", "", "", "", List.of(), 500, 2, List.of(), List.of()),
                List.of(),
                List.of(),
                new SkillAnalysisDto(List.of(), List.of(), List.of()),
                new ExperienceAnalysisDto(1, "STRONG", "QUANTIFIED", List.of()),
                null,
                "test",
                Instant.now(),
                Instant.now()
        );

        when(resumeService.getResumeAnalyses(testResumeId, testUserId)).thenReturn(List.of(analysisDto));

        mockMvc.perform(get("/api/resumes/" + testResumeId + "/analyses")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].overallScore", is(88)));
    }
}
