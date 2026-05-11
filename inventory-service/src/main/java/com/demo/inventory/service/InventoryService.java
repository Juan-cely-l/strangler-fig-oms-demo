package com.demo.inventory.service;

import com.demo.common.dto.InventoryResponse;
import com.demo.common.event.InventoryRejectedEvent;
import com.demo.common.event.InventoryReservedEvent;
import com.demo.common.event.OrderCreatedEvent;
import com.demo.common.event.OrderCreatedItem;
import com.demo.common.util.CorrelationIds;
import com.demo.inventory.entity.InventoryItem;
import com.demo.inventory.entity.ProcessedEvent;
import com.demo.inventory.repository.InventoryRepository;
import com.demo.inventory.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional(readOnly = true)
    public InventoryResponse findBySku(String sku) {
        InventoryItem item = inventoryRepository.findById(sku)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "SKU not found: " + sku));
        return InventoryResponse.builder()
                .sku(item.getSku())
                .availableQuantity(item.getAvailableQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .backend("INVENTORY_SERVICE")
                .build();
    }

    @Transactional
    public InventoryProcessingResult processOrderCreated(OrderCreatedEvent event) {
        validate(event);
        String correlationId = CorrelationIds.resolve(event.getCorrelationId());
        var processedEvent = processedEventRepository.findById(event.getEventId());
        if (processedEvent.isPresent()) {
            ProcessedEvent processed = processedEvent.get();
            Instant observedAt = Instant.now();
            log.info("[INVENTORY] eventId={} orderId={} correlationId={} result=DUPLICATE outcome={} orderCreatedAt={} inventoryProcessedAt={} eventProcessingLatencyMs={} convergenceLatencyMs={}",
                    event.getEventId(), event.getOrderId(), correlationId, processed.getResult(), event.getTimestamp(), observedAt,
                    eventLatencyMs(event, observedAt), eventLatencyMs(event, observedAt));
            return InventoryProcessingResult.builder()
                    .eventId(event.getEventId())
                    .orderId(event.getOrderId())
                    .correlationId(correlationId)
                    .result("DUPLICATE")
                    .outcome(processed.getResult())
                    .message("Event already processed")
                    .orderCreatedAt(event.getTimestamp())
                    .processedAt(processed.getProcessedAt())
                    .duplicate(true)
                    .build();
        }

        Map<String, Integer> requestedQuantities = requestedQuantities(event.getItems());
        Map<String, InventoryItem> inventoryItems = lockInventory(requestedQuantities);
        String rejectionReason = rejectionReason(requestedQuantities, inventoryItems);

        if (rejectionReason != null) {
            Instant processedAt = Instant.now();
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .orderId(event.getOrderId())
                    .correlationId(correlationId)
                    .result("REJECTED")
                    .processedAt(processedAt)
                    .build());
            InventoryRejectedEvent rejectedEvent = InventoryRejectedEvent.builder()
                    .eventId(event.getEventId())
                    .orderId(event.getOrderId())
                    .correlationId(correlationId)
                    .orderCreatedAt(event.getTimestamp())
                    .timestamp(processedAt)
                    .reason(rejectionReason)
                    .build();
            log.info("[INVENTORY] eventId={} orderId={} correlationId={} result={} reason={} orderCreatedAt={} inventoryProcessedAt={} eventProcessingLatencyMs={} convergenceLatencyMs={}",
                    rejectedEvent.getEventId(), rejectedEvent.getOrderId(), rejectedEvent.getCorrelationId(),
                    "InventoryRejectedEvent", rejectionReason, event.getTimestamp(), processedAt,
                    eventLatencyMs(event, processedAt), eventLatencyMs(event, processedAt));
            return InventoryProcessingResult.builder()
                    .eventId(event.getEventId())
                    .orderId(event.getOrderId())
                    .correlationId(correlationId)
                    .result("REJECTED")
                    .outcome("REJECTED")
                    .message(rejectionReason)
                    .orderCreatedAt(event.getTimestamp())
                    .processedAt(processedAt)
                    .build();
        }

        requestedQuantities.forEach((sku, quantity) -> {
            InventoryItem item = inventoryItems.get(sku);
            item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
            item.setReservedQuantity(item.getReservedQuantity() + quantity);
            item.setUpdatedAt(Instant.now());
            inventoryRepository.save(item);
        });
        Instant processedAt = Instant.now();
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.getEventId())
                .orderId(event.getOrderId())
                .correlationId(correlationId)
                .result("RESERVED")
                .processedAt(processedAt)
                .build());
        InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                .eventId(event.getEventId())
                .orderId(event.getOrderId())
                .correlationId(correlationId)
                .orderCreatedAt(event.getTimestamp())
                .timestamp(processedAt)
                .result("RESERVED")
                .build();
        log.info("[INVENTORY] eventId={} orderId={} correlationId={} result={} orderCreatedAt={} inventoryProcessedAt={} eventProcessingLatencyMs={} convergenceLatencyMs={}",
                reservedEvent.getEventId(), reservedEvent.getOrderId(), reservedEvent.getCorrelationId(),
                "InventoryReservedEvent", event.getTimestamp(), processedAt,
                eventLatencyMs(event, processedAt), eventLatencyMs(event, processedAt));
        return InventoryProcessingResult.builder()
                .eventId(event.getEventId())
                .orderId(event.getOrderId())
                .correlationId(correlationId)
                .result("RESERVED")
                .outcome("RESERVED")
                .message("Inventory reserved")
                .orderCreatedAt(event.getTimestamp())
                .processedAt(processedAt)
                .build();
    }

    private void validate(OrderCreatedEvent event) {
        if (event == null || event.getEventId() == null || event.getEventId().isBlank()
                || event.getOrderId() == null || event.getItems() == null || event.getItems().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "eventId, orderId and items are required");
        }
        event.getItems().forEach(item -> {
            if (item.getSku() == null || item.getSku().isBlank() || item.getQuantity() <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "Each event item requires sku and quantity > 0");
            }
        });
    }

    private Map<String, Integer> requestedQuantities(List<OrderCreatedItem> items) {
        return items.stream()
                .collect(groupingBy(OrderCreatedItem::getSku, LinkedHashMap::new, summingInt(OrderCreatedItem::getQuantity)));
    }

    private Map<String, InventoryItem> lockInventory(Map<String, Integer> requestedQuantities) {
        Map<String, InventoryItem> items = new LinkedHashMap<>();
        requestedQuantities.keySet().forEach(sku ->
                inventoryRepository.findWithLockBySku(sku).ifPresent(item -> items.put(sku, item)));
        return items;
    }

    private String rejectionReason(Map<String, Integer> requestedQuantities, Map<String, InventoryItem> inventoryItems) {
        for (Map.Entry<String, Integer> request : requestedQuantities.entrySet()) {
            InventoryItem item = inventoryItems.get(request.getKey());
            if (item == null) {
                return "SKU not found: " + request.getKey();
            }
            if (item.getAvailableQuantity() < request.getValue()) {
                return "Insufficient stock for SKU " + request.getKey();
            }
        }
        return null;
    }

    private long eventLatencyMs(OrderCreatedEvent event, Instant observedAt) {
        if (event.getTimestamp() == null) {
            return 0;
        }
        return Duration.between(event.getTimestamp(), observedAt).toMillis();
    }
}
