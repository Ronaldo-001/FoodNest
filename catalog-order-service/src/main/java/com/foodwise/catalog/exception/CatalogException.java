package com.foodwise.catalog.exception;

import org.springframework.http.HttpStatus;

public class CatalogException extends RuntimeException {

    private final HttpStatus status;

    public CatalogException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
