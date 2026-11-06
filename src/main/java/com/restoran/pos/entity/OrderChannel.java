package com.restoran.pos.entity;

/**
 * Siparişin hangi kanaldan geldiği (çok kanallı sipariş).
 */
public enum OrderChannel {

    QR_MENU("Masadaki QR menü"),
    WAITER("Garson");

    private final String label;

    OrderChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
