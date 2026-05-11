package com.demo.orders.event;

import com.demo.orders.outbox.OutboxEvent;

public interface EventPublisher {
    void publish(OutboxEvent event);
}
