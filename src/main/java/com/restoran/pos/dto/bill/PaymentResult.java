package com.restoran.pos.dto.bill;

/**
 * @param replayed true ise aynı Idempotency-Key ile alınmış önceki ödeme döndürülmüştür (tekrar tahsilat yapılmaz)
 */
public record PaymentResult(PaymentReceiptResponse receipt, boolean replayed) {
}
