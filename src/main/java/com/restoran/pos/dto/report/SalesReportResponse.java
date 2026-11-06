package com.restoran.pos.dto.report;

import com.restoran.pos.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "Tarih aralığına göre satış raporu")
public record SalesReportResponse(
        LocalDate from,
        LocalDate to,
        @Schema(example = "Europe/Istanbul") String zoneId,
        @Schema(description = "İptal edilmeyen sipariş sayısı") long orderCount,
        long cancelledOrderCount,
        @Schema(description = "İptal edilmeyen siparişlerin toplam tutarı") BigDecimal grossSales,
        @Schema(description = "Tahsil edilen toplam ödeme") BigDecimal collectedPayments,
        Map<PaymentMethod, BigDecimal> paymentsByMethod,
        long closedBillCount,
        List<TopProductResponse> topProducts) {
}
