package com.demo.orders.event;

import com.demo.common.event.InventoryRejectedEvent;
import com.demo.common.event.InventoryReservedEvent;
import com.demo.orders.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sqs.result-listener-enabled", havingValue = "true", matchIfMissing = true)
public class InventoryResultListener {
    private final SqsClient sqsClient;
    private final InventoryResultQueueService queueService;
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.sqs.result-poll-interval-ms:2000}")
    public void poll() {
        try {
            var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueService.queueUrl())
                    .maxNumberOfMessages(5)
                    .waitTimeSeconds(1)
                    .visibilityTimeout(10)
                    .messageAttributeNames("All")
                    .build());
            for (Message message : response.messages()) {
                handle(message);
            }
        } catch (RuntimeException ex) {
            log.warn("[SQS] inventory result polling failed reason={}", ex.getMessage());
        }
    }

    void handle(Message message) {
        try {
            String eventType = message.messageAttributes().getOrDefault("eventType", null) == null
                    ? ""
                    : message.messageAttributes().get("eventType").stringValue();
            if ("InventoryReservedEvent".equals(eventType)) {
                orderService.applyInventoryReserved(objectMapper.readValue(message.body(), InventoryReservedEvent.class));
            } else if ("InventoryRejectedEvent".equals(eventType)) {
                orderService.applyInventoryRejected(objectMapper.readValue(message.body(), InventoryRejectedEvent.class));
            } else {
                log.warn("[SQS] unknown inventory result messageId={} eventType={}", message.messageId(), eventType);
            }
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueService.queueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception ex) {
            log.warn("[SQS] inventory result processing failed messageId={} reason={}", message.messageId(), ex.getMessage());
        }
    }
}
