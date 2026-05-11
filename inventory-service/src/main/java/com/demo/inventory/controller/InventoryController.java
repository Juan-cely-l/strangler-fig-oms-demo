package com.demo.inventory.controller;

import com.demo.common.dto.InventoryResponse;
import com.demo.common.dto.ReserveInventoryRequest;
import com.demo.common.event.OrderCreatedEvent;
import com.demo.common.event.OrderCreatedItem;
import com.demo.common.util.CorrelationConstants;
import com.demo.common.util.CorrelationIds;
import com.demo.inventory.service.InventoryProcessingResult;
import com.demo.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/{sku}")
    public InventoryResponse getInventory(@PathVariable("sku") String sku) {
        return inventoryService.findBySku(sku);
    }

    @PostMapping("/reserve")
    public InventoryProcessingResult reserve(
            @RequestBody ReserveInventoryRequest request,
            @RequestHeader(value = CorrelationConstants.HEADER_NAME, required = false) String correlationId) {
        String resolved = resolve(correlationId);
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .correlationId(resolved)
                .timestamp(Instant.now())
                .items(request.getItems().stream()
                        .map(item -> OrderCreatedItem.builder()
                                .sku(item.getSku())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .toList())
                .build();
        return inventoryService.processOrderCreated(event);
    }

    private String resolve(String headerValue) {
        String correlationId = CorrelationIds.resolve(headerValue);
        MDC.put("correlationId", correlationId);
        return correlationId;
    }
}
