package com.careerpilot.controller;

import com.careerpilot.config.CorsConfig;
import com.careerpilot.config.CorsProperties;
import com.careerpilot.config.JwtDecoderConfig;
import com.careerpilot.config.JwtProperties;
import com.careerpilot.config.SecurityConfig;
import com.careerpilot.dto.CareerAnalyticsDto;
import com.careerpilot.security.RestAccessDeniedHandler;
import com.careerpilot.security.RestAuthenticationEntryPoint;
import com.careerpilot.service.CareerAnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CareerAnalyticsController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtDecoderConfig.class
})
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
class CareerAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CareerAnalyticsService analyticsService;

    private final UUID testUserId = UUID.randomUUID();

    @Test
    @DisplayName("GET /api/analytics/applications unauthenticated should return 401")
    void testGetAnalytics_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/analytics/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/analytics/applications authenticated should return analytics payload")
    void testGetAnalytics_Authenticated() throws Exception {
        CareerAnalyticsDto dto = new CareerAnalyticsDto(
                10,
                8,
                2,
                4,
                4,
                50.0,
                1,
                12.5,
                Map.of("APPLIED", 4, "INTERVIEW", 3, "OFFER", 1, "WISHLIST", 2),
                List.of()
        );

        when(analyticsService.getAnalytics(testUserId)).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/applications")
                        .with(jwt().jwt(j -> j.subject(testUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications", is(10)))
                .andExpect(jsonPath("$.submittedApplications", is(8)))
                .andExpect(jsonPath("$.interviewCount", is(4)))
                .andExpect(jsonPath("$.interviewRate", is(50.0)))
                .andExpect(jsonPath("$.offerCount", is(1)))
                .andExpect(jsonPath("$.offerRate", is(12.5)));
    }
}
