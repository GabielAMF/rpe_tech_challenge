package com.rpe.cardprocessor.controller;

import com.rpe.cardprocessor.exception.CustomException;
import com.rpe.cardprocessor.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps exceptions to RFC 9457 problem details. Every error response has the same shape:
 * {@code status}, {@code title}, {@code detail}, {@code instance}, plus a machine-readable {@code code}
 * and a {@code timestamp}. Spring MVC's own exceptions (malformed JSON, type mismatches, ...) are
 * handled by the parent class and get the same extra fields in {@link #handleExceptionInternal}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ProblemDetail handleCustomException(CustomException ex) {
        log.warn("{}: {}", ex.getErrorCode(), ex.getMessage());
        ProblemDetail problem = problem(ex.getErrorCode(), ex.getMessage());
        ex.getProperties().forEach(problem::setProperty);
        return problem;
    }

    // Copied from the other services so every service answers the same way. The database message is not
    // logged: it contains the conflicting value, which may be sensitive.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation ({})", ex.getMostSpecificCause().getClass().getSimpleName());
        return problem(ErrorCode.DATA_CONFLICT, "The request conflicts with existing data");
    }

    // Anything unexpected: log the stack trace, but don't leak internals to the client.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = problem(ErrorCode.VALIDATION_ERROR, "Invalid request body");
        problem.setProperty("errors", errors);
        log.warn("Rejected request with invalid fields: {}", errors.keySet());
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception ex,
                                                             @Nullable Object body,
                                                             @NonNull HttpHeaders headers,
                                                             @NonNull HttpStatusCode statusCode,
                                                             @NonNull WebRequest request) {
        // Framework errors have no ErrorCode; use the HTTP status name (e.g. BAD_REQUEST) as the code.
        if (body instanceof ProblemDetail problem && (problem.getProperties() == null || !problem.getProperties().containsKey("code"))) {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            problem.setProperty("code", status != null ? status.name() : String.valueOf(statusCode.value()));
            problem.setProperty("timestamp", Instant.now());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static ProblemDetail problem(ErrorCode errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), detail);
        problem.setProperty("code", errorCode.name());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
