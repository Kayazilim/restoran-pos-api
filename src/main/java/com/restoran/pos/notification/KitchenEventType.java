package com.restoran.pos.notification;

/**
 * Mutfak ekranlarına SSE ile gönderilen olay türleri; eventName, SSE "event:" alanına yazılır.
 */
public enum KitchenEventType {

    ORDER_CREATED("order-created"),
    ORDER_UPDATED("order-updated"),
    ORDER_STATUS_CHANGED("order-status-changed"),
    ORDER_CANCELLED("order-cancelled");

    private final String eventName;

    KitchenEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
