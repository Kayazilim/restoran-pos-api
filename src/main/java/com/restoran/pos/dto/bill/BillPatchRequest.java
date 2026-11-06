package com.restoran.pos.dto.bill;

import com.restoran.pos.entity.BillStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Adisyonun kısmi güncellemesi: yalnızca CLOSED durumuna geçirilerek kapatılabilir")
public record BillPatchRequest(
        @Schema(description = "Birincil anahtar değiştirilemez; farklı bir id gönderilirse 400 döner.")
        Long id,

        @Schema(example = "CLOSED", allowableValues = {"CLOSED"})
        @NotNull(message = "status alanı zorunludur")
        BillStatus status) {
}
