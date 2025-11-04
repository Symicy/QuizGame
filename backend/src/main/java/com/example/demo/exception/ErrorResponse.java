package com.example.demo.exception;

import java.time.Instant;

public record ErrorResponse(int status, String message, String path, Instant timestamp) {
    public static ErrorResponse of(int status, String message, String path) {
        return new ErrorResponse(status, message, path, Instant.now());
    }
}
