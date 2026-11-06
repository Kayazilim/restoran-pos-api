package com.restoran.pos.repository.impl;

import com.restoran.pos.entity.RestaurantTable;
import com.restoran.pos.repository.RestaurantTableRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RestaurantTableRepositoryImpl implements RestaurantTableRepository {

    private static final String LOCK_TIMEOUT_HINT = "jakarta.persistence.lock.timeout";

    private final EntityManager entityManager;
    private final int lockTimeoutMs;

    public RestaurantTableRepositoryImpl(EntityManager entityManager,
                                         @Value("${pos.concurrency.lock-timeout-ms}") int lockTimeoutMs) {
        this.entityManager = entityManager;
        this.lockTimeoutMs = lockTimeoutMs;
    }

    @Override
    public RestaurantTable save(RestaurantTable table) {
        entityManager.persist(table);
        return table;
    }

    @Override
    public Optional<RestaurantTable> findById(Long id) {
        return Optional.ofNullable(entityManager.find(RestaurantTable.class, id));
    }

    @Override
    public Optional<RestaurantTable> findByIdForUpdate(Long id) {
        return Optional.ofNullable(entityManager.find(RestaurantTable.class, id,
                LockModeType.PESSIMISTIC_WRITE, Map.of(LOCK_TIMEOUT_HINT, lockTimeoutMs)));
    }

    @Override
    public List<RestaurantTable> findAll() {
        return entityManager.createQuery("SELECT t FROM RestaurantTable t ORDER BY t.tableNumber", RestaurantTable.class)
                .getResultList();
    }

    @Override
    public List<RestaurantTable> findByWaiterId(Long waiterId) {
        return entityManager.createQuery(
                        "SELECT t FROM RestaurantTable t WHERE t.waiter.id = :waiterId ORDER BY t.tableNumber",
                        RestaurantTable.class)
                .setParameter("waiterId", waiterId)
                .getResultList();
    }

    @Override
    public RestaurantTable update(RestaurantTable table) {
        RestaurantTable merged = entityManager.merge(table);
        entityManager.flush();
        return merged;
    }

    @Override
    public void delete(RestaurantTable table) {
        entityManager.remove(entityManager.contains(table) ? table : entityManager.merge(table));
        entityManager.flush();
    }

    @Override
    public boolean existsByTableNumber(Integer tableNumber, Long excludeId) {
        String jpql = "SELECT COUNT(t) FROM RestaurantTable t WHERE t.tableNumber = :tableNumber"
                + (excludeId != null ? " AND t.id <> :excludeId" : "");
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("tableNumber", tableNumber);
        if (excludeId != null) {
            query.setParameter("excludeId", excludeId);
        }
        return query.getSingleResult() > 0;
    }

    @Override
    public long countByWaiterId(Long waiterId) {
        return entityManager.createQuery(
                        "SELECT COUNT(t) FROM RestaurantTable t WHERE t.waiter.id = :waiterId", Long.class)
                .setParameter("waiterId", waiterId)
                .getSingleResult();
    }
}
