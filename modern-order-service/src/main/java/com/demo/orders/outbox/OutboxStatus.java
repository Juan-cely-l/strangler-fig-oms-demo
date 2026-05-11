package com.demo.orders.outbox;

public enum OutboxStatus {
    NEW,
    PUBLISHED,
    FAILED
}
