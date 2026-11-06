package com.restoran.pos.service.impl;

import com.restoran.pos.assistant.KnowledgeBaseChangedEvent;
import com.restoran.pos.dto.knowledge.KnowledgeDocumentCreateRequest;
import com.restoran.pos.dto.knowledge.KnowledgeDocumentPatchRequest;
import com.restoran.pos.dto.knowledge.KnowledgeDocumentResponse;
import com.restoran.pos.dto.knowledge.KnowledgeDocumentUpdateRequest;
import com.restoran.pos.entity.DocumentType;
import com.restoran.pos.entity.KnowledgeDocument;
import com.restoran.pos.exception.ConflictException;
import com.restoran.pos.exception.ResourceNotFoundException;
import com.restoran.pos.repository.KnowledgeDocumentRepository;
import com.restoran.pos.service.KnowledgeDocumentService;
import com.restoran.pos.util.PatchGuard;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Asistanın tek bilgi kaynağı olan menü/alerjen belgeleri. Her değişiklikten sonra vektör indeksi yenilenir.
 */
@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public KnowledgeDocumentServiceImpl(KnowledgeDocumentRepository documentRepository,
                                        ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> findAll(DocumentType type) {
        List<KnowledgeDocument> documents = type != null ? documentRepository.findByType(type) : documentRepository.findAll();
        return documents.stream().map(KnowledgeDocumentResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeDocumentResponse findById(Long id) {
        return KnowledgeDocumentResponse.from(getDocument(id));
    }

    @Override
    @Transactional
    public KnowledgeDocumentResponse create(KnowledgeDocumentCreateRequest request) {
        String title = request.title().trim();
        ensureTitleIsUnique(title, null);
        KnowledgeDocument document = documentRepository.save(
                new KnowledgeDocument(title, request.documentType(), request.content().strip()));
        eventPublisher.publishEvent(new KnowledgeBaseChangedEvent(document.getId()));
        return KnowledgeDocumentResponse.from(document);
    }

    @Override
    @Transactional
    public KnowledgeDocumentResponse update(Long id, KnowledgeDocumentUpdateRequest request) {
        KnowledgeDocument document = getDocument(id);
        String title = request.title().trim();
        ensureTitleIsUnique(title, id);
        document.setTitle(title);
        document.setDocumentType(request.documentType());
        document.setContent(request.content().strip());
        KnowledgeDocument updated = documentRepository.update(document);
        eventPublisher.publishEvent(new KnowledgeBaseChangedEvent(id));
        return KnowledgeDocumentResponse.from(updated);
    }

    @Override
    @Transactional
    public KnowledgeDocumentResponse patch(Long id, KnowledgeDocumentPatchRequest request) {
        PatchGuard.ensureIdUnchanged(id, request.id());
        KnowledgeDocument document = getDocument(id);
        if (request.title() != null) {
            String title = request.title().trim();
            ensureTitleIsUnique(title, id);
            document.setTitle(title);
        }
        if (request.documentType() != null) {
            document.setDocumentType(request.documentType());
        }
        if (request.content() != null) {
            document.setContent(request.content().strip());
        }
        KnowledgeDocument updated = documentRepository.update(document);
        eventPublisher.publishEvent(new KnowledgeBaseChangedEvent(id));
        return KnowledgeDocumentResponse.from(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        documentRepository.delete(getDocument(id));
        eventPublisher.publishEvent(new KnowledgeBaseChangedEvent(id));
    }

    private void ensureTitleIsUnique(String title, Long excludeId) {
        if (documentRepository.existsByTitle(title, excludeId)) {
            throw new ConflictException("Bu başlıkta bir belge zaten var: " + title);
        }
    }

    private KnowledgeDocument getDocument(Long id) {
        return documentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Belge", id));
    }
}
