package com.restoran.pos.notification;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Mutfak ekranlarına sunucudan anlık bildirim gönderen kanal.
 */
public interface KitchenNotifier {

    SseEmitter subscribe(String username);

    void publish(KitchenOrderEvent event);

    int activeSubscriberCount();
}
