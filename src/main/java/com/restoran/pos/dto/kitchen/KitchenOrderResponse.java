package com.restoran.pos.dto.kitchen;

import com.restoran.pos.entity.CustomerOrder;
import com.restoran.pos.entity.OrderChannel;
import com.restoran.pos.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Mutfak görünümü: fiyat, ödeme ve stok bilgisi İÇERMEZ")
public record KitchenOrderResponse(
        @Schema(example = "15") Long orderId,
        @Schema(example = "3") Integer tableNumber,
        OrderStatus status,
        @Schema(example = "Hazırlanıyor") String statusLabel,
        OrderChannel channel,
        String note,
        List<KitchenOrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    public static KitchenOrderResponse from(CustomerOrder order) {
        return new KitchenOrderResponse(order.getId(), order.getTable().getTableNumber(), order.getStatus(),
                order.getStatus().getLabel(), order.getChannel(), order.getNote(),
                order.getItems().stream().map(KitchenOrderItemResponse::from).toList(),
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
