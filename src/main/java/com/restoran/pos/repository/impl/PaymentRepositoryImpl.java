package com.restoran.pos.repository.impl;

import com.restoran.pos.entity.Payment;
import com.restoran.pos.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final EntityManager entityManager;

    public PaymentRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Payment save(Payment payment) {
        entityManager.persist(payment);
        entityManager.flush();
        return payment;
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Payment.class, id));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return entityManager.createQuery(
                        "SELECT p FROM Payment p WHERE p.idempotencyKey = :idempotencyKey", Payment.class)
                .setParameter("idempotencyKey", idempotencyKey)
                .getResultList().stream()
                .findFirst();
    }

    @Override
    public List<Payment> findByBillId(Long billId) {
        return entityManager.createQuery("SELECT p FROM Payment p WHERE p.bill.id = :billId ORDER BY p.id", Payment.class)
                .setParameter("billId", billId)
                .getResultList();
    }

    @Override
    public long countByReceivedById(Long userId) {
        return entityManager.createQuery("SELECT COUNT(p) FROM Payment p WHERE p.receivedBy.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }
}
