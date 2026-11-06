package com.restoran.pos.notification;

import com.restoran.pos.dto.kitchen.KitchenOrderResponse;

/**
 * Sipariş değişikliği olayı. Yük (payload) fiyat/stok içermeyen mutfak görünümüdür.
 */
public record KitchenOrderEvent(KitchenEventType type, KitchenOrderResponse order) {
}
