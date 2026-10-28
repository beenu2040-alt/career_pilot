package com.careerpilot.controller;

import com.careerpilot.config.CorsConfig;
import com.careerpilot.config.CorsProperties;
import com.careerpilot.config.JwtDecoderConfig;
import com.careerpilot.config.JwtProperties;
import com.careerpilot.config.SecurityConfig;
import com.careerpilot.dto.*;
import com.careerpilot.entity.ApplicationStatus;
import com.careerpilot.security.RestAccessDeniedHandler;
import com.careerpilot.security.RestAuthenticationEntryPoint;
import com.careerpilot.service.ApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(controllers = ApplicationController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtDecoderConfig.class
})
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationService applicationService;

    private final UUID testUserId = UUID.randomUUID();
    private final UUID testAppId = UUID.randomUUID();
    private final UUID testJobId = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/applications unauthenticated should return 401 Unauthorized")
    void testCreateApplication_Unauthenticated() throws Exception {
        CreateApplicationRequest req = new CreateApplicationRequest(
                testJobId, "Netflix", "Senior Platform Engineer", null, null, null, null, null, ApplicationStatus.APPLIED, null, null, null, null
        );

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/applications authenticated should return 201 Created")
    void testCreateApplication_Authenticated() throws Exception {
        CreateApplicationRequest req = new CreateApplicationRequest(
                testJobId, "Netflix", "Senior Platform Engineer", null, null, null, null, null, ApplicationStatus.APPLIED, null, null, null, null
        );

        ApplicationDto dto = new ApplicationDto(
                testAppId,
                testJobId,
                "Netflix",
                "Senior Platform Engineer",
                null,
                null,
                null,
                null,
                null,
                ApplicationStatus.APPLIED,
                Instant.now(),
                null,
                null,
                null,
                List.of(),
                Instant.now(),
                Instant.now()
        );

        when(applicationService.createApplication(eq(testUserId), any())).thenReturn(dto);

        mockMvc.perform(post("/api/applications")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(testAppId.toString())))
                .andExpect(jsonPath("$.companyName", is("Netflix")))
                .andExpect(jsonPath("$.status", is("APPLIED")));
    }

    @Test
    @DisplayName("GET /api/applications should return paginated list")
    void testGetApplications() throws Exception {
        ApplicationDto dto = new ApplicationDto(
                testAppId, testJobId, "Netflix", "Senior Platform Engineer", null, null, null, null, null, ApplicationStatus.APPLIED, Instant.now(), null, null, null, List.of(), Instant.now(), Instant.now()
        );

        PageResponse<ApplicationDto> page = new PageResponse<>(
                List.of(dto), 0, 20, 1, 1, true, true
        );

        when(applicationService.getApplications(eq(testUserId), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/applications")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].companyName", is("Netflix")));
    }

    @Test
    @DisplayName("GET /api/applications/reminders should return reminder summaries")
    void testGetReminders() throws Exception {
        RemindersSummaryDto reminders = new RemindersSummaryDto(
                List.of(), List.of(), List.of(), List.of()
        );

        when(applicationService.getReminders(testUserId)).thenReturn(reminders);

        mockMvc.perform(get("/api/applications/reminders")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue", hasSize(0)));
    }

    @Test
    @DisplayName("PATCH /api/applications/{id}/status should update status")
    void testUpdateStatus() throws Exception {
        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest(ApplicationStatus.INTERVIEW, "Interview confirmed");
        ApplicationDto dto = new ApplicationDto(
                testAppId, testJobId, "Netflix", "Senior Platform Engineer", null, null, null, null, null, ApplicationStatus.INTERVIEW, Instant.now(), null, null, null, List.of(), Instant.now(), Instant.now()
        );

        when(applicationService.updateStatus(eq(testAppId), eq(testUserId), any())).thenReturn(dto);

        mockMvc.perform(patch("/api/applications/{id}/status", testAppId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INTERVIEW")));
    }

    @Test
    @DisplayName("DELETE /api/applications/{id} should return 204 No Content")
    void testDeleteApplication() throws Exception {
        doNothing().when(applicationService).deleteApplication(testAppId, testUserId);

        mockMvc.perform(delete("/api/applications/{id}", testAppId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isNoContent());
    }
}
