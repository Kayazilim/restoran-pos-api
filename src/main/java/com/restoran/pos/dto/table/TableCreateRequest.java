package com.restoran.pos.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Yeni masa isteği. Masa için otomatik olarak QR müşteri hesabı da oluşturulur.")
public record TableCreateRequest(
        @Schema(description = "Masa numarası (benzersiz)", example = "7")
        @NotNull(message = "Masa numarası zorunludur")
        @Min(value = 1, message = "Masa numarası en az 1 olmalıdır")
        @Max(value = 9999, message = "Masa numarası en fazla 9999 olabilir")
        Integer tableNumber,

        @Schema(description = "Kişi kapasitesi", example = "4")
        @NotNull(message = "Kapasite zorunludur")
        @Min(value = 1, message = "Kapasite en az 1 olmalıdır")
        @Max(value = 50, message = "Kapasite en fazla 50 olabilir")
        Integer capacity,

        @Schema(description = "Konum", example = "Bahçe")
        @Size(max = 50, message = "Konum en fazla 50 karakter olabilir")
        String location,

        @Schema(description = "Masaya atanacak garsonun kullanıcı kimliği", example = "2")
        Long waiterId,

        @Schema(description = "Masa kullanımda mı? (gönderilmezse true)", example = "true")
        Boolean active) {
}
