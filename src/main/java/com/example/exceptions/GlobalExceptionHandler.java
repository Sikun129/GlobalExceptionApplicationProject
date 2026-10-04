package com.example.exceptions;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// One advice handles exceptions across BOTH controllers.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail notFound(ProductNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail stockConflict(InsufficientStockException ex) {
        return problem(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Please correct the highlighted fields.");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .sorted(java.util.Comparator.comparing(error -> error.get("field")))
                .toList();
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error; requestId={}",
                request.getAttribute(RequestIdFilter.ATTRIBUTE, WebRequest.SCOPE_REQUEST), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Something went wrong. Contact support with the request ID.");
    }

    // Preserve Spring MVC's correct 400/404/405/415 statuses and headers.
    // In particular, malformed JSON or an invalid ID must not become HTTP 500.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail detail = body instanceof ProblemDetail p
                && p.getProperties() != null && p.getProperties().containsKey("code") ? p
                : problem(status, "HTTP_" + status.value(),
                        status.is5xxServerError() ? "The server could not complete the request."
                                : "Check the request path, method, headers, and body.");
        if (status.is5xxServerError()) {
            log.error("Framework error; requestId={}",
                    request.getAttribute(RequestIdFilter.ATTRIBUTE, WebRequest.SCOPE_REQUEST), ex);
        }
        return super.handleExceptionInternal(ex, detail, headers, status, request);
    }

    private ProblemDetail problem(HttpStatusCode status, String code, String message) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, message);
        body.setProperty("code", code);
        return body;
    }
}
