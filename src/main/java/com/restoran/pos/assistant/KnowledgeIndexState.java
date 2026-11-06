package com.restoran.pos.assistant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Bilgi tabanının sürüm sayacı. Belge değişikliği commit edildiğinde artar; RAG asistanı sürüm değiştiyse
 * vektör indeksini yeniden oluşturur. Böylece asistan her zaman güncel belgelerle cevap verir.
 */
@Component
public class KnowledgeIndexState {

    private final AtomicLong version = new AtomicLong();

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onKnowledgeBaseChanged(KnowledgeBaseChangedEvent event) {
        version.incrementAndGet();
    }

    public long currentVersion() {
        return version.get();
    }
}
