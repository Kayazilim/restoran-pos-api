package com.restoran.pos.service;

import com.restoran.pos.dto.order.OrderCreateRequest;
import com.restoran.pos.dto.order.OrderPatchRequest;
import com.restoran.pos.dto.order.OrderPlacementResult;
import com.restoran.pos.dto.order.OrderResponse;
import com.restoran.pos.dto.order.OrderUpdateRequest;
import com.restoran.pos.entity.OrderStatus;
import com.restoran.pos.security.CurrentUser;

import java.util.List;

public interface OrderService {

    List<OrderResponse> findAll(OrderStatus status, Long tableId, CurrentUser user);

    OrderResponse findById(Long id, CurrentUser user);

    OrderPlacementResult placeOrder(OrderCreateRequest request, String idempotencyKey, CurrentUser user);

    OrderResponse update(Long id, OrderUpdateRequest request, CurrentUser user);

    OrderResponse patch(Long id, OrderPatchRequest request, CurrentUser user);

    /** Siparişi veritabanından siler; stok iade edilir, tutar adisyondan düşülür. */
    void delete(Long id, CurrentUser user);
}
