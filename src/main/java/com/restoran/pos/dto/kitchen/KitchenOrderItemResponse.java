package com.restoran.pos.dto.kitchen;

import com.restoran.pos.entity.OrderItem;

public record KitchenOrderItemResponse(String productName, Integer quantity, String note) {

    public static KitchenOrderItemResponse from(OrderItem item) {
        return new KitchenOrderItemResponse(item.getProductName(), item.getQuantity(), item.getNote());
    }
}
