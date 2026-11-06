package com.restoran.pos.service.impl;

import com.restoran.pos.assistant.MenuAssistant;
import com.restoran.pos.dto.assistant.AssistantAnswerResponse;
import com.restoran.pos.dto.assistant.AssistantQuestionRequest;
import com.restoran.pos.service.AssistantService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AssistantServiceImpl implements AssistantService {

    private final MenuAssistant menuAssistant;

    /**
     * MenuAssistant arayüzünün iki gerçekleştirimi vardır; @Primary olan RAG asistanı enjekte edilir.
     * {@code @Lazy} sayesinde buraya bir vekil (proxy) verilir ve ağır RAG bileşeni ilk soru sorulana kadar oluşturulmaz.
     */
    public AssistantServiceImpl(@Lazy MenuAssistant menuAssistant) {
        this.menuAssistant = menuAssistant;
    }

    @Override
    public AssistantAnswerResponse ask(AssistantQuestionRequest request) {
        return menuAssistant.answer(request.question().strip());
    }
}
