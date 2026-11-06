package com.restoran.pos.dto.bill;

import com.restoran.pos.entity.Payment;
import com.restoran.pos.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long billId,
        BigDecimal amount,
        PaymentMethod method,
        String methodLabel,
        String receivedBy,
        Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getBill().getId(), payment.getAmount(),
                payment.getMethod(), payment.getMethod().getLabel(), payment.getReceivedBy().getUsername(),
                payment.getCreatedAt());
    }
}
