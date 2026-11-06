package com.restoran.pos.exception;

import org.springframework.http.HttpStatus;

/**
 * Kullanıcının yetkisi olsa bile kayıt sahipliği veya rol kuralı nedeniyle işleme izin verilmediğinde
 * fırlatılır (403); ör. bir garsonun başka garsonun masasına erişmesi.
 */
public class ForbiddenOperationException extends PosException {

    public ForbiddenOperationException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
