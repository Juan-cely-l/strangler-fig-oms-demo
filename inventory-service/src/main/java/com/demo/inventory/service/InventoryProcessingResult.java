package com.demo.inventory.service;

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
public class InventoryProcessingResult {
    private String eventId;
    private Long orderId;
    private String correlationId;
    private String result;
    private String outcome;
    private String message;
    private Instant orderCreatedAt;
    private Instant processedAt;
    private boolean duplicate;

    public boolean isPublishableOutcome() {
        return "RESERVED".equals(outcome) || "REJECTED".equals(outcome);
    }
}
