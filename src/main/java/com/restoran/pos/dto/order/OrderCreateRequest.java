package com.restoran.pos.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Yeni sipariş. Aynı işlemin tekrar denemeleri aynı 'Idempotency-Key' başlığıyla gönderilmelidir.")
public record OrderCreateRequest(
        @Schema(description = "Masa kimliği. Garson için zorunludur; müşteri (masa) hesaplarında yok sayılır "
                + "ve sipariş her zaman müşterinin kendi masasına yazılır.", example = "1")
        Long tableId,

        @NotEmpty(message = "Sipariş en az bir kalem içermelidir")
        @Size(max = 30, message = "Bir siparişte en fazla 30 kalem olabilir")
        List<@Valid @NotNull(message = "Sipariş kalemi boş olamaz") OrderItemRequest> items,

        @Schema(description = "Siparişe genel not", example = "Doğum günü masası")
        @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
        String note) {
}
