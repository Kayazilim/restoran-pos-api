package com.restoran.pos.assistant;

import java.util.ArrayList;
import java.util.List;

/**
 * Belgeleri, boş satırlarla ayrılmış bloklara (paragraf/ürün bölümü) ayırır.
 */
public final class KnowledgeChunker {

    private KnowledgeChunker() {
    }

    /** Her blok ayrı bir parça olarak döner (anahtar kelime aramasında hassas eşleşme için). */
    public static List<String> blocks(String content) {
        List<String> blocks = new ArrayList<>();
        for (String rawBlock : content.replace("\r\n", "\n").split("\\n\\s*\\n")) {
            String block = rawBlock.strip();
            if (!block.isEmpty()) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    /** Ardışık küçük blokları en fazla maxLength karakterlik parçalarda birleştirir (embedding için). */
    public static List<String> chunks(String content, int maxLength) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String block : blocks(content)) {
            if (!current.isEmpty() && current.length() + block.length() + 2 > maxLength) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(block);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }
}
