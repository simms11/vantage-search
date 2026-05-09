package com.vantage.search_api.infrastructure.web.controller;

import com.vantage.search_api.application.port.in.SearchUseCase;
import com.vantage.search_api.domain.model.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Unified Search", description = "Hybrid search engine combining lexical and semantic results")
public class SearchController {

    private final SearchUseCase searchUseCase;

    @Operation(
            summary = "Perform hybrid search",
            description = "Executes a unified search across clients and documents, merging PostgreSQL lexical results with AI semantic matches via RRF."
    )
    @GetMapping("/search")
    public List<SearchResult> search(
            @Parameter(description = "The search query string", example = "Vantage Wealth", required = true)
            @RequestParam("query") @NotBlank @Size(max = 500) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return searchUseCase.search(query, page, size);
    }
}
