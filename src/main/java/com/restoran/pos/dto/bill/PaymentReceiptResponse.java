package com.restoran.pos.dto.bill;

import com.restoran.pos.entity.Bill;
import com.restoran.pos.entity.Payment;

import java.math.BigDecimal;

/**
 * Ödeme makbuzu: alınan ödeme ve adisyonun güncel durumu.
 */
public record PaymentReceiptResponse(
        PaymentResponse payment,
        Long billId,
        BigDecimal billTotal,
        BigDecimal paidAmount,
        BigDecimal remainingAmount) {

    public static PaymentReceiptResponse of(Payment payment, Bill bill) {
        return new PaymentReceiptResponse(PaymentResponse.from(payment), bill.getId(), bill.getTotalAmount(),
                bill.getPaidAmount(), bill.getRemainingAmount());
    }
}
