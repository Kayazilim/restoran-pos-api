package com.restoran.pos.repository.impl;

import com.restoran.pos.entity.BillStatus;
import com.restoran.pos.entity.OrderStatus;
import com.restoran.pos.entity.PaymentMethod;
import com.restoran.pos.repository.ReportRepository;
import com.restoran.pos.repository.TopProductRow;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReportRepositoryImpl implements ReportRepository {

    private final EntityManager entityManager;

    public ReportRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public long countOrders(Instant from, Instant to, boolean cancelled) {
        String statusCondition = cancelled ? "o.status = :cancelled" : "o.status <> :cancelled";
        return entityManager.createQuery("SELECT COUNT(o) FROM CustomerOrder o"
                        + " WHERE o.createdAt >= :from AND o.createdAt < :to AND " + statusCondition, Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("cancelled", OrderStatus.CANCELLED)
                .getSingleResult();
    }

    @Override
    public BigDecimal sumOrderTotals(Instant from, Instant to) {
        BigDecimal total = entityManager.createQuery("SELECT SUM(o.totalAmount) FROM CustomerOrder o"
                        + " WHERE o.createdAt >= :from AND o.createdAt < :to AND o.status <> :cancelled", BigDecimal.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("cancelled", OrderStatus.CANCELLED)
                .getSingleResult();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public Map<PaymentMethod, BigDecimal> sumPaymentsByMethod(Instant from, Instant to) {
        List<Object[]> rows = entityManager.createQuery("SELECT p.method, SUM(p.amount) FROM Payment p"
                        + " WHERE p.createdAt >= :from AND p.createdAt < :to GROUP BY p.method", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        Map<PaymentMethod, BigDecimal> totals = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            totals.put(method, BigDecimal.ZERO);
        }
        for (Object[] row : rows) {
            totals.put((PaymentMethod) row[0], (BigDecimal) row[1]);
        }
        return totals;
    }

    @Override
    public long countClosedBills(Instant from, Instant to) {
        return entityManager.createQuery("SELECT COUNT(b) FROM Bill b"
                        + " WHERE b.status = :closed AND b.closedAt >= :from AND b.closedAt < :to", Long.class)
                .setParameter("closed", BillStatus.CLOSED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    @Override
    public List<TopProductRow> findTopProducts(Instant from, Instant to, int limit) {
        return entityManager.createQuery("SELECT new com.restoran.pos.repository.TopProductRow("
                        + "i.productName, SUM(i.quantity), SUM(i.lineTotal)) FROM OrderItem i"
                        + " WHERE i.order.createdAt >= :from AND i.order.createdAt < :to"
                        + " AND i.order.status <> :cancelled"
                        + " GROUP BY i.productName ORDER BY SUM(i.quantity) DESC", TopProductRow.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("cancelled", OrderStatus.CANCELLED)
                .setMaxResults(limit)
                .getResultList();
    }
}
