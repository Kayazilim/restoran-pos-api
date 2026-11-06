package com.restoran.pos.repository;

import com.restoran.pos.entity.CustomerOrder;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    CustomerOrder save(CustomerOrder order);

    Optional<CustomerOrder> findById(Long id);

    Optional<CustomerOrder> findByIdForUpdate(Long id);

    Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey);

    /** Siparişi yüklemeden yalnızca masa kimliğini döndürür (kilit sırasını korumak için). */
    Optional<Long> findTableIdByOrderId(Long orderId);

    List<CustomerOrder> search(OrderSearchCriteria criteria);

    CustomerOrder update(CustomerOrder order);

    void delete(CustomerOrder order);

    long countByBillId(Long billId);

    long countByCreatedById(Long userId);
}
