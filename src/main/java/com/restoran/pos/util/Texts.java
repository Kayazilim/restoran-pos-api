package com.restoran.pos.util;

public final class Texts {

    private Texts() {
    }

    /** Baştaki/sondaki boşlukları siler; sonuç boşsa null döndürür. */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
