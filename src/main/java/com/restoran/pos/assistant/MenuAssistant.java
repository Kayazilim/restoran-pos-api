package com.restoran.pos.assistant;

import com.restoran.pos.dto.assistant.AssistantAnswerResponse;

/**
 * Menü ve alerjen sorularını yalnızca yüklenen belgelere dayanarak yanıtlayan asistan.
 * İki gerçekleştirimi vardır: {@link RagMenuAssistant} (@Primary, Gemini + RAG) ve
 * {@link KeywordMenuAssistant} (@Qualifier ile seçilen, yapay zeka kullanmayan güvenli yedek).
 */
public interface MenuAssistant {

    AssistantAnswerResponse answer(String question);
}
