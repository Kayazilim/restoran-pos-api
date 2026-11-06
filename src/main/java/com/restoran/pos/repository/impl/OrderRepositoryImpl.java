package com.restoran.pos.repository.impl;

import com.restoran.pos.entity.BillStatus;
import com.restoran.pos.entity.CustomerOrder;
import com.restoran.pos.repository.OrderRepository;
import com.restoran.pos.repository.OrderSearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private static final String LOCK_TIMEOUT_HINT = "jakarta.persistence.lock.timeout";

    private final EntityManager entityManager;
    private final int lockTimeoutMs;

    public OrderRepositoryImpl(EntityManager entityManager,
                               @Value("${pos.concurrency.lock-timeout-ms}") int lockTimeoutMs) {
        this.entityManager = entityManager;
        this.lockTimeoutMs = lockTimeoutMs;
    }

    @Override
    public CustomerOrder save(CustomerOrder order) {
        entityManager.persist(order);
        // UNIQUE (idempotency_key) ihlali varsa hemen burada ortaya çıksın.
        entityManager.flush();
        return order;
    }

    @Override
    public Optional<CustomerOrder> findById(Long id) {
        return Optional.ofNullable(entityManager.find(CustomerOrder.class, id));
    }

    @Override
    public Optional<CustomerOrder> findByIdForUpdate(Long id) {
        return Optional.ofNullable(entityManager.find(CustomerOrder.class, id,
                LockModeType.PESSIMISTIC_WRITE, Map.of(LOCK_TIMEOUT_HINT, lockTimeoutMs)));
    }

    @Override
    public Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey) {
        return entityManager.createQuery(
                        "SELECT o FROM CustomerOrder o WHERE o.idempotencyKey = :idempotencyKey", CustomerOrder.class)
                .setParameter("idempotencyKey", idempotencyKey)
                .getResultList().stream()
                .findFirst();
    }

    @Override
    public Optional<Long> findTableIdByOrderId(Long orderId) {
        return entityManager.createQuery("SELECT o.table.id FROM CustomerOrder o WHERE o.id = :orderId", Long.class)
                .setParameter("orderId", orderId)
                .getResultList().stream()
                .findFirst();
    }

    @Override
    public List<CustomerOrder> search(OrderSearchCriteria criteria) {
        StringBuilder jpql = new StringBuilder("SELECT o FROM CustomerOrder o JOIN o.table t JOIN o.bill b WHERE 1 = 1");
        Map<String, Object> parameters = new LinkedHashMap<>();

        if (criteria.tableId() != null) {
            jpql.append(" AND t.id = :tableId");
            parameters.put("tableId", criteria.tableId());
        }
        if (criteria.waiterId() != null) {
            jpql.append(" AND t.waiter.id = :waiterId");
            parameters.put("waiterId", criteria.waiterId());
        }
        if (criteria.statuses() != null && !criteria.statuses().isEmpty()) {
            jpql.append(" AND o.status IN :statuses");
            parameters.put("statuses", criteria.statuses());
        }
        if (criteria.onlyOpenBills()) {
            jpql.append(" AND b.status = :billStatus");
            parameters.put("billStatus", BillStatus.OPEN);
        }
        jpql.append(criteria.oldestFirst() ? " ORDER BY o.createdAt ASC, o.id ASC" : " ORDER BY o.createdAt DESC, o.id DESC");

        TypedQuery<CustomerOrder> query = entityManager.createQuery(jpql.toString(), CustomerOrder.class);
        parameters.forEach(query::setParameter);
        return query.getResultList();
    }

    @Override
    public CustomerOrder update(CustomerOrder order) {
        CustomerOrder merged = entityManager.merge(order);
        entityManager.flush();
        return merged;
    }

    @Override
    public void delete(CustomerOrder order) {
        // Sipariş kalemleri (order_items) CascadeType.ALL + orphanRemoval ile birlikte silinir.
        entityManager.remove(entityManager.contains(order) ? order : entityManager.merge(order));
        entityManager.flush();
    }

    @Override
    public long countByBillId(Long billId) {
        return entityManager.createQuery("SELECT COUNT(o) FROM CustomerOrder o WHERE o.bill.id = :billId", Long.class)
                .setParameter("billId", billId)
                .getSingleResult();
    }

    @Override
    public long countByCreatedById(Long userId) {
        return entityManager.createQuery("SELECT COUNT(o) FROM CustomerOrder o WHERE o.createdBy.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }
}
