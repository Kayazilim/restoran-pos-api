package com.restoran.pos.assistant;

/**
 * Bir bilgi belgesi eklendiğinde, değiştiğinde veya silindiğinde yayımlanır.
 */
public record KnowledgeBaseChangedEvent(Long documentId) {
}
