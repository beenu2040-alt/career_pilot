package com.careerpilot.controller;

import com.careerpilot.config.CorsConfig;
import com.careerpilot.config.CorsProperties;
import com.careerpilot.config.JwtDecoderConfig;
import com.careerpilot.config.JwtProperties;
import com.careerpilot.config.SecurityConfig;
import com.careerpilot.dto.ProfileResponse;
import com.careerpilot.dto.ProfileUpdateRequest;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.security.RestAccessDeniedHandler;
import com.careerpilot.security.RestAuthenticationEntryPoint;
import com.careerpilot.service.ProfileService;
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
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfileController.class)
@Import({
    SecurityConfig.class,
    CorsConfig.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    JwtDecoderConfig.class
})
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /api/profile without JWT returns 401 Unauthorized")
    void shouldRejectUnauthenticatedProfileGet() throws Exception {
        mockMvc.perform(get("/api/profile")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("GET /api/profile with valid JWT returns profile when found")
    void shouldReturnProfileWhenExists() throws Exception {
        UUID userId = UUID.randomUUID();
        ProfileResponse mockResponse = new ProfileResponse(
                userId,
                "Alex Rivera",
                "Senior Backend Engineer",
                "San Francisco, CA",
                "Principal Architect",
                Instant.now(),
                Instant.now()
        );

        when(profileService.getProfile(eq(userId))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/profile")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.fullName", is("Alex Rivera")))
                .andExpect(jsonPath("$.targetRole", is("Principal Architect")));
    }

    @Test
    @DisplayName("GET /api/profile with valid JWT returns 404 when profile not yet created")
    void shouldReturn404WhenProfileDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        when(profileService.getProfile(eq(userId)))
                .thenThrow(new ResourceNotFoundException("Profile not found for user: " + userId));

        mockMvc.perform(get("/api/profile")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("PUT /api/profile with valid JWT updates or creates profile")
    void shouldUpsertProfileSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "Alex Rivera",
                "Senior Full-Stack Engineer",
                "Seattle, WA",
                "Lead Architect"
        );

        ProfileResponse updatedResponse = new ProfileResponse(
                userId,
                request.fullName(),
                request.headline(),
                request.location(),
                request.targetRole(),
                Instant.now(),
                Instant.now()
        );

        when(profileService.upsertProfile(eq(userId), any(ProfileUpdateRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/profile")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.fullName", is("Alex Rivera")))
                .andExpect(jsonPath("$.headline", is("Senior Full-Stack Engineer")));
    }

    @Test
    @DisplayName("PUT /api/profile rejects request body with fields exceeding maximum length")
    void shouldRejectInvalidProfileUpdateRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        String overlongName = "a".repeat(256);
        ProfileUpdateRequest invalidRequest = new ProfileUpdateRequest(
                overlongName,
                "Valid Headline",
                "Valid Location",
                "Valid Target"
        );

        mockMvc.perform(put("/api/profile")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }
}
