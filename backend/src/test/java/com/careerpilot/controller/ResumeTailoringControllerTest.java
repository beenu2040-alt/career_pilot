package com.careerpilot.controller;

import com.careerpilot.config.CorsConfig;
import com.careerpilot.config.CorsProperties;
import com.careerpilot.config.JwtDecoderConfig;
import com.careerpilot.config.JwtProperties;
import com.careerpilot.config.SecurityConfig;
import com.careerpilot.dto.*;
import com.careerpilot.entity.SuggestionSection;
import com.careerpilot.entity.SuggestionStatus;
import com.careerpilot.entity.TailoringSessionStatus;
import com.careerpilot.security.RestAccessDeniedHandler;
import com.careerpilot.security.RestAuthenticationEntryPoint;
import com.careerpilot.service.ResumeTailoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ResumeTailoringController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtDecoderConfig.class
})
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
class ResumeTailoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeTailoringService tailoringService;

    private final UUID testUserId = UUID.randomUUID();
    private final UUID testResumeId = UUID.randomUUID();
    private final UUID testJobId = UUID.randomUUID();
    private final UUID testSessionId = UUID.randomUUID();
    private final UUID testSuggestionId = UUID.randomUUID();
    private final UUID testVersionId = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/resume-tailoring unauthenticated should return 401 Unauthorized")
    void testCreateSession_Unauthenticated() throws Exception {
        CreateTailoringSessionRequest req = new CreateTailoringSessionRequest(
                testResumeId, testJobId, "Dev", "Co", null
        );

        mockMvc.perform(post("/api/resume-tailoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/resume-tailoring authenticated should return 201 Created")
    void testCreateSession_Authenticated() throws Exception {
        CreateTailoringSessionRequest req = new CreateTailoringSessionRequest(
                testResumeId, testJobId, "Lead Engineer", "Acme", null
        );

        TailoringSessionDto responseDto = new TailoringSessionDto(
                testSessionId,
                testResumeId,
                "resume.pdf",
                testJobId,
                "Lead Engineer",
                "Acme",
                TailoringSessionStatus.CREATED,
                "Lead Engineer",
                null,
                null,
                null,
                0,
                Instant.now(),
                Instant.now()
        );

        when(tailoringService.createSession(eq(testUserId), any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/resume-tailoring")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(testSessionId.toString())))
                .andExpect(jsonPath("$.jobTitle", is("Lead Engineer")))
                .andExpect(jsonPath("$.status", is("CREATED")));
    }

    @Test
    @DisplayName("GET /api/resume-tailoring should return list of sessions")
    void testGetUserSessions() throws Exception {
        TailoringSessionDto session1 = new TailoringSessionDto(
                testSessionId,
                testResumeId,
                "resume.pdf",
                testJobId,
                "Lead Engineer",
                "Acme",
                TailoringSessionStatus.SUGGESTIONS_READY,
                "Lead Engineer",
                "gemini:1.5-flash",
                65,
                88,
                1,
                Instant.now(),
                Instant.now()
        );

        when(tailoringService.getUserSessions(testUserId)).thenReturn(List.of(session1));

        mockMvc.perform(get("/api/resume-tailoring")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].jobTitle", is("Lead Engineer")));
    }

    @Test
    @DisplayName("GET /api/resume-tailoring/{id} should return session detail")
    void testGetSessionDetails() throws Exception {
        TailoringSessionDetailDto detail = new TailoringSessionDetailDto(
                testSessionId,
                testResumeId,
                "resume.pdf",
                testJobId,
                "Lead Engineer",
                "Acme",
                "Job requirements snapshot",
                TailoringSessionStatus.SUGGESTIONS_READY,
                "Lead Engineer",
                "gemini:1.5-flash",
                65,
                88,
                null,
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now()
        );

        when(tailoringService.getSessionDetails(testSessionId, testUserId)).thenReturn(detail);

        mockMvc.perform(get("/api/resume-tailoring/{id}", testSessionId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testSessionId.toString())))
                .andExpect(jsonPath("$.companyName", is("Acme")));
    }

    @Test
    @DisplayName("POST /api/resume-tailoring/{id}/generate should trigger suggestion generation")
    void testGenerateSuggestions() throws Exception {
        TailoringSessionDetailDto detail = new TailoringSessionDetailDto(
                testSessionId,
                testResumeId,
                "resume.pdf",
                testJobId,
                "Lead Engineer",
                "Acme",
                "Job requirements snapshot",
                TailoringSessionStatus.SUGGESTIONS_READY,
                "Lead Engineer",
                "gemini:1.5-flash",
                65,
                88,
                null,
                List.of(
                        new TailoringSuggestionDto(
                                testSuggestionId,
                                testSessionId,
                                SuggestionSection.SUMMARY,
                                "Reframe Summary",
                                "Old summary",
                                "New summary",
                                null,
                                "Aligns with role",
                                SuggestionStatus.PENDING,
                                false,
                                null,
                                1,
                                Instant.now(),
                                Instant.now()
                        )
                ),
                List.of(),
                Instant.now(),
                Instant.now()
        );

        when(tailoringService.generateSuggestions(testSessionId, testUserId)).thenReturn(detail);

        mockMvc.perform(post("/api/resume-tailoring/{id}/generate", testSessionId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions", hasSize(1)))
                .andExpect(jsonPath("$.suggestions[0].title", is("Reframe Summary")));
    }

    @Test
    @DisplayName("PATCH /api/resume-tailoring/{id}/suggestions/{suggestionId} should update suggestion")
    void testUpdateSuggestion() throws Exception {
        UpdateSuggestionRequest req = new UpdateSuggestionRequest(SuggestionStatus.ACCEPTED, null);
        TailoringSuggestionDto updated = new TailoringSuggestionDto(
                testSuggestionId,
                testSessionId,
                SuggestionSection.SUMMARY,
                "Reframe Summary",
                "Old summary",
                "New summary",
                null,
                "Aligns with role",
                SuggestionStatus.ACCEPTED,
                false,
                null,
                1,
                Instant.now(),
                Instant.now()
        );

        when(tailoringService.updateSuggestion(eq(testSessionId), eq(testSuggestionId), eq(testUserId), any()))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/resume-tailoring/{id}/suggestions/{suggestionId}", testSessionId, testSuggestionId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));
    }

    @Test
    @DisplayName("POST /api/resume-tailoring/{id}/versions should save reviewed version")
    void testSaveVersion() throws Exception {
        SaveTailoredVersionRequest req = new SaveTailoredVersionRequest("v1 - Tailored", "Applied suggestions", null);
        TailoredResumeVersionDto version = new TailoredResumeVersionDto(
                testVersionId,
                testSessionId,
                1,
                "v1 - Tailored",
                null,
                "Plain text resume",
                "Applied suggestions",
                2,
                null,
                Instant.now()
        );

        when(tailoringService.saveVersion(eq(testSessionId), eq(testUserId), any())).thenReturn(version);

        mockMvc.perform(post("/api/resume-tailoring/{id}/versions", testSessionId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber", is(1)))
                .andExpect(jsonPath("$.versionName", is("v1 - Tailored")));
    }

    @Test
    @DisplayName("GET /api/resume-tailoring/{id}/versions/{versionId}/export should stream PDF")
    void testExportVersionPdf() throws Exception {
        byte[] fakePdfBytes = "%PDF-1.4 test stream".getBytes();
        when(tailoringService.exportVersionPdf(testSessionId, testVersionId, testUserId)).thenReturn(fakePdfBytes);

        mockMvc.perform(get("/api/resume-tailoring/{id}/versions/{versionId}/export", testSessionId, testVersionId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @DisplayName("DELETE /api/resume-tailoring/{id} should delete session")
    void testDeleteSession() throws Exception {
        doNothing().when(tailoringService).deleteSession(testSessionId, testUserId);

        mockMvc.perform(delete("/api/resume-tailoring/{id}", testSessionId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isNoContent());
    }
}
