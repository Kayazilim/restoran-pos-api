package com.restoran.pos.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Uygulamaya özgü hataların ortak üst sınıfı. Her alt sınıf kendi HTTP durum kodunu belirler.
 */
public abstract class PosException extends RuntimeException {

    private final List<String> details;

    protected PosException(String message) {
        this(message, List.of());
    }

    protected PosException(String message, List<String> details) {
        super(message);
        this.details = List.copyOf(details);
    }

    public abstract HttpStatus getStatus();

    public List<String> getDetails() {
        return details;
    }
}
