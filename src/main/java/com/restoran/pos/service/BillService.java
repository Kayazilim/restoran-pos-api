package com.restoran.pos.service;

import com.restoran.pos.dto.bill.BillPatchRequest;
import com.restoran.pos.dto.bill.BillResponse;
import com.restoran.pos.dto.bill.PaymentCreateRequest;
import com.restoran.pos.dto.bill.PaymentResponse;
import com.restoran.pos.dto.bill.PaymentResult;
import com.restoran.pos.entity.BillStatus;
import com.restoran.pos.security.CurrentUser;

import java.util.List;

public interface BillService {

    List<BillResponse> findAll(BillStatus status, CurrentUser user);

    BillResponse findById(Long id, CurrentUser user);

    List<PaymentResponse> findPayments(Long billId, CurrentUser user);

    PaymentResult addPayment(Long billId, PaymentCreateRequest request, String idempotencyKey, CurrentUser user);

    BillResponse patch(Long billId, BillPatchRequest request, CurrentUser user);
}
