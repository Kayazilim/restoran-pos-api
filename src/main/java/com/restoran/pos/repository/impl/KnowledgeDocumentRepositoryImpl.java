package com.restoran.pos.repository.impl;

import com.restoran.pos.entity.DocumentType;
import com.restoran.pos.entity.KnowledgeDocument;
import com.restoran.pos.repository.KnowledgeDocumentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class KnowledgeDocumentRepositoryImpl implements KnowledgeDocumentRepository {

    private final EntityManager entityManager;

    public KnowledgeDocumentRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        entityManager.persist(document);
        return document;
    }

    @Override
    public Optional<KnowledgeDocument> findById(Long id) {
        return Optional.ofNullable(entityManager.find(KnowledgeDocument.class, id));
    }

    @Override
    public List<KnowledgeDocument> findAll() {
        return entityManager.createQuery("SELECT d FROM KnowledgeDocument d ORDER BY d.id", KnowledgeDocument.class)
                .getResultList();
    }

    @Override
    public List<KnowledgeDocument> findByType(DocumentType documentType) {
        return entityManager.createQuery(
                        "SELECT d FROM KnowledgeDocument d WHERE d.documentType = :documentType ORDER BY d.id",
                        KnowledgeDocument.class)
                .setParameter("documentType", documentType)
                .getResultList();
    }

    @Override
    public KnowledgeDocument update(KnowledgeDocument document) {
        KnowledgeDocument merged = entityManager.merge(document);
        entityManager.flush();
        return merged;
    }

    @Override
    public void delete(KnowledgeDocument document) {
        entityManager.remove(entityManager.contains(document) ? document : entityManager.merge(document));
        entityManager.flush();
    }

    @Override
    public boolean existsByTitle(String title, Long excludeId) {
        String jpql = "SELECT COUNT(d) FROM KnowledgeDocument d WHERE LOWER(d.title) = LOWER(:title)"
                + (excludeId != null ? " AND d.id <> :excludeId" : "");
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("title", title);
        if (excludeId != null) {
            query.setParameter("excludeId", excludeId);
        }
        return query.getSingleResult() > 0;
    }

    @Override
    public long count() {
        return entityManager.createQuery("SELECT COUNT(d) FROM KnowledgeDocument d", Long.class).getSingleResult();
    }
}
