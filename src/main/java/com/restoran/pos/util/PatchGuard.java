package com.restoran.pos.util;

import com.restoran.pos.exception.BadRequestException;

/**
 * PATCH gövdesiyle birincil anahtarın değiştirilmesini engeller.
 */
public final class PatchGuard {

    private PatchGuard() {
    }

    public static void ensureIdUnchanged(Long pathId, Long bodyId) {
        if (bodyId != null && !bodyId.equals(pathId)) {
            throw new BadRequestException("Birincil anahtar (id) PATCH gövdesiyle değiştirilemez (URL'deki id: "
                    + pathId + ", gövdedeki id: " + bodyId + ").");
        }
    }
}
