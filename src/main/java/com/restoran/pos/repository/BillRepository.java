package com.restoran.pos.repository;

import com.restoran.pos.entity.Bill;
import com.restoran.pos.entity.BillStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BillRepository {

    Bill save(Bill bill);

    Optional<Bill> findById(Long id);

    Optional<Bill> findByIdForUpdate(Long id);

    /** Masanın açık adisyonunu kilitleyerek okur (masa başına tek açık adisyon garantisi). */
    Optional<Bill> findOpenByTableIdForUpdate(Long tableId);

    Optional<Long> findTableIdByBillId(Long billId);

    List<Bill> search(BillStatus status, Long tableId, Long waiterId);

    Bill update(Bill bill);

    Set<Long> findTableIdsWithOpenBill();

    boolean existsByTableId(Long tableId);

    long countByClosedById(Long userId);
}
