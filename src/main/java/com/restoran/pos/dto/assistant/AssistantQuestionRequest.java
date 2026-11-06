package com.restoran.pos.dto.assistant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Personel asistanına soru")
public record AssistantQuestionRequest(
        @Schema(example = "Fıstıklı baklavada hangi alerjenler var?")
        @NotBlank(message = "Soru boş olamaz")
        @Size(max = 500, message = "Soru en fazla 500 karakter olabilir")
        String question) {
}
