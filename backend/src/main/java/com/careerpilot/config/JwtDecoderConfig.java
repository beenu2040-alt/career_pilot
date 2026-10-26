package com.careerpilot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtDecoderConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtDecoderConfig.class);

    // Fallback development secret (32+ bytes for HMAC-SHA256) used only when no environment variable is provided
    private static final String DEV_FALLBACK_SECRET = "dev-insecure-secret-key-careerpilot-supabase-32chars!";

    private final JwtProperties jwtProperties;

    public JwtDecoderConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder;

        if (jwtProperties.jwkSetUri() != null && !jwtProperties.jwkSetUri().isBlank()) {
            log.info("Configuring Supabase JwtDecoder using JWKS URI: {}", jwtProperties.jwkSetUri());
            decoder = NimbusJwtDecoder.withJwkSetUri(jwtProperties.jwkSetUri()).build();
        } else if (jwtProperties.jwtSecret() != null && !jwtProperties.jwtSecret().isBlank()) {
            log.info("Configuring Supabase JwtDecoder using configured HMAC-SHA256 secret");
            byte[] secretBytes = jwtProperties.jwtSecret().getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
            decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
        } else if (jwtProperties.issuerUri() != null && !jwtProperties.issuerUri().isBlank()) {
            log.info("Configuring Supabase JwtDecoder using Issuer URI: {}", jwtProperties.issuerUri());
            decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(jwtProperties.issuerUri());
        } else {
            log.warn("No Supabase JWT secret or JWKS URI configured. Using development fallback secret. Set SUPABASE_JWT_SECRET or SUPABASE_JWT_JWK_SET_URI for production.");
            byte[] secretBytes = DEV_FALLBACK_SECRET.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
            decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
        }

        // Configure standard token validators
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());

        if (jwtProperties.issuerUri() != null && !jwtProperties.issuerUri().isBlank()) {
            validators.add(new JwtIssuerValidator(jwtProperties.issuerUri()));
        }

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }
}
