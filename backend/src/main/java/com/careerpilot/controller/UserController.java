package com.careerpilot.controller;

import com.careerpilot.dto.UserMeResponse;
import com.careerpilot.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        String email = jwt.getClaimAsString("email");
        String role = jwt.getClaimAsString("role");
        if (role == null) {
            role = "authenticated";
        }

        Instant authTime = jwt.getIssuedAt() != null ? jwt.getIssuedAt() : Instant.now();
        return ResponseEntity.ok(new UserMeResponse(userId, email, role, authTime));
    }
}
