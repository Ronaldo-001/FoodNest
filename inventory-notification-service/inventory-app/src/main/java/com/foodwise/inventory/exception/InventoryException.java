package com.foodwise.inventory.exception;

import org.springframework.http.HttpStatus;

public class InventoryException extends RuntimeException {

    private final HttpStatus status;

    public InventoryException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
