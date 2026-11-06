package com.restoran.pos.repository.impl;

import com.restoran.pos.entity.Bill;
import com.restoran.pos.entity.BillStatus;
import com.restoran.pos.repository.BillRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class BillRepositoryImpl implements BillRepository {

    private static final String LOCK_TIMEOUT_HINT = "jakarta.persistence.lock.timeout";

    private final EntityManager entityManager;
    private final int lockTimeoutMs;

    public BillRepositoryImpl(EntityManager entityManager,
                              @Value("${pos.concurrency.lock-timeout-ms}") int lockTimeoutMs) {
        this.entityManager = entityManager;
        this.lockTimeoutMs = lockTimeoutMs;
    }

    @Override
    public Bill save(Bill bill) {
        entityManager.persist(bill);
        return bill;
    }

    @Override
    public Optional<Bill> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Bill.class, id));
    }

    @Override
    public Optional<Bill> findByIdForUpdate(Long id) {
        return Optional.ofNullable(entityManager.find(Bill.class, id,
                LockModeType.PESSIMISTIC_WRITE, Map.of(LOCK_TIMEOUT_HINT, lockTimeoutMs)));
    }

    @Override
    public Optional<Bill> findOpenByTableIdForUpdate(Long tableId) {
        return entityManager.createQuery(
                        "SELECT b FROM Bill b WHERE b.table.id = :tableId AND b.status = :status", Bill.class)
                .setParameter("tableId", tableId)
                .setParameter("status", BillStatus.OPEN)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(LOCK_TIMEOUT_HINT, lockTimeoutMs)
                .getResultList().stream()
                .findFirst();
    }

    @Override
    public Optional<Long> findTableIdByBillId(Long billId) {
        return entityManager.createQuery("SELECT b.table.id FROM Bill b WHERE b.id = :billId", Long.class)
                .setParameter("billId", billId)
                .getResultList().stream()
                .findFirst();
    }

    @Override
    public List<Bill> search(BillStatus status, Long tableId, Long waiterId) {
        StringBuilder jpql = new StringBuilder("SELECT b FROM Bill b JOIN b.table t WHERE 1 = 1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (status != null) {
            jpql.append(" AND b.status = :status");
            parameters.put("status", status);
        }
        if (tableId != null) {
            jpql.append(" AND t.id = :tableId");
            parameters.put("tableId", tableId);
        }
        if (waiterId != null) {
            jpql.append(" AND t.waiter.id = :waiterId");
            parameters.put("waiterId", waiterId);
        }
        jpql.append(" ORDER BY b.openedAt DESC, b.id DESC");

        TypedQuery<Bill> query = entityManager.createQuery(jpql.toString(), Bill.class);
        parameters.forEach(query::setParameter);
        return query.getResultList();
    }

    @Override
    public Bill update(Bill bill) {
        Bill merged = entityManager.merge(bill);
        entityManager.flush();
        return merged;
    }

    @Override
    public void delete(Bill bill) {
        entityManager.remove(entityManager.contains(bill) ? bill : entityManager.merge(bill));
        entityManager.flush();
    }

    @Override
    public Set<Long> findTableIdsWithOpenBill() {
        return new HashSet<>(entityManager.createQuery(
                        "SELECT b.table.id FROM Bill b WHERE b.status = :status", Long.class)
                .setParameter("status", BillStatus.OPEN)
                .getResultList());
    }

    @Override
    public boolean existsByTableId(Long tableId) {
        return entityManager.createQuery("SELECT COUNT(b) FROM Bill b WHERE b.table.id = :tableId", Long.class)
                .setParameter("tableId", tableId)
                .getSingleResult() > 0;
    }

    @Override
    public long countByClosedById(Long userId) {
        return entityManager.createQuery("SELECT COUNT(b) FROM Bill b WHERE b.closedBy.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }
}
