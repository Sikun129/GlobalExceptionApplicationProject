package com.example.exceptions;

import java.time.Instant;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

// Attach common metadata to custom and framework ProblemDetail responses alike.
@RestControllerAdvice
public class ErrorResponseMetadata implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body instanceof ProblemDetail problem && request instanceof ServletServerHttpRequest servletRequest) {
            problem.setProperty("timestamp", Instant.now().toString());
            problem.setProperty("requestId", servletRequest.getServletRequest().getAttribute(RequestIdFilter.ATTRIBUTE));
        }
        return body;
    }
}
