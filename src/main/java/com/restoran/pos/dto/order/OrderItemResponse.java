package com.restoran.pos.dto.order;

import com.restoran.pos.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal,
        String note) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getProduct().getId(), item.getProductName(),
                item.getUnitPrice(), item.getQuantity(), item.getLineTotal(), item.getNote());
    }
}
