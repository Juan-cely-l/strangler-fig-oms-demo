package com.demo.legacy.service;

import com.demo.common.dto.CreateOrderRequest;
import com.demo.common.dto.OrderItemRequest;
import com.demo.common.dto.OrderItemResponse;
import com.demo.common.dto.OrderResponse;
import com.demo.common.event.OrderCreatedEvent;
import com.demo.common.event.OrderCreatedItem;
import com.demo.legacy.entity.Order;
import com.demo.legacy.entity.OrderItem;
import com.demo.legacy.entity.OrderStatus;
import com.demo.legacy.entity.OutboxEvent;
import com.demo.legacy.payment.PaymentStubService;
import com.demo.legacy.repository.OrderRepository;
import com.demo.legacy.repository.OutboxRepository;
import com.demo.legacy.shipping.ShippingStubService;
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
    private final OutboxRepository outboxRepository;
    private final InventoryService inventoryService;
    private final PaymentStubService paymentStubService;
    private final ShippingStubService shippingStubService;
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

        if (!inventoryService.canReserve(request.getItems())) {
            order.setStatus(OrderStatus.INVENTORY_REJECTED);
            Order saved = orderRepository.save(order);
            saveOutbox(saved, correlationId);
            log.info("[LEGACY] orderId={} correlationId={} result=INVENTORY_REJECTED latencyMs={}",
                    saved.getId(), correlationId, elapsedMs(start));
            return toResponse(saved);
        }

        inventoryService.reserve(request.getItems());
        order.setStatus(OrderStatus.INVENTORY_RESERVED);
        paymentStubService.authorize(order.getCustomerId(), correlationId);
        shippingStubService.prepareShipment(correlationId);
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        saveOutbox(saved, correlationId);

        log.info("[LEGACY] orderId={} correlationId={} result=CONFIRMED latencyMs={}",
                saved.getId(), correlationId, elapsedMs(start));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found: " + id));
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

    private void saveOutbox(Order order, String correlationId) {
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .items(toEventItems(order.getItems()))
                .build();
        outboxRepository.save(OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("Order")
                .aggregateId(String.valueOf(order.getId()))
                .eventType("LegacyOrderCreated")
                .payload(toJson(event))
                .correlationId(correlationId)
                .createdAt(Instant.now())
                .build());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize outbox event", ex);
        }
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

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .backend("LEGACY")
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
}
