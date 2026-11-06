package com.restoran.pos.repository;

import com.restoran.pos.entity.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(Long id);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByBillId(Long billId);

    long countByReceivedById(Long userId);
}
