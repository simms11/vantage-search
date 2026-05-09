package com.vantage.search.infrastructure.web.controller;

import com.vantage.search.application.dto.request.AuthRequest;
import com.vantage.search.application.dto.response.AuthResponse;
import com.vantage.search.infrastructure.security.JwtService;
import com.vantage.search.infrastructure.security.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Obtain and revoke JWTs for accessing protected endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Operation(summary = "Login", description = "Returns a JWT Bearer token. Include it as: Authorization: Bearer <token>")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String token = jwtService.generateToken(authentication.getName());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @Operation(summary = "Logout", description = "Immediately revokes the current JWT token. Idempotent.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);

        // Parsing failures (malformed / expired token) are absorbed: logout is
        // idempotent. Revoke failures (e.g. Redis down) are not absorbed; they
        // propagate to the global handler and surface as 503 so the client
        // knows the token may not actually be revoked.
        String jti;
        java.time.OffsetDateTime expiration;
        try {
            jti = jwtService.extractJti(token);
            expiration = jwtService.extractExpiration(token);
        } catch (RuntimeException ex) {
            log.debug("Logout suppressed token-parse exception: {}", ex.getClass().getSimpleName());
            return;
        }
        tokenBlacklistService.revoke(jti, expiration);
    }
}
