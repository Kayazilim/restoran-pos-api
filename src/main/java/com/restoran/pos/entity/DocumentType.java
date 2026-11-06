package com.restoran.pos.entity;

/**
 * Asistanın referans aldığı belge türleri.
 */
public enum DocumentType {

    MENU("Menü belgesi"),
    ALLERGEN("Alerjen belgesi");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
