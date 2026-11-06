package com.restoran.pos.dto.knowledge;

import com.restoran.pos.entity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "PATCH ile belgenin kısmi güncellenmesi")
public record KnowledgeDocumentPatchRequest(
        @Schema(description = "Birincil anahtar değiştirilemez; farklı bir id gönderilirse 400 döner.")
        Long id,

        @Pattern(regexp = "(?s).*\\S.*", message = "Başlık boş olamaz")
        @Size(max = 150, message = "Başlık en fazla 150 karakter olabilir")
        String title,

        DocumentType documentType,

        @Pattern(regexp = "(?s).*\\S.*", message = "İçerik boş olamaz")
        @Size(max = 50000, message = "İçerik en fazla 50000 karakter olabilir")
        String content) {
}
