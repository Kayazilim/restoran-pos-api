package com.restoran.pos.entity;

public enum BillStatus {

    OPEN("Açık"),
    CLOSED("Kapalı");

    private final String label;

    BillStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
