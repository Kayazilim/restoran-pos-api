package com.restoran.pos.dto.kitchen;

import com.restoran.pos.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Mutfağın hazırlık durumu güncellemesi (yalnızca PREPARING veya READY)")
public record KitchenStatusPatchRequest(
        @Schema(description = "Birincil anahtar değiştirilemez; farklı bir id gönderilirse 400 döner.")
        Long id,

        @Schema(description = "Yeni durum", example = "PREPARING", allowableValues = {"PREPARING", "READY"})
        @NotNull(message = "status alanı zorunludur")
        OrderStatus status) {
}
