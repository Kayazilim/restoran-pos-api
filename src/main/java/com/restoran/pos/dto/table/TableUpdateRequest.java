package com.restoran.pos.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "PUT ile tam güncelleme. Gönderilmeyen konum ve garson alanları boşaltılır.")
public record TableUpdateRequest(
        @Schema(example = "7")
        @NotNull(message = "Masa numarası zorunludur")
        @Min(value = 1, message = "Masa numarası en az 1 olmalıdır")
        @Max(value = 9999, message = "Masa numarası en fazla 9999 olabilir")
        Integer tableNumber,

        @Schema(example = "6")
        @NotNull(message = "Kapasite zorunludur")
        @Min(value = 1, message = "Kapasite en az 1 olmalıdır")
        @Max(value = 50, message = "Kapasite en fazla 50 olabilir")
        Integer capacity,

        @Schema(example = "Teras")
        @Size(max = 50, message = "Konum en fazla 50 karakter olabilir")
        String location,

        @Schema(description = "Atanacak garson; boş bırakılırsa masanın garson ataması kaldırılır", example = "2")
        Long waiterId,

        @Schema(example = "true")
        @NotNull(message = "active alanı zorunludur")
        Boolean active) {
}
