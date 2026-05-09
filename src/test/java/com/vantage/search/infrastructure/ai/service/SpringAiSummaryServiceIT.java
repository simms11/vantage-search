package com.vantage.search.infrastructure.ai.service;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.vantage.search.support.BaseIT;
import com.vantage.search.application.port.out.AiSearchPort;
import com.vantage.search.application.port.out.ClientPersistencePort;
import com.vantage.search.infrastructure.ai.service.SpringAiSummaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpringAiSummaryServiceIT extends BaseIT {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .port(8089)
                    .http2PlainDisabled(true))
            .build();

    @MockBean
    private ClientPersistencePort clientPersistencePort;

    @MockBean
    private AiSearchPort aiSearchPort;

    @Autowired
    private SpringAiSummaryService summaryService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", () -> "http://localhost:8089");
    }

    @Test
    void shouldReturnMockedSummaryWithoutHittingRealLLM() {
        String mockJsonResponse = "{\"model\":\"llama3\",\"message\":{\"role\":\"assistant\",\"content\":\"Fake Point 1 \\n Fake Point 2 \\n Fake Point 3\"},\"done\":true}";

        wireMock.stubFor(post(urlEqualTo("/api/chat"))
                .willReturn(okJson(mockJsonResponse)));

        String actualSummary = summaryService.generateBulletSummary("Please summarise this document.");

        assertThat(actualSummary).contains("Fake Point 1");
    }

    @Test
    void shouldThrowExceptionWhenAiServiceIsDown() {
        wireMock.stubFor(post(urlEqualTo("/api/chat"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        assertThrows(Exception.class, () -> {
            summaryService.generateBulletSummary("Summarize this document.");
        });
    }

    @Test
    void shouldHandleExtremeLatencyGracefully() {
        wireMock.stubFor(post(urlEqualTo("/api/chat"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(10_000)
                        .withBody("{\"message\": {\"content\": \"Too slow\"}}")));

        // Read timeout in tests is 2 seconds (see application-test.properties);
        // the WireMock 10-second delay must trip it.
        assertThrows(Exception.class, () -> {
            summaryService.generateBulletSummary("Summarize this document.");
        });
    }
}