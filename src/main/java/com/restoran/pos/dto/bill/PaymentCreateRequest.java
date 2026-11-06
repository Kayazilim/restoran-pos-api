package com.restoran.pos.dto.bill;

import com.restoran.pos.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Ödeme isteği. Çifte tahsilatı önlemek için 'Idempotency-Key' başlığıyla gönderilir.")
public record PaymentCreateRequest(
        @Schema(example = "460.00")
        @NotNull(message = "Tutar zorunludur")
        @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalıdır")
        @Digits(integer = 10, fraction = 2, message = "Tutar en fazla 10 tam ve 2 ondalık basamak içerebilir")
        BigDecimal amount,

        @Schema(example = "CARD")
        @NotNull(message = "Ödeme yöntemi zorunludur")
        PaymentMethod method) {
}
