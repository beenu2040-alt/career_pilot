package com.careerpilot.controller;

import com.careerpilot.config.CorsConfig;
import com.careerpilot.config.CorsProperties;
import com.careerpilot.config.JwtDecoderConfig;
import com.careerpilot.config.JwtProperties;
import com.careerpilot.config.SecurityConfig;
import com.careerpilot.dto.*;
import com.careerpilot.security.RestAccessDeniedHandler;
import com.careerpilot.security.RestAuthenticationEntryPoint;
import com.careerpilot.service.CareerRoadmapService;
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
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CareerRoadmapController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtDecoderConfig.class
})
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
class CareerRoadmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CareerRoadmapService careerRoadmapService;

    private final UUID testUserId = UUID.randomUUID();
    private final UUID testRoadmapId = UUID.randomUUID();
    private final UUID testMilestoneId = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/roadmaps/generate without auth should return 401 Unauthorized")
    void testGenerateRoadmap_Unauthenticated() throws Exception {
        mockMvc.perform(post("/api/roadmaps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/roadmaps/generate with auth should return 201 Created and roadmap details")
    void testGenerateRoadmap_Authenticated() throws Exception {
        RoadmapDetailDto detailDto = new RoadmapDetailDto(
                testRoadmapId,
                testUserId,
                "Senior Backend Engineer",
                "Mid-Level",
                0,
                "NOT_STARTED",
                "Personalized career plan",
                null,
                List.of(),
                "test-model",
                Instant.now(),
                Instant.now()
        );

        when(careerRoadmapService.generateRoadmap(eq(testUserId), any())).thenReturn(detailDto);

        mockMvc.perform(post("/api/roadmaps/generate")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRole\":\"Senior Backend Engineer\",\"currentLevel\":\"Mid-Level\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(testRoadmapId.toString())))
                .andExpect(jsonPath("$.targetRole", is("Senior Backend Engineer")))
                .andExpect(jsonPath("$.overallProgress", is(0)));
    }

    @Test
    @DisplayName("GET /api/roadmaps should return list of roadmap summaries for the authenticated user")
    void testGetRoadmaps_Authenticated() throws Exception {
        RoadmapSummaryDto summary = new RoadmapSummaryDto(
                testRoadmapId,
                "Cloud Architect",
                "Senior",
                50,
                "IN_PROGRESS",
                "Cloud path",
                4,
                2,
                12,
                Instant.now(),
                Instant.now()
        );

        when(careerRoadmapService.getRoadmapsForUser(testUserId)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/roadmaps")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].targetRole", is("Cloud Architect")))
                .andExpect(jsonPath("$[0].overallProgress", is(50)));
    }

    @Test
    @DisplayName("GET /api/roadmaps/{id} should return roadmap detail")
    void testGetRoadmapById_Authenticated() throws Exception {
        RoadmapDetailDto detailDto = new RoadmapDetailDto(
                testRoadmapId,
                testUserId,
                "Cloud Architect",
                "Senior",
                50,
                "IN_PROGRESS",
                "Cloud path",
                null,
                List.of(),
                "test-model",
                Instant.now(),
                Instant.now()
        );

        when(careerRoadmapService.getRoadmapDetail(testRoadmapId, testUserId)).thenReturn(detailDto);

        mockMvc.perform(get("/api/roadmaps/" + testRoadmapId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testRoadmapId.toString())))
                .andExpect(jsonPath("$.targetRole", is("Cloud Architect")));
    }

    @Test
    @DisplayName("PATCH /api/roadmaps/{id}/milestones/{milestoneId} should update milestone and return roadmap")
    void testUpdateMilestoneStatus_Authenticated() throws Exception {
        RoadmapDetailDto detailDto = new RoadmapDetailDto(
                testRoadmapId,
                testUserId,
                "Cloud Architect",
                "Senior",
                100,
                "COMPLETED",
                "Cloud path",
                null,
                List.of(),
                "test-model",
                Instant.now(),
                Instant.now()
        );

        when(careerRoadmapService.updateMilestoneStatus(eq(testRoadmapId), eq(testMilestoneId), eq(testUserId), any()))
                .thenReturn(detailDto);

        UpdateMilestoneRequest updateReq = new UpdateMilestoneRequest("COMPLETED", "Completed capstone project");

        mockMvc.perform(patch("/api/roadmaps/" + testRoadmapId + "/milestones/" + testMilestoneId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallProgress", is(100)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    @DisplayName("DELETE /api/roadmaps/{id} should return 204 No Content")
    void testDeleteRoadmap_Authenticated() throws Exception {
        doNothing().when(careerRoadmapService).deleteRoadmap(testRoadmapId, testUserId);

        mockMvc.perform(delete("/api/roadmaps/" + testRoadmapId)
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isNoContent());
    }
}
