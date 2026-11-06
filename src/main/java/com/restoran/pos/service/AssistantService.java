package com.restoran.pos.service;

import com.restoran.pos.dto.assistant.AssistantAnswerResponse;
import com.restoran.pos.dto.assistant.AssistantQuestionRequest;

public interface AssistantService {

    AssistantAnswerResponse ask(AssistantQuestionRequest request);
}
