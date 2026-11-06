package com.restoran.pos.service.impl;

import com.restoran.pos.dto.kitchen.KitchenOrderResponse;
import com.restoran.pos.dto.kitchen.KitchenStatusPatchRequest;
import com.restoran.pos.entity.CustomerOrder;
import com.restoran.pos.entity.OrderStatus;
import com.restoran.pos.exception.BadRequestException;
import com.restoran.pos.exception.ConflictException;
import com.restoran.pos.exception.ResourceNotFoundException;
import com.restoran.pos.notification.KitchenEventType;
import com.restoran.pos.notification.KitchenNotifier;
import com.restoran.pos.notification.KitchenOrderEvent;
import com.restoran.pos.repository.OrderRepository;
import com.restoran.pos.repository.OrderSearchCriteria;
import com.restoran.pos.security.CurrentUser;
import com.restoran.pos.service.KitchenService;
import com.restoran.pos.util.PatchGuard;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.List;

/**
 * Mutfak iş akışı: yeni siparişleri görür ve hazırlık durumunu günceller. Mutfak görünümü fiyat, ödeme ve stok
 * bilgisi içermez.
 */
@Service
public class KitchenServiceImpl implements KitchenService {

    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING);

    private final OrderRepository orderRepository;
    private final KitchenNotifier kitchenNotifier;
    private final ApplicationEventPublisher eventPublisher;

    public KitchenServiceImpl(OrderRepository orderRepository,
                              KitchenNotifier kitchenNotifier,
                              ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.kitchenNotifier = kitchenNotifier;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenOrderResponse> findOrders(OrderStatus status) {
        Collection<OrderStatus> statuses = status != null ? List.of(status) : ACTIVE_STATUSES;
        return orderRepository.search(new OrderSearchCriteria(null, null, statuses, true, true)).stream()
                .map(KitchenOrderResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public KitchenOrderResponse findById(Long id) {
        return KitchenOrderResponse.from(orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Sipariş", id)));
    }

    @Override
    @Transactional
    public KitchenOrderResponse updateStatus(Long id, KitchenStatusPatchRequest request) {
        PatchGuard.ensureIdUnchanged(id, request.id());
        OrderStatus target = request.status();
        if (target != OrderStatus.PREPARING && target != OrderStatus.READY) {
            throw new BadRequestException("Mutfak siparişi yalnızca PREPARING (Hazırlanıyor) veya READY (Hazır) "
                    + "durumuna geçirebilir.");
        }

        // Sipariş satırı kilitlenir: garsonun aynı anda yaptığı düzenleme ile mutfağın durum güncellemesi çakışmaz.
        CustomerOrder order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Sipariş", id));
        OrderStatus current = order.getStatus();
        boolean allowed = (current == OrderStatus.CONFIRMED && (target == OrderStatus.PREPARING || target == OrderStatus.READY))
                || (current == OrderStatus.PREPARING && target == OrderStatus.READY);
        if (!allowed) {
            throw new ConflictException("Geçersiz durum geçişi: '" + current.getLabel() + "' → '" + target.getLabel() + "'.");
        }

        order.setStatus(target);
        KitchenOrderResponse response = KitchenOrderResponse.from(orderRepository.update(order));
        eventPublisher.publishEvent(new KitchenOrderEvent(KitchenEventType.ORDER_STATUS_CHANGED, response));
        return response;
    }

    @Override
    public SseEmitter subscribe(CurrentUser user) {
        return kitchenNotifier.subscribe(user.username());
    }
}
