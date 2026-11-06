package com.restoran.pos.dto.assistant;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Asistan cevabı. 'grounded' false ise belgelerde dayanak bulunamamıştır ve cevap uydurulmamıştır.")
public record AssistantAnswerResponse(
        String question,
        String answer,
        @Schema(description = "Cevap yüklenen belgelere dayanıyorsa true") boolean grounded,
        @Schema(description = "Cevabın dayandığı belge başlıkları") List<String> sources,
        @Schema(description = "Cevabı üreten mod", example = "RAG_GEMINI") String mode,
        String warning) {

    public static final String WARNING =
            "Bu cevap yalnızca yüklenen menü ve alerjen belgelerine dayanır. Ciddi alerjisi olan misafirler için "
                    + "lütfen mutfak sorumlusuyla teyit edin.";

    public static final String NOT_FOUND_ANSWER =
            "Bu soruya yüklenen menü ve alerjen belgelerinde bir dayanak bulamadım; tahmin yürütmüyorum. "
                    + "Lütfen mutfak sorumlusuna danışın.";

    public static AssistantAnswerResponse grounded(String question, String answer, List<String> sources, String mode) {
        return new AssistantAnswerResponse(question, answer, true, sources, mode, WARNING);
    }

    public static AssistantAnswerResponse notFound(String question, String mode) {
        return new AssistantAnswerResponse(question, NOT_FOUND_ANSWER, false, List.of(), mode, WARNING);
    }
}
