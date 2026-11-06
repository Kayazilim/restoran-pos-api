package com.restoran.pos.assistant;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Türkçe metin normalleştirme: küçük harfe çevirme, Türkçe karakterleri sadeleştirme (ç→c, ş→s, ı→i ...)
 * ve ekleri tolere etmek için kelime köklerinin ilk harflerini kullanma.
 */
public final class TurkishText {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");
    private static final int STEM_LENGTH = 5;
    private static final int MIN_TOKEN_LENGTH = 3;
    private static final Set<String> STOP_WORDS = Set.of(
            "bir", "ile", "icin", "icinde", "icerir", "iceriyor", "icermez", "icerik", "var", "yok", "midir",
            "mudur", "hangi", "hangisi", "hangileri", "nedir", "neler", "nelerdir", "olan", "olarak", "acaba",
            "lutfen", "gibi", "kadar", "daha", "cok", "veya", "ama", "urun", "urunde", "urunler", "urunlerde",
            "yemek", "yemekte", "yemekler", "yemeklerde", "menu", "menude", "musteri", "sorusu", "soru");

    private TurkishText() {
    }

    public static String fold(String text) {
        String lower = text.toLowerCase(TURKISH);
        StringBuilder folded = new StringBuilder(lower.length());
        for (char character : lower.toCharArray()) {
            folded.append(switch (character) {
                case 'ç' -> 'c';
                case 'ğ' -> 'g';
                case 'ı', 'î' -> 'i';
                case 'ö' -> 'o';
                case 'ş' -> 's';
                case 'ü', 'û' -> 'u';
                case 'â' -> 'a';
                default -> Character.isLetterOrDigit(character) ? character : ' ';
            });
        }
        return folded.toString();
    }

    /** Sorudaki anlamlı kelimelerin köklerini döndürür (ör. "baklavada fıstık var mı" → [bakla, fisti]). */
    public static Set<String> keywordStems(String text) {
        Set<String> stems = new LinkedHashSet<>();
        for (String token : fold(text).split("\\s+")) {
            if (token.length() < MIN_TOKEN_LENGTH || STOP_WORDS.contains(token)) {
                continue;
            }
            stems.add(token.length() > STEM_LENGTH ? token.substring(0, STEM_LENGTH) : token);
        }
        return stems;
    }
}
