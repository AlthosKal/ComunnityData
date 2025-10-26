package com.senasoft.comunidataapi.chat.enums;

import org.springframework.http.HttpStatus;

public enum ApiError {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "The are attributes with wrong values"),
    BAD_FORMAT(HttpStatus.BAD_REQUEST, "The message not have a correct form"),
    FILES_NOT_FOUND(HttpStatus.NOT_FOUND, "Files not found"),
    AI_PROVIDER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI provider timeout"),
    AI_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI provider unavailable"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error processing request");

    private final HttpStatus httpStatus;
    private final String message;

    ApiError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
