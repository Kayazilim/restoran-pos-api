package com.restoran.pos.dto.user;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "PUT ile hesabın tam güncellenmesi (parola ayrı uç noktadan değiştirilir)")
public record UserUpdateRequest(
        @Schema(example = "garson3")
        @NotBlank(message = "Kullanıcı adı boş olamaz")
        @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter olmalıdır")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Kullanıcı adı yalnızca harf, rakam, '.', '_' ve '-' içerebilir")
        String username,

        @Schema(example = "Zeynep Kaya Demir")
        @NotBlank(message = "Ad soyad boş olamaz")
        @Size(max = 100, message = "Ad soyad en fazla 100 karakter olabilir")
        String fullName,

        @Schema(example = "true")
        @NotNull(message = "enabled alanı zorunludur")
        Boolean enabled,

        @ArraySchema(arraySchema = @Schema(description = "Atanacak roller"), schema = @Schema(example = "KASIYER"))
        @NotEmpty(message = "En az bir rol atanmalıdır")
        Set<@NotBlank(message = "Rol adı boş olamaz") String> roles) {
}
