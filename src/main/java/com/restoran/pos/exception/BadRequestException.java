package com.restoran.pos.exception;

import org.springframework.http.HttpStatus;

/**
 * İstek içeriği geçersiz olduğunda fırlatılır (400).
 */
public class BadRequestException extends PosException {

    public BadRequestException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
