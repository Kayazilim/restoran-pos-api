package com.restoran.pos.service;

import com.restoran.pos.dto.knowledge.KnowledgeDocumentCreateRequest;
import com.restoran.pos.dto.knowledge.KnowledgeDocumentPatchRequest;
import com.restoran.pos.dto.knowledge.KnowledgeDocumentResponse;
import com.restoran.pos.dto.knowledge.KnowledgeDocumentUpdateRequest;
import com.restoran.pos.entity.DocumentType;

import java.util.List;

public interface KnowledgeDocumentService {

    List<KnowledgeDocumentResponse> findAll(DocumentType type);

    KnowledgeDocumentResponse findById(Long id);

    KnowledgeDocumentResponse create(KnowledgeDocumentCreateRequest request);

    KnowledgeDocumentResponse update(Long id, KnowledgeDocumentUpdateRequest request);

    KnowledgeDocumentResponse patch(Long id, KnowledgeDocumentPatchRequest request);

    void delete(Long id);
}
