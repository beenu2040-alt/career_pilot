package com.careerpilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "careerpilot.supabase")
public record JwtProperties(
    String jwtSecret,
    String jwkSetUri,
    String issuerUri
) {
}
