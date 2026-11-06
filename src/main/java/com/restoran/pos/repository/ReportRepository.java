package com.restoran.pos.repository;

import com.restoran.pos.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Yönetici raporları için toplama (aggregate) JPQL sorguları.
 */
public interface ReportRepository {

    long countOrders(Instant from, Instant to, boolean cancelled);

    BigDecimal sumOrderTotals(Instant from, Instant to);

    Map<PaymentMethod, BigDecimal> sumPaymentsByMethod(Instant from, Instant to);

    long countClosedBills(Instant from, Instant to);

    List<TopProductRow> findTopProducts(Instant from, Instant to, int limit);
}
