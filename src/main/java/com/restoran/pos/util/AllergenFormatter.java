package com.restoran.pos.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Alerjen listesini veritabanındaki virgülle ayrılmış biçime ve geri dönüştürür.
 */
public final class AllergenFormatter {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    private AllergenFormatter() {
    }

    public static String join(List<String> allergens) {
        if (allergens == null) {
            return null;
        }
        String joined = allergens.stream()
                .filter(Objects::nonNull)
                .map(allergen -> allergen.trim().toLowerCase(TURKISH))
                .filter(allergen -> !allergen.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    public static List<String> split(String allergens) {
        if (allergens == null || allergens.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allergens.split(","))
                .map(String::trim)
                .filter(allergen -> !allergen.isEmpty())
                .toList();
    }
}
