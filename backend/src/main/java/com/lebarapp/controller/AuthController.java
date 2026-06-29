package com.lebarapp.controller;

import com.lebarapp.dto.AuthUserDto;
import com.lebarapp.dto.LoginRequest;
import com.lebarapp.dto.LoginResponse;
import com.lebarapp.service.AuthService;
import com.lebarapp.security.SecurityUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Barmaker authentication endpoints: stateless login (issues a signed JWT) and
 * current-user retrieval. The entity is never exposed; only explicit DTOs cross
 * the boundary.
 *
 * <p>{@code GET /api/auth/me} reads the {@link SecurityUserPrincipal} that was
 * built from the live database state by {@code ActiveUserJwtAuthenticationConverter}.
 * No additional database lookup is needed — the converter already verified that
 * the user exists, is active, and has the expected role.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthUserDto> me(@AuthenticationPrincipal SecurityUserPrincipal principal) {
        return ResponseEntity.ok(toDto(principal));
    }

    private static AuthUserDto toDto(SecurityUserPrincipal principal) {
        return new AuthUserDto(principal.getUserId(), principal.getUsername(),
                principal.getDisplayName(), principal.getRole());
    }
}
