package com.restoran.pos.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Masanın QR koduna yazılacak, masaya özel müşteri kimlik bilgileri")
public record QrCredentialsResponse(
        @Schema(example = "7") Integer tableNumber,
        @Schema(description = "HTTP Basic kullanıcı adı", example = "masa-7") String username,
        @Schema(description = "HTTP Basic parolası (QR anahtarı). Yalnızca bu yanıtta gösterilir.") String secret,
        @Schema(description = "QR koda basılacak içerik") String qrContent,
        String warning) {

    public static QrCredentialsResponse of(Integer tableNumber, String username, String secret, String qrBaseUrl) {
        String qrContent = qrBaseUrl + "?masa=" + tableNumber + "&kullanici=" + username + "&anahtar=" + secret;
        return new QrCredentialsResponse(tableNumber, username, secret, qrContent,
                "Anahtar yalnızca bu yanıtta gösterilir; veritabanında yalnızca Argon2id hash'i saklanır. "
                        + "Kaybedilirse QR kodu yeniden oluşturun.");
    }
}
