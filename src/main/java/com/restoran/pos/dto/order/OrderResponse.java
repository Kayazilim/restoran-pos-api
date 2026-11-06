package com.restoran.pos.dto.order;

import com.restoran.pos.entity.CustomerOrder;
import com.restoran.pos.entity.OrderChannel;
import com.restoran.pos.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Sipariş (fiyat bilgisi dahil; müşteri, garson, kasiyer ve yönetici görünümü)")
public record OrderResponse(
        @Schema(example = "15") Long id,
        @Schema(example = "1") Long tableId,
        @Schema(example = "1") Integer tableNumber,
        @Schema(example = "4") Long billId,
        OrderStatus status,
        @Schema(example = "Onaylandı") String statusLabel,
        OrderChannel channel,
        @Schema(example = "masa-1") String createdBy,
        String note,
        @Schema(example = "460.00") BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Long version,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(order.getId(), order.getTable().getId(), order.getTable().getTableNumber(),
                order.getBill().getId(), order.getStatus(), order.getStatus().getLabel(), order.getChannel(),
                order.getCreatedBy().getUsername(), order.getNote(), order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getVersion(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
