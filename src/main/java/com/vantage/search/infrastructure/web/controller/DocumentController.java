package com.vantage.search.infrastructure.web.controller;

import com.vantage.search.application.port.in.SummariseUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document Intelligence", description = "AI-powered document analysis and summarisation")
public class DocumentController {

    /** Caps body size to bound LLM token spend per request and prevent DoS. */
    private static final int MAX_DOCUMENT_TEXT_LENGTH = 100_000;

    private final SummariseUseCase summariseUseCase;

    @Operation(
            summary = "Summarise document text",
            description = "Generates a concise three-point summary using AI-powered text analysis. "
                    + "Body must be non-blank and no longer than 100,000 characters."
    )
    @PostMapping("/summary")
    public String summariseDocument(@RequestBody String documentText) {
        if (documentText == null || documentText.isBlank()) {
            throw new IllegalArgumentException("Document text must not be empty.");
        }
        if (documentText.length() > MAX_DOCUMENT_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "Document text exceeds maximum length of " + MAX_DOCUMENT_TEXT_LENGTH + " characters.");
        }
        return summariseUseCase.summarise(documentText);
    }
}
