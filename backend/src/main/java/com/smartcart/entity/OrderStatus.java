package com.smartcart.entity;

public enum OrderStatus {
    PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED;

    /** Allowed state machine transitions. */
    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == PROCESSING || next == CANCELLED;
            case PROCESSING -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
