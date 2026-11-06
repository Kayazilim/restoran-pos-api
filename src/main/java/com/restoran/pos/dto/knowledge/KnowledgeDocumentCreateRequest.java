package com.restoran.pos.dto.knowledge;

import com.restoran.pos.entity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Asistanın referans alacağı yeni menü/alerjen belgesi")
public record KnowledgeDocumentCreateRequest(
        @Schema(example = "Mevsimlik Menü")
        @NotBlank(message = "Başlık boş olamaz")
        @Size(max = 150, message = "Başlık en fazla 150 karakter olabilir")
        String title,

        @Schema(example = "MENU")
        @NotNull(message = "Belge türü zorunludur")
        DocumentType documentType,

        @Schema(example = "Balkabağı Çorbası: balkabağı, krema, tereyağı. Alerjenler: süt.")
        @NotBlank(message = "İçerik boş olamaz")
        @Size(max = 50000, message = "İçerik en fazla 50000 karakter olabilir")
        String content) {
}
