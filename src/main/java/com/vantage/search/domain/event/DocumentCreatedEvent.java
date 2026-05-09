package com.vantage.search.domain.event;

import java.util.UUID;

public record DocumentCreatedEvent(UUID documentId, UUID clientId, String title, String content) {
}
