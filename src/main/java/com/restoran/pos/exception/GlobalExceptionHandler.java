package com.restoran.pos.exception;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tüm hataların tek merkezden yönetildiği sınıf. Her hata, durum kodu ile tutarlı
 * {@link ApiError} gövdesiyle döner. Güvenlik filtresindeki 401/403 hataları da
 * (RestAuthenticationEntryPoint ve RestAccessDeniedHandler üzerinden) buraya yönlendirilir.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String BASIC_CHALLENGE = "Basic realm=\"restoran-pos\", charset=\"UTF-8\"";
    private static final int MAX_DETAIL_LENGTH = 200;

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(PosException.class)
    public ResponseEntity<ApiError> handlePosException(PosException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), ex.getDetails(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> details.add(error.getField() + ": " + error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> details.add(error.getObjectName() + ": " + error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "İstek doğrulanamadı; lütfen alanları kontrol edin.", details, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex,
                                                           HttpServletRequest request) {
        List<String> details = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "İstek parametreleri doğrulanamadı.", details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "İstek doğrulanamadı.", details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        String cause = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        List<String> details = cause == null ? List.of() : List.of(shorten(cause));
        return build(HttpStatus.BAD_REQUEST,
                "İstek gövdesi okunamadı: JSON biçimi veya alan değerlerinden biri geçersiz.", details, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "'" + ex.getName() + "' parametresi için geçersiz değer: '" + ex.getValue() + "'", List.of(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "'" + ex.getParameterName() + "' parametresi zorunludur.", List.of(), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "'" + ex.getHeaderName() + "' başlığı zorunludur.", List.of(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "Bu adres " + ex.getMethod() + " metodunu desteklemiyor.", List.of(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Desteklenmeyen içerik tipi; 'application/json' kullanın.", List.of(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "İstenen adres bulunamadı: /" + ex.getResourcePath(), List.of(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        String message = switch (ex) {
            case DisabledException disabled -> "Kullanıcı hesabı pasif durumda; lütfen yöneticinize başvurun.";
            case BadCredentialsException badCredentials -> "Kullanıcı adı veya parola hatalı.";
            case InsufficientAuthenticationException insufficient ->
                    "Bu kaynağa erişmek için HTTP Basic kimlik doğrulaması gerekiyor.";
            default -> "Kimlik doğrulaması başarısız.";
        };
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, BASIC_CHALLENGE);
        return build(HttpStatus.UNAUTHORIZED, message, List.of(), request, headers);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN,
                "Bu işlem için yetkiniz yok (rolünüz bu uç noktaya erişemez).", List.of(), request);
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiError> handleOptimisticLock(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "Kayıt eşzamanlı olarak başka bir işlem tarafından değiştirildi; lütfen güncel veriyle tekrar deneyin.",
                List.of(), request);
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, PessimisticLockException.class,
            LockTimeoutException.class})
    public ResponseEntity<ApiError> handlePessimisticLock(RuntimeException ex, HttpServletRequest request) {
        log.warn("Kilit beklenirken zaman aşımı/çakışma: {} {}", request.getMethod(), request.getRequestURI());
        return build(HttpStatus.CONFLICT,
                "Kayıt şu anda başka bir işlem tarafından kullanılıyor; lütfen birkaç saniye sonra tekrar deneyin.",
                List.of(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Veri bütünlüğü ihlali: {}", NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
        return build(HttpStatus.CONFLICT,
                "İşlem veri bütünlüğü kuralını ihlal ediyor (tekrarlanan benzersiz değer veya ilişkili kayıt).",
                List.of(), request);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public void handleAsyncTimeout() {
        // SSE bağlantısı zaman aşımına uğradı; istemci yeniden bağlanır. Akış zaten başladığı için gövde yazılmaz.
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Beklenmeyen hata: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Beklenmeyen bir hata oluştu; lütfen daha sonra tekrar deneyin.", List.of(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details,
                                           HttpServletRequest request) {
        return build(status, message, details, request, new HttpHeaders());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details,
                                           HttpServletRequest request, HttpHeaders headers) {
        // İçerik tipi sabitlenir; SSE gibi farklı Accept başlığı taşıyan isteklerde de hata JSON döner.
        headers.setContentType(MediaType.APPLICATION_JSON);
        ApiError body = new ApiError(Instant.now(clock), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), details);
        return new ResponseEntity<>(body, headers, status);
    }

    private static String shorten(String text) {
        String firstLine = text.lines().findFirst().orElse(text);
        return firstLine.length() > MAX_DETAIL_LENGTH ? firstLine.substring(0, MAX_DETAIL_LENGTH) + "…" : firstLine;
    }
}
