package com.restoran.pos.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Tüm hata yanıtlarında kullanılan ortak JSON hata modeli.
 */
@Schema(description = "Ortak hata modeli: durum kodu, açıklayıcı mesaj ve zaman bilgisi")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        @Schema(description = "Hatanın oluştuğu an (UTC)", example = "2026-11-06T10:15:30Z")
        Instant timestamp,

        @Schema(description = "HTTP durum kodu", example = "404")
        int status,

        @Schema(description = "HTTP durum adı", example = "Not Found")
        String error,

        @Schema(description = "Açıklayıcı özel hata mesajı", example = "Ürün bulunamadı: id=99")
        String message,

        @Schema(description = "Hatanın oluştuğu istek yolu", example = "/api/products/99")
        String path,

        @Schema(description = "Alan bazlı doğrulama hataları veya ek ayrıntılar")
        List<String> details) {
}
