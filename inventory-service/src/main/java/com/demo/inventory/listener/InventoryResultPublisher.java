package com.demo.inventory.listener;

import com.demo.common.event.InventoryRejectedEvent;
import com.demo.common.event.InventoryReservedEvent;
import com.demo.inventory.service.InventoryProcessingResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryResultPublisher {
    private final SqsClient sqsClient;
    private final InventoryResultQueueService queueService;
    private final ObjectMapper objectMapper;

    public void publish(InventoryProcessingResult result) {
        if (!result.isPublishableOutcome()) {
            log.info("[SQS] inventoryResult eventId={} orderId={} outcome={} skipped=true",
                    result.getEventId(), result.getOrderId(), result.getOutcome());
            return;
        }

        String eventType = eventType(result);
        long start = System.nanoTime();
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueService.queueUrl())
                .messageBody(toJson(toEvent(result)))
                .messageAttributes(Map.of(
                        "eventId", stringAttribute(result.getEventId()),
                        "eventType", stringAttribute(eventType),
                        "correlationId", stringAttribute(result.getCorrelationId())
                ))
                .build());
        log.info("[SQS] inventoryResult eventId={} orderId={} correlationId={} eventType={} duplicate={} publishLatencyMs={}",
                result.getEventId(), result.getOrderId(), result.getCorrelationId(), eventType,
                result.isDuplicate(), elapsedMs(start));
    }

    private Object toEvent(InventoryProcessingResult result) {
        if ("RESERVED".equals(result.getOutcome())) {
            return InventoryReservedEvent.builder()
                    .eventId(result.getEventId())
                    .orderId(result.getOrderId())
                    .correlationId(result.getCorrelationId())
                    .orderCreatedAt(result.getOrderCreatedAt())
                    .timestamp(result.getProcessedAt())
                    .result("RESERVED")
                    .build();
        }
        return InventoryRejectedEvent.builder()
                .eventId(result.getEventId())
                .orderId(result.getOrderId())
                .correlationId(result.getCorrelationId())
                .orderCreatedAt(result.getOrderCreatedAt())
                .timestamp(result.getProcessedAt())
                .reason(result.getMessage())
                .build();
    }

    private String eventType(InventoryProcessingResult result) {
        return "RESERVED".equals(result.getOutcome()) ? "InventoryReservedEvent" : "InventoryRejectedEvent";
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize inventory result event", ex);
        }
    }

    private MessageAttributeValue stringAttribute(String value) {
        return MessageAttributeValue.builder().dataType("String").stringValue(value == null ? "" : value).build();
    }

    private long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
