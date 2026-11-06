package com.restoran.pos.dto.role;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Yeni rol tanımı (ör. paket servis platformu gibi dış sistemler için)")
public record RoleCreateRequest(
        @Schema(example = "PAKET_SERVIS")
        @NotBlank(message = "Rol adı boş olamaz")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "Rol adı büyük harfle başlamalı; yalnızca A-Z, 0-9 ve '_' içermelidir")
        String name,

        @Schema(example = "Paket servis platformu entegrasyonu: menüyü okur")
        @NotBlank(message = "Açıklama boş olamaz")
        @Size(max = 200, message = "Açıklama en fazla 200 karakter olabilir")
        String description,

        @ArraySchema(arraySchema = @Schema(description = "Role verilecek yetkiler"), schema = @Schema(example = "MENU_READ"))
        @NotEmpty(message = "En az bir yetki seçilmelidir")
        Set<@NotBlank(message = "Yetki adı boş olamaz") String> permissions) {
}
