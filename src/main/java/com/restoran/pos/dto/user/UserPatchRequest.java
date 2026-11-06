package com.restoran.pos.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "PATCH ile kısmi güncelleme: ör. yalnızca aktiflik veya rol ataması değiştirilebilir")
public record UserPatchRequest(
        @Schema(description = "Birincil anahtar değiştirilemez; farklı bir id gönderilirse 400 döner.")
        Long id,

        @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter olmalıdır")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Kullanıcı adı yalnızca harf, rakam, '.', '_' ve '-' içerebilir")
        String username,

        @Pattern(regexp = "(?s).*\\S.*", message = "Ad soyad boş olamaz")
        @Size(max = 100, message = "Ad soyad en fazla 100 karakter olabilir")
        String fullName,

        @Schema(example = "false")
        Boolean enabled,

        @Size(min = 1, message = "Rol listesi gönderilecekse en az bir rol içermelidir")
        Set<@NotBlank(message = "Rol adı boş olamaz") String> roles) {
}
