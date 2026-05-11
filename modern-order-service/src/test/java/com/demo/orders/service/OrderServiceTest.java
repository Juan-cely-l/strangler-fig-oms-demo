package com.demo.orders.service;

import com.demo.common.dto.CreateOrderRequest;
import com.demo.common.dto.OrderItemRequest;
import com.demo.common.dto.OrderResponse;
import com.demo.common.event.InventoryRejectedEvent;
import com.demo.common.event.InventoryReservedEvent;
import com.demo.orders.outbox.OutboxStatus;
import com.demo.orders.repository.OutboxEventRepository;
import com.demo.orders.repository.ProcessedInventoryEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.outbox.publisher-enabled=false",
        "app.sqs.result-listener-enabled=false"
})
class OrderServiceTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedInventoryEventRepository processedInventoryEventRepository;

    @Test
    void createsOrderPendingAndStoresOutboxEvent() {
        OrderResponse response = orderService.createOrder(validRequest("CUST-MODERN"), "modern-correlation");

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getBackend()).isEqualTo("MODERN_ORDER");
        assertThat(outboxEventRepository.findAll())
                .anySatisfy(event -> {
                    assertThat(event.getStatus()).isEqualTo(OutboxStatus.NEW);
                    assertThat(event.getPayload()).contains("\"orderId\":" + response.getId());
                });
    }

    @Test
    void appliesInventoryReservedEventAndIgnoresDuplicate() {
        OrderResponse created = orderService.createOrder(validRequest("CUST-RESERVED"), "reserved-correlation");
        String eventId = UUID.randomUUID().toString();
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(eventId)
                .orderId(created.getId())
                .correlationId("reserved-correlation")
                .orderCreatedAt(created.getCreatedAt())
                .timestamp(Instant.now())
                .result("RESERVED")
                .build();

        OrderResponse first = orderService.applyInventoryReserved(event);
        OrderResponse duplicate = orderService.applyInventoryReserved(event);

        assertThat(first.getStatus()).isEqualTo("CONFIRMED");
        assertThat(duplicate.getStatus()).isEqualTo("CONFIRMED");
        assertThat(processedInventoryEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void appliesInventoryRejectedEventAndIgnoresDuplicate() {
        OrderResponse created = orderService.createOrder(validRequest("CUST-REJECTED"), "rejected-correlation");
        String eventId = UUID.randomUUID().toString();
        InventoryRejectedEvent event = InventoryRejectedEvent.builder()
                .eventId(eventId)
                .orderId(created.getId())
                .correlationId("rejected-correlation")
                .orderCreatedAt(created.getCreatedAt())
                .timestamp(Instant.now())
                .reason("Insufficient stock")
                .build();

        OrderResponse first = orderService.applyInventoryRejected(event);
        OrderResponse duplicate = orderService.applyInventoryRejected(event);

        assertThat(first.getStatus()).isEqualTo("REJECTED");
        assertThat(duplicate.getStatus()).isEqualTo("REJECTED");
        assertThat(processedInventoryEventRepository.existsById(eventId)).isTrue();
    }

    private CreateOrderRequest validRequest(String customerId) {
        return CreateOrderRequest.builder()
                .customerId(customerId)
                .items(List.of(OrderItemRequest.builder()
                        .sku("MOUSE-001")
                        .quantity(3)
                        .unitPrice(new BigDecimal("25.00"))
                        .build()))
                .build();
    }
}
