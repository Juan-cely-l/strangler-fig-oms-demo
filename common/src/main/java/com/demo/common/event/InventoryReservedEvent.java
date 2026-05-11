package com.demo.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent {
    private String eventId;
    private Long orderId;
    private String correlationId;
    private Instant orderCreatedAt;
    private Instant timestamp;
    private String result;
}
