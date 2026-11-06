package com.restoran.pos.dto.bill;

import com.restoran.pos.dto.order.OrderResponse;
import com.restoran.pos.entity.Bill;
import com.restoran.pos.entity.BillStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Adisyon: masanın siparişleri, ödemeleri ve kalan tutar")
public record BillResponse(
        @Schema(example = "4") Long id,
        @Schema(example = "1") Long tableId,
        @Schema(example = "1") Integer tableNumber,
        BillStatus status,
        @Schema(example = "Açık") String statusLabel,
        @Schema(example = "460.00") BigDecimal totalAmount,
        @Schema(example = "200.00") BigDecimal paidAmount,
        @Schema(example = "260.00") BigDecimal remainingAmount,
        Instant openedAt,
        Instant closedAt,
        String closedBy,
        List<OrderResponse> orders,
        List<PaymentResponse> payments) {

    public static BillResponse from(Bill bill) {
        return new BillResponse(bill.getId(), bill.getTable().getId(), bill.getTable().getTableNumber(),
                bill.getStatus(), bill.getStatus().getLabel(), bill.getTotalAmount(), bill.getPaidAmount(),
                bill.getRemainingAmount(), bill.getOpenedAt(), bill.getClosedAt(),
                bill.getClosedBy() != null ? bill.getClosedBy().getUsername() : null,
                bill.getOrders().stream().map(OrderResponse::from).toList(),
                bill.getPayments().stream().map(PaymentResponse::from).toList());
    }
}
