package com.restoran.pos.exception;

import org.springframework.http.HttpStatus;

/**
 * İstenen kayıt bulunamadığında fırlatılır (404).
 */
public class ResourceNotFoundException extends PosException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resourceName, Object id) {
        return new ResourceNotFoundException(resourceName + " bulunamadı: id=" + id);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
