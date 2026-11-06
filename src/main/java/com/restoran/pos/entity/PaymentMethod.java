package com.restoran.pos.entity;

public enum PaymentMethod {

    CASH("Nakit"),
    CARD("Kart");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
