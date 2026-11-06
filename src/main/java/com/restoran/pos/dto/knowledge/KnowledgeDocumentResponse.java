package com.restoran.pos.dto.knowledge;

import com.restoran.pos.entity.DocumentType;
import com.restoran.pos.entity.KnowledgeDocument;

import java.time.Instant;

public record KnowledgeDocumentResponse(
        Long id,
        String title,
        DocumentType documentType,
        String documentTypeLabel,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static KnowledgeDocumentResponse from(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(document.getId(), document.getTitle(), document.getDocumentType(),
                document.getDocumentType().getLabel(), document.getContent(),
                document.getCreatedAt(), document.getUpdatedAt());
    }
}
