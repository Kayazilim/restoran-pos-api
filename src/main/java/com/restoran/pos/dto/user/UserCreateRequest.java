package com.restoran.pos.dto.user;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Yeni personel hesabı. Parola Argon2id ile hash'lenerek saklanır.")
public record UserCreateRequest(
        @Schema(example = "garson3")
        @NotBlank(message = "Kullanıcı adı boş olamaz")
        @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter olmalıdır")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Kullanıcı adı yalnızca harf, rakam, '.', '_' ve '-' içerebilir")
        String username,

        @Schema(example = "Garson123!")
        @NotBlank(message = "Parola boş olamaz")
        @Size(min = 8, max = 100, message = "Parola 8-100 karakter olmalıdır")
        String password,

        @Schema(example = "Zeynep Kaya")
        @NotBlank(message = "Ad soyad boş olamaz")
        @Size(max = 100, message = "Ad soyad en fazla 100 karakter olabilir")
        String fullName,

        @Schema(description = "Hesap aktif mi? (gönderilmezse true)", example = "true")
        Boolean enabled,

        @ArraySchema(arraySchema = @Schema(description = "Atanacak roller"), schema = @Schema(example = "GARSON"))
        @NotEmpty(message = "En az bir rol atanmalıdır")
        Set<@NotBlank(message = "Rol adı boş olamaz") String> roles) {
}
