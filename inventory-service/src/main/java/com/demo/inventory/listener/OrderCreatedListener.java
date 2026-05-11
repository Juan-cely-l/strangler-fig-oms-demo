package com.demo.inventory.listener;

import com.demo.common.event.OrderCreatedEvent;
import com.demo.inventory.service.InventoryService;
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
@ConditionalOnProperty(name = "app.sqs.listener-enabled", havingValue = "true", matchIfMissing = true)
public class OrderCreatedListener {
    private final SqsClient sqsClient;
    private final SqsQueueService sqsQueueService;
    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final InventoryResultPublisher inventoryResultPublisher;

    @Scheduled(fixedDelayString = "${app.sqs.poll-interval-ms:2000}")
    public void poll() {
        try {
            var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(sqsQueueService.queueUrl())
                    .maxNumberOfMessages(5)
                    .waitTimeSeconds(1)
                    .visibilityTimeout(10)
                    .build());
            for (Message message : response.messages()) {
                handle(message);
            }
        } catch (RuntimeException ex) {
            log.warn("[SQS] listener polling failed reason={}", ex.getMessage());
        }
    }

    private void handle(Message message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message.body(), OrderCreatedEvent.class);
            var result = inventoryService.processOrderCreated(event);
            inventoryResultPublisher.publish(result);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(sqsQueueService.queueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception ex) {
            log.warn("[SQS] message processing failed messageId={} reason={}", message.messageId(), ex.getMessage());
        }
    }
}
