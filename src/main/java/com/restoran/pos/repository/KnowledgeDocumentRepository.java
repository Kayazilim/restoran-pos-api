package com.restoran.pos.repository;

import com.restoran.pos.entity.DocumentType;
import com.restoran.pos.entity.KnowledgeDocument;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepository {

    KnowledgeDocument save(KnowledgeDocument document);

    Optional<KnowledgeDocument> findById(Long id);

    List<KnowledgeDocument> findAll();

    List<KnowledgeDocument> findByType(DocumentType documentType);

    KnowledgeDocument update(KnowledgeDocument document);

    void delete(KnowledgeDocument document);

    boolean existsByTitle(String title, Long excludeId);

    long count();
}
