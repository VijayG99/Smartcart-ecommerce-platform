package com.smartcart.exception;

public class InvalidOrderStateException extends ConflictException {
    public InvalidOrderStateException(String message) { super(message); }
}
