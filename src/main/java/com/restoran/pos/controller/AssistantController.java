package com.restoran.pos.controller;

import com.restoran.pos.dto.assistant.AssistantAnswerResponse;
import com.restoran.pos.dto.assistant.AssistantQuestionRequest;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.service.AssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@Tag(name = "Personel Asistanı", description = "Yalnızca yüklenen menü ve alerjen belgelerine dayanan RAG asistanı (ASSISTANT_USE)")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/questions")
    @PreAuthorize("hasAuthority('" + Permissions.ASSISTANT_USE + "')")
    @Operation(summary = "Asistana soru sor",
            description = "Cevap yalnızca yüklenen belgelerden üretilir; belgelerde dayanak yoksa 'grounded=false' ile "
                    + "tahmin yürütülmediği bildirilir. GEMINI_API_KEY tanımlı değilse belgelerden birebir alıntı yapılır.")
    public AssistantAnswerResponse ask(@Valid @RequestBody AssistantQuestionRequest request) {
        return assistantService.ask(request);
    }
}
