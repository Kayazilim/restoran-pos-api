package com.restoran.pos.util;

import com.restoran.pos.exception.BadRequestException;

import java.util.regex.Pattern;

/**
 * Idempotency-Key başlığının biçim denetimi. İstemci her "Siparişi Ver"/"Ödeme Al" denemesi için
 * bir anahtar (ör. UUID) üretir ve tekrar denemelerde aynı anahtarı gönderir.
 */
public final class IdempotencyKeys {

    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9_-]{8,100}");

    private IdempotencyKeys() {
    }

    public static String validate(String key) {
        String trimmed = key == null ? "" : key.trim();
        if (!VALID_KEY.matcher(trimmed).matches()) {
            throw new BadRequestException("Idempotency-Key başlığı 8-100 karakter olmalı ve yalnızca harf, rakam, "
                    + "'-' ve '_' içermelidir (ör. bir UUID).");
        }
        return trimmed;
    }
}
