package com.vantage.search.application.service;

import com.vantage.search.application.port.in.SummariseUseCase;
import com.vantage.search.application.port.out.AiSummaryPort;
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
