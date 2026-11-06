package com.restoran.pos.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Parola değişikliği; yeni parola Argon2id ile hash'lenir")
public record PasswordChangeRequest(
        @Schema(example = "YeniParola123!")
        @NotBlank(message = "Yeni parola boş olamaz")
        @Size(min = 8, max = 100, message = "Parola 8-100 karakter olmalıdır")
        String newPassword) {
}
