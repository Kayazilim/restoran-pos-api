package com.restoran.pos.repository;

import com.restoran.pos.entity.OrderStatus;

import java.util.Collection;

/**
 * Sipariş listeleme filtresi. Null alanlar filtreye dahil edilmez.
 *
 * @param tableId       yalnızca bu masanın siparişleri
 * @param waiterId      yalnızca bu garsona atanmış masaların siparişleri (kayıt sahipliği)
 * @param statuses      yalnızca bu durumdaki siparişler
 * @param onlyOpenBills yalnızca açık adisyona bağlı (güncel oturumdaki) siparişler
 * @param oldestFirst   true ise en eski sipariş en üstte (mutfak ekranı)
 */
public record OrderSearchCriteria(Long tableId,
                                  Long waiterId,
                                  Collection<OrderStatus> statuses,
                                  boolean onlyOpenBills,
                                  boolean oldestFirst) {
}
