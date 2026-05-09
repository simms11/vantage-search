package com.vantage.search_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(long capacity, long refillDurationSeconds, boolean trustProxy) {}
