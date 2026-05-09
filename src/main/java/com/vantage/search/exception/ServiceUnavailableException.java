package com.vantage.search.exception;

/**
 * Thrown when an upstream dependency that materially affects the response is
 * unavailable and the API cannot continue with the request safely. Translates
 * to HTTP 503 in the global exception handler.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
