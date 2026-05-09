package com.vantage.search_api.infrastructure.web.controller;

import com.vantage.search_api.application.port.in.SummariseUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document Intelligence", description = "AI-powered document analysis and summarisation")
public class DocumentController {

    private final SummariseUseCase summariseUseCase;

    @Operation(
            summary = "Summarise document text",
            description = "Generates a concise three-point summary using AI-powered text analysis."
    )
    @PostMapping("/summary")
    public String summariseDocument(@RequestBody String documentText) {
        return summariseUseCase.summarise(documentText);
    }
}
