package com.vantage.search.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        // 32 bytes is the minimum HMAC-SHA256 key length; Base64 of 32 bytes is
        // 44 characters (with padding). A shorter value would otherwise surface
        // as io.jsonwebtoken.security.WeakKeyException at JwtService
        // construction; this constraint fails fast at bind time instead.
        @NotBlank @Size(min = 44) String secret,
        @Positive long expirationMs
) {}
