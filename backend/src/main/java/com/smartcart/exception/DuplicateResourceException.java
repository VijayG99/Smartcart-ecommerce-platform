package com.smartcart.exception;

public class DuplicateResourceException extends ConflictException {
    public DuplicateResourceException(String message) { super(message); }
}
