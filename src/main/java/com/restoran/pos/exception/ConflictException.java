package com.restoran.pos.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * İstek geçerli olsa da kaydın mevcut durumuyla çeliştiğinde fırlatılır (409);
 * ör. tekrar eden benzersiz alan, kapalı adisyona işlem veya geçersiz durum geçişi.
 */
public class ConflictException extends PosException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, List<String> details) {
        super(message, details);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
