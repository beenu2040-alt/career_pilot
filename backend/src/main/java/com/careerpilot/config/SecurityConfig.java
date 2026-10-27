package com.careerpilot.config;

import com.careerpilot.security.RestAccessDeniedHandler;
import com.careerpilot.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            CorsConfigurationSource corsConfigurationSource,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS using the configured CorsConfigurationSource bean
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Disable CSRF for stateless REST APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session policy: no HTTP sessions created
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Exception handling with JSON error responses
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // OAuth2 Resource Server for validating Supabase JWT access tokens
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow CORS pre-flight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public health probe endpoint
                .requestMatchers("/api/health").permitAll()

                // Public skills catalog
                .requestMatchers(HttpMethod.GET, "/api/skills").permitAll()

                // User-specific saved jobs & recommendations require authentication
                .requestMatchers(HttpMethod.GET, "/api/jobs/saved").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/jobs/recommendations").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/jobs/*/match").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/jobs/*/save").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/jobs/*/save").authenticated()

                // Public job search and job details (with optional user context if authenticated)
                .requestMatchers(HttpMethod.GET, "/api/jobs").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/jobs/*").permitAll()

                // Profile and user skills require authentication
                .requestMatchers("/api/profile/**").authenticated()

                // All other API endpoints require verified Supabase JWT authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
