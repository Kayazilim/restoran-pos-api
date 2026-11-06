package com.restoran.pos.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

/**
 * Kimliği doğrulanmış ama gerekli yetkisi olmayan isteklerde (403) hatayı merkezi hata işleyiciye devreder.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public RestAccessDeniedHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ModelAndView handled = handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
        if (handled == null && !response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
