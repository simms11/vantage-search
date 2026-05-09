package com.vantage.search.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Document(
        UUID id,
        UUID clientId,
        String title,
        String summary,
        OffsetDateTime createdAt
) {}
