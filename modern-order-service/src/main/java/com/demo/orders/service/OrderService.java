package com.demo.orders.service;

import com.demo.common.dto.CreateOrderRequest;
import com.demo.common.dto.OrderItemResponse;
import com.demo.common.dto.OrderResponse;
import com.demo.common.event.InventoryRejectedEvent;
import com.demo.common.event.InventoryReservedEvent;
import com.demo.common.event.OrderCreatedEvent;
import com.demo.common.event.OrderCreatedItem;
import com.demo.orders.entity.Order;
import com.demo.orders.entity.OrderItem;
import com.demo.orders.entity.OrderStatus;
import com.demo.orders.entity.ProcessedInventoryEvent;
import com.demo.orders.outbox.OutboxEvent;
import com.demo.orders.outbox.OutboxStatus;
import com.demo.orders.repository.OrderRepository;
import com.demo.orders.repository.OutboxEventRepository;
import com.demo.orders.repository.ProcessedInventoryEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedInventoryEventRepository processedInventoryEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String correlationId) {
        validate(request);
        long start = System.nanoTime();

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .createdAt(Instant.now())
                .status(OrderStatus.PENDING)
                .build();
        request.getItems().forEach(item -> order.addItem(OrderItem.builder()
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build()));

        Order saved = orderRepository.save(order);
        outboxEventRepository.save(toOutboxEvent(saved, correlationId));

        log.info("[MODERN-ORDER] orderId={} status=PENDING correlationId={} saveAndOutboxLatencyMs={}",
                saved.getId(), correlationId, elapsedMs(start));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found: " + id));
    }

    @Transactional
    public OrderResponse applyInventoryReserved(InventoryReservedEvent event) {
        return applyInventoryResult(event.getEventId(), event.getOrderId(), event.getCorrelationId(),
                event.getOrderCreatedAt(), event.getTimestamp(), "RESERVED", OrderStatus.CONFIRMED, null);
    }

    @Transactional
    public OrderResponse applyInventoryRejected(InventoryRejectedEvent event) {
        return applyInventoryResult(event.getEventId(), event.getOrderId(), event.getCorrelationId(),
                event.getOrderCreatedAt(), event.getTimestamp(), "REJECTED", OrderStatus.REJECTED, event.getReason());
    }

    private OrderResponse applyInventoryResult(
            String eventId,
            Long orderId,
            String correlationId,
            Instant orderCreatedAt,
            Instant inventoryProcessedAt,
            String outcome,
            OrderStatus finalStatus,
            String reason) {
        validateInventoryResult(eventId, orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found: " + orderId));

        if (processedInventoryEventRepository.existsById(eventId)) {
            Instant observedAt = Instant.now();
            log.info("[MODERN-ORDER] inventoryResult eventId={} orderId={} correlationId={} outcome={} duplicate=true orderStatus={} orderCreatedAt={} inventoryProcessedAt={} orderFinalizedAt={} convergenceLatencyMs={} resultDeliveryLatencyMs={}",
                    eventId, orderId, correlationId, outcome, order.getStatus(), orderCreatedAt, inventoryProcessedAt,
                    observedAt, latencyMs(orderCreatedAt, observedAt), latencyMs(inventoryProcessedAt, observedAt));
            return toResponse(order);
        }

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus == OrderStatus.PENDING) {
            order.setStatus(finalStatus);
        }
        processedInventoryEventRepository.save(ProcessedInventoryEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .outcome(outcome)
                .processedAt(Instant.now())
                .build());

        Instant finalizedAt = Instant.now();
        log.info("[MODERN-ORDER] inventoryResult eventId={} orderId={} correlationId={} outcome={} reason={} previousStatus={} finalStatus={} orderCreatedAt={} inventoryProcessedAt={} orderFinalizedAt={} convergenceLatencyMs={} resultDeliveryLatencyMs={}",
                eventId, orderId, correlationId, outcome, reason, previousStatus, order.getStatus(),
                orderCreatedAt, inventoryProcessedAt, finalizedAt,
                latencyMs(orderCreatedAt, finalizedAt), latencyMs(inventoryProcessedAt, finalizedAt));
        return toResponse(order);
    }

    private void validateInventoryResult(String eventId, Long orderId) {
        if (eventId == null || eventId.isBlank() || orderId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "eventId and orderId are required");
        }
    }

    private void validate(CreateOrderRequest request) {
        if (request == null || request.getCustomerId() == null || request.getCustomerId().isBlank()
                || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "customerId and at least one item are required");
        }
        request.getItems().forEach(item -> {
            if (item.getSku() == null || item.getSku().isBlank() || item.getQuantity() <= 0 || item.getUnitPrice() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Each item requires sku, quantity > 0 and unitPrice");
            }
        });
    }

    private OutboxEvent toOutboxEvent(Order order, String correlationId) {
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .items(toEventItems(order.getItems()))
                .build();
        return OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("Order")
                .aggregateId(String.valueOf(order.getId()))
                .eventType("OrderCreatedEvent")
                .payload(toJson(event))
                .correlationId(correlationId)
                .createdAt(Instant.now())
                .status(OutboxStatus.NEW)
                .attempts(0)
                .build();
    }

    private List<OrderCreatedItem> toEventItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> OrderCreatedItem.builder()
                        .sku(item.getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .toList();
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize outbox event", ex);
        }
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .backend("MODERN_ORDER")
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .sku(item.getSku())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .toList())
                .build();
    }

    private long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private long latencyMs(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0;
        }
        return java.time.Duration.between(start, end).toMillis();
    }
}
