package com.restoran.pos.dto.order;

/**
 * Sipariş oluşturma sonucu.
 *
 * @param replayed true ise aynı Idempotency-Key ile daha önce oluşturulmuş sipariş döndürülmüştür
 *                 (yeni sipariş açılmamış, stok tekrar düşülmemiştir)
 */
public record OrderPlacementResult(OrderResponse order, boolean replayed) {
}
