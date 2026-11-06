package com.restoran.pos.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "PUT ile siparişin tam güncellenmesi: kalemlerin tamamı yeniden gönderilir, stok farkı otomatik düzeltilir. "
        + "Yalnızca 'Onaylandı' durumundaki siparişler değiştirilebilir.")
public record OrderUpdateRequest(
        @NotEmpty(message = "Sipariş en az bir kalem içermelidir")
        @Size(max = 30, message = "Bir siparişte en fazla 30 kalem olabilir")
        List<@Valid @NotNull(message = "Sipariş kalemi boş olamaz") OrderItemRequest> items,

        @Schema(description = "Siparişe genel not; gönderilmezse not silinir")
        @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
        String note) {
}
