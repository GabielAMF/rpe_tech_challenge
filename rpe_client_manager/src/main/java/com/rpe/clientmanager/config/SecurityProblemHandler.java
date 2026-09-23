package com.rpe.clientmanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpe.clientmanager.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Security errors happen in the filter chain, before any controller, so GlobalExceptionHandler never sees
 * them. This writes them in the same ProblemDetail shape (with {@code code} and {@code timestamp}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        log.warn("Unauthenticated request to {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        write(request, response, ErrorCode.UNAUTHORIZED, "A valid bearer token is required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        log.warn("Forbidden request to {} {}", request.getMethod(), request.getRequestURI());
        write(request, response, ErrorCode.FORBIDDEN, "You don't have permission to perform this action");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       ErrorCode errorCode, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", errorCode.name());
        problem.setProperty("timestamp", Instant.now());

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        if (errorCode == ErrorCode.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
