package com.restoran.pos.service;

import com.restoran.pos.dto.kitchen.KitchenOrderResponse;
import com.restoran.pos.dto.kitchen.KitchenStatusPatchRequest;
import com.restoran.pos.entity.OrderStatus;
import com.restoran.pos.security.CurrentUser;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface KitchenService {

    List<KitchenOrderResponse> findOrders(OrderStatus status);

    KitchenOrderResponse findById(Long id);

    KitchenOrderResponse updateStatus(Long id, KitchenStatusPatchRequest request);

    /** Mutfak ekranını, yeni sipariş geldiğinde sunucunun haber verdiği SSE kanalına bağlar. */
    SseEmitter subscribe(CurrentUser user);
}
