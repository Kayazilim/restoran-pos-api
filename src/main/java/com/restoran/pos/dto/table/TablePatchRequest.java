package com.restoran.pos.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "PATCH ile kısmi güncelleme: yalnızca gönderilen alanlar değişir.")
public record TablePatchRequest(
        @Schema(description = "Birincil anahtar değiştirilemez; farklı bir id gönderilirse 400 döner.")
        Long id,

        @Min(value = 1, message = "Masa numarası en az 1 olmalıdır")
        @Max(value = 9999, message = "Masa numarası en fazla 9999 olabilir")
        Integer tableNumber,

        @Min(value = 1, message = "Kapasite en az 1 olmalıdır")
        @Max(value = 50, message = "Kapasite en fazla 50 olabilir")
        Integer capacity,

        @Size(max = 50, message = "Konum en fazla 50 karakter olabilir")
        String location,

        @Schema(description = "Masaya atanacak garson", example = "3")
        Long waiterId,

        Boolean active) {
}
