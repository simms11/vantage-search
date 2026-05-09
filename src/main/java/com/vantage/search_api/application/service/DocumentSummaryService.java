package com.vantage.search_api.application.service;

import com.vantage.search_api.application.port.in.SummariseUseCase;
import com.vantage.search_api.application.port.out.AiSummaryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentSummaryService implements SummariseUseCase {

    private final AiSummaryPort aiSummaryPort;

    @Override
    public String summarise(String text) {
        return aiSummaryPort.generateBulletSummary(text);
    }
}
