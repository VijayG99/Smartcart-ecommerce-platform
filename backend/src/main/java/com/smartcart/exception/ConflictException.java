package com.smartcart.exception;

/** Base type for business-rule conflicts that map to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
