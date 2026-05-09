package com.vantage.search.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(
        @Positive long capacity,
        @Positive long refillDurationSeconds,
        boolean trustProxy
) {}
