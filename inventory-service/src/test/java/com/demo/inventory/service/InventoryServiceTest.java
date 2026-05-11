package com.demo.inventory.service;

import com.demo.common.event.OrderCreatedEvent;
import com.demo.common.event.OrderCreatedItem;
import com.demo.inventory.entity.InventoryItem;
import com.demo.inventory.repository.InventoryRepository;
import com.demo.inventory.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.sqs.listener-enabled=false")
class InventoryServiceTest {
    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void processesOrderCreatedEventAndUpdatesStockIdempotently() {
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(1001L)
                .customerId("CUST-INV")
                .correlationId("inventory-correlation")
                .timestamp(Instant.now())
                .items(List.of(OrderCreatedItem.builder()
                        .sku("MOUSE-001")
                        .quantity(3)
                        .unitPrice(new BigDecimal("25.00"))
                        .build()))
                .build();

        InventoryProcessingResult first = inventoryService.processOrderCreated(event);
        InventoryProcessingResult second = inventoryService.processOrderCreated(event);

        InventoryItem inventory = inventoryRepository.findById("MOUSE-001").orElseThrow();
        assertThat(first.getResult()).isEqualTo("RESERVED");
        assertThat(first.getOutcome()).isEqualTo("RESERVED");
        assertThat(first.isPublishableOutcome()).isTrue();
        assertThat(second.getResult()).isEqualTo("DUPLICATE");
        assertThat(second.getOutcome()).isEqualTo("RESERVED");
        assertThat(second.isDuplicate()).isTrue();
        assertThat(inventory.getAvailableQuantity()).isEqualTo(497);
        assertThat(inventory.getReservedQuantity()).isEqualTo(3);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }
}
