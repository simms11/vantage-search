package com.vantage.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Bounds the outbound HTTP calls Spring AI makes to Ollama. Without these,
 * a slow or hung LLM endpoint pins Tomcat request threads (synchronous
 * /api/documents/summary calls) and saturates the @Async executor that
 * processes document indexing events.
 */
@Configuration
public class AiClientConfig {

    @Value("${app.ai.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds;

    @Value("${app.ai.read-timeout-seconds:30}")
    private int readTimeoutSeconds;

    @Bean
    public RestClientCustomizer aiClientTimeoutCustomizer() {
        return builder -> builder.requestFactory(
                ClientHttpRequestFactories.get(
                        ClientHttpRequestFactorySettings.DEFAULTS
                                .withConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                                .withReadTimeout(Duration.ofSeconds(readTimeoutSeconds))));
    }
}
