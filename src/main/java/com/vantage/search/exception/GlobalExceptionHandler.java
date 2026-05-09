package com.vantage.search.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
// ServiceUnavailableException is in the same package; no import needed.
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/**
 * Centralised exception handling for the API.
 * Standardises error responses to ensure a predictable schema for API consumers
 * and sanitises output to prevent internal stack trace leakage.
 *
 * <p>Logging discipline: never log raw exception messages from validation or
 * database layers — Postgres and Spring's BindingResult include the offending
 * value in the text and we don't want PII bleeding into the log pipeline.
 * Stack traces stay at ERROR for genuine system failures.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID_KEY = "traceId";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        String traceId = MDC.get(TRACE_ID_KEY);

        log.warn("RESOURCE_NOT_FOUND | traceId={} | message={}", traceId, ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://api.vantage.com/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);

        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequests(IllegalArgumentException ex) {
        String traceId = MDC.get(TRACE_ID_KEY);

        log.warn("INVALID_REQUEST | traceId={}", traceId);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Invalid Request");
        problemDetail.setType(URI.create("https://api.vantage.com/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        String traceId = MDC.get(TRACE_ID_KEY);

        List<String> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField())
                .distinct()
                .toList();
        log.warn("VALIDATION_ERROR | traceId={} | fields={}", traceId, fields);

        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.vantage.com/errors/bad-request"));
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);

        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String traceId = MDC.get(TRACE_ID_KEY);

        List<String> paths = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath().toString())
                .distinct()
                .toList();
        log.warn("CONSTRAINT_VIOLATION | traceId={} | paths={}", traceId, paths);

        List<String> errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.vantage.com/errors/bad-request"));
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);

        return problemDetail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String traceId = MDC.get(TRACE_ID_KEY);
        String sqlState = sqlStateOf(ex);

        // Only Postgres SQLState 23505 (unique_violation) is a client-resolvable conflict.
        // NOT NULL / FK / check violations are bugs in our code or schema and surface as 500.
        if (!"23505".equals(sqlState)) {
            log.error("DATABASE_INTEGRITY_FAILURE | traceId={} | sqlState={}", traceId, sqlState, ex);
            ProblemDetail serverError = ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected internal system error occurred. Please provide the traceId to support.");
            serverError.setTitle("Internal Server Error");
            serverError.setType(URI.create("https://api.vantage.com/errors/internal-error"));
            serverError.setProperty("timestamp", Instant.now());
            serverError.setProperty("traceId", traceId);
            return serverError;
        }

        log.warn("UNIQUE_CONSTRAINT_VIOLATION | traceId={} | sqlState={}", traceId, sqlState);

        ProblemDetail conflict = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "A unique constraint was violated.");
        conflict.setTitle("Resource Conflict");
        conflict.setType(URI.create("https://api.vantage.com/errors/conflict"));
        conflict.setProperty("timestamp", Instant.now());
        conflict.setProperty("traceId", traceId);
        return conflict;
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ProblemDetail handleServiceUnavailable(ServiceUnavailableException ex) {
        String traceId = MDC.get(TRACE_ID_KEY);
        log.error("SERVICE_UNAVAILABLE | traceId={} | cause={}",
                traceId,
                ex.getCause() == null ? "unknown" : ex.getCause().getClass().getSimpleName(),
                ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setType(URI.create("https://api.vantage.com/errors/service-unavailable"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        return problemDetail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        String traceId = MDC.get(TRACE_ID_KEY);
        log.warn("AUTH_FAILURE | traceId={}", traceId);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        problemDetail.setTitle("Unauthorized");
        problemDetail.setType(URI.create("https://api.vantage.com/errors/unauthorized"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericExceptions(Exception ex) {
        String traceId = MDC.get(TRACE_ID_KEY);

        log.error("CRITICAL_SYSTEM_ERROR | traceId={}", traceId, ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal system error occurred. Please provide the traceId to support.");

        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.vantage.com/errors/internal-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", traceId);

        return problemDetail;
    }

    private static String sqlStateOf(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof SQLException sql) {
                return sql.getSQLState();
            }
            cursor = cursor.getCause();
        }
        return null;
    }
}
