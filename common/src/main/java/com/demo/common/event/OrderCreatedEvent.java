package com.demo.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private String eventId;
    private Long orderId;
    private String customerId;
    private String correlationId;
    private Instant timestamp;

    @Builder.Default
    private List<OrderCreatedItem> items = new ArrayList<>();
}
