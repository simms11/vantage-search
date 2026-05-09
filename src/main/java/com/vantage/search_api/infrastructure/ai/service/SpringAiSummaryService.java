package com.vantage.search_api.infrastructure.ai.service;

import com.vantage.search_api.application.port.out.AiSummaryPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;


@Service
public class SpringAiSummaryService implements AiSummaryPort {

    private final ChatClient chatClient;

    public SpringAiSummaryService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String generateBulletSummary(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return chatClient.prompt()
                .user("Summarise the following document in 3 concise bullet points:\n\n" + text)
                .call()
                .content();
    }
}