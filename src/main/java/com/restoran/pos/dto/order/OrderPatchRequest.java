package com.restoran.pos.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "PATCH ile kısmi güncelleme: yalnızca gönderilen alanlar (not ve/veya kalemler) değişir.")
public record OrderPatchRequest(
        @Schema(description = "Birincil anahtar değiştirilemez; farklı bir id gönderilirse 400 döner.")
        Long id,

        @Size(min = 1, max = 30, message = "Kalem listesi gönderilecekse 1-30 kalem içermelidir")
        List<@Valid @NotNull(message = "Sipariş kalemi boş olamaz") OrderItemRequest> items,

        @Schema(example = "Tatlılar kahveyle birlikte gelsin")
        @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
        String note) {
}
