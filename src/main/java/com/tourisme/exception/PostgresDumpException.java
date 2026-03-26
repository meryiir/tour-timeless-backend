package com.tourisme.exception;

/**
 * Thrown when PostgreSQL data export via {@code pg_dump} cannot run (missing binary, wrong URL, process failure).
 */
public class PostgresDumpException extends RuntimeException {

    public PostgresDumpException(String message) {
        super(message);
    }

    public PostgresDumpException(String message, Throwable cause) {
        super(message, cause);
    }
}
