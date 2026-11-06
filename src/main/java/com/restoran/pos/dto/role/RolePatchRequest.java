package com.restoran.pos.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "PATCH ile rolün kısmi güncellenmesi")
public record RolePatchRequest(
        @Schema(description = "Birincil anahtar değiştirilemez; farklı bir id gönderilirse 400 döner.")
        Long id,

        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "Rol adı büyük harfle başlamalı; yalnızca A-Z, 0-9 ve '_' içermelidir")
        String name,

        @Pattern(regexp = "(?s).*\\S.*", message = "Açıklama boş olamaz")
        @Size(max = 200, message = "Açıklama en fazla 200 karakter olabilir")
        String description,

        @Size(min = 1, message = "Yetki listesi gönderilecekse en az bir yetki içermelidir")
        Set<@NotBlank(message = "Yetki adı boş olamaz") String> permissions) {
}
