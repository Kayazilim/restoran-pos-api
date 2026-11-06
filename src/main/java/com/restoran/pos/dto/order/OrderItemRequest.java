package com.restoran.pos.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Sipariş kalemi")
public record OrderItemRequest(
        @Schema(description = "Ürün kimliği", example = "1")
        @NotNull(message = "Ürün kimliği zorunludur")
        Long productId,

        @Schema(description = "Adet", example = "2")
        @NotNull(message = "Adet zorunludur")
        @Min(value = 1, message = "Adet en az 1 olmalıdır")
        Integer quantity,

        @Schema(description = "Kaleme özel not", example = "Az acılı")
        @Size(max = 200, message = "Kalem notu en fazla 200 karakter olabilir")
        String note) {
}
