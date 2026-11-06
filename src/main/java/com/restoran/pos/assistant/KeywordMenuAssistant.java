package com.restoran.pos.assistant;

import com.restoran.pos.dto.assistant.AssistantAnswerResponse;
import com.restoran.pos.entity.KnowledgeDocument;
import com.restoran.pos.repository.KnowledgeDocumentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Yapay zeka kullanmayan güvenli mod: soru kelimeleriyle en iyi eşleşen belge bölümlerini değiştirmeden
 * (birebir alıntı) döndürür. Gemini anahtarı tanımlı değilse veya Gemini'ye ulaşılamazsa devreye girer;
 * içerik üretmediği için yanlış alerjen bilgisi uydurması mümkün değildir.
 */
@Component(KeywordMenuAssistant.BEAN_NAME)
public class KeywordMenuAssistant implements MenuAssistant {

    public static final String BEAN_NAME = "keywordMenuAssistant";
    public static final String MODE = "KEYWORD_SEARCH";
    private static final int MAX_PASSAGES = 3;

    private final KnowledgeDocumentRepository documentRepository;

    public KeywordMenuAssistant(KnowledgeDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantAnswerResponse answer(String question) {
        Set<String> stems = TurkishText.keywordStems(question);
        if (stems.isEmpty()) {
            return AssistantAnswerResponse.notFound(question, MODE);
        }

        List<ScoredPassage> scored = new ArrayList<>();
        for (KnowledgeDocument document : documentRepository.findAll()) {
            for (String passage : KnowledgeChunker.blocks(document.getContent())) {
                String folded = TurkishText.fold(passage);
                long score = stems.stream().filter(folded::contains).count();
                if (score > 0) {
                    scored.add(new ScoredPassage(document.getTitle(), passage, score));
                }
            }
        }
        if (scored.isEmpty()) {
            return AssistantAnswerResponse.notFound(question, MODE);
        }

        // Yalnızca en yüksek skora sahip bölümler alınır; zayıf eşleşmeler cevaba karışmaz.
        long bestScore = scored.stream().mapToLong(ScoredPassage::score).max().orElse(0);
        List<ScoredPassage> best = scored.stream()
                .filter(passage -> passage.score() == bestScore)
                .sorted(Comparator.comparing(ScoredPassage::title))
                .limit(MAX_PASSAGES)
                .toList();

        String answer = "Yüklenen belgelerde bulunan ilgili bölümler (yapay zeka kullanılmadan, birebir alıntı):\n\n"
                + best.stream().map(passage -> "[" + passage.title() + "] " + passage.text())
                .collect(Collectors.joining("\n\n"));
        List<String> sources = best.stream().map(ScoredPassage::title).distinct().toList();
        return AssistantAnswerResponse.grounded(question, answer, sources, MODE);
    }

    private record ScoredPassage(String title, String text, long score) {
    }
}
