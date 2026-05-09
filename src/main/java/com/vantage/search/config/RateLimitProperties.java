package com.vantage.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(long capacity, long refillDurationSeconds, boolean trustProxy) {}
