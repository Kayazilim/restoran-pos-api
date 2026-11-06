package com.restoran.pos.repository;

import java.math.BigDecimal;

/**
 * En çok satan ürün satırı (JPQL constructor expression ile doldurulur).
 */
public record TopProductRow(String productName, Long quantity, BigDecimal revenue) {
}
