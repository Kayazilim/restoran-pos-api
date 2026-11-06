package com.restoran.pos.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

/**
 * Kimlik doğrulaması olmayan veya başarısız olan isteklerde (401) hatayı, merkezi
 * {@code GlobalExceptionHandler}'a devreder; böylece güvenlik hataları da ortak JSON hata modeliyle döner.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public RestAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ModelAndView handled = handlerExceptionResolver.resolveException(request, response, null, authException);
        if (handled == null && !response.isCommitted()) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"restoran-pos\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
