package com.restoran.pos.entity;

/**
 * Siparişin yaşam döngüsü. Stok, sipariş CONFIRMED durumuna geçtiği anda düşülür.
 */
public enum OrderStatus {

    CONFIRMED("Onaylandı"),
    PREPARING("Hazırlanıyor"),
    READY("Hazır"),
    CANCELLED("İptal edildi");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
