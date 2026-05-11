package com.demo.inventory.listener;

import com.demo.inventory.service.InventoryProcessingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryResultPublisherTest {
    @Mock
    private SqsClient sqsClient;

    @Mock
    private InventoryResultQueueService queueService;

    @Test
    void publishesInventoryReservedEventToResultQueue() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        InventoryResultPublisher publisher = new InventoryResultPublisher(sqsClient, queueService, objectMapper);
        when(queueService.queueUrl()).thenReturn("inventory-result-url");
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("message-1").build());

        publisher.publish(InventoryProcessingResult.builder()
                .eventId("event-1")
                .orderId(10L)
                .correlationId("corr-1")
                .outcome("RESERVED")
                .result("RESERVED")
                .orderCreatedAt(Instant.parse("2026-05-10T10:00:00Z"))
                .processedAt(Instant.parse("2026-05-10T10:00:01Z"))
                .build());

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(requestCaptor.capture());
        SendMessageRequest request = requestCaptor.getValue();

        assertThat(request.queueUrl()).isEqualTo("inventory-result-url");
        assertThat(request.messageAttributes().get("eventType").stringValue()).isEqualTo("InventoryReservedEvent");
        assertThat(request.messageBody()).contains("\"orderId\":10", "\"result\":\"RESERVED\"");
    }
}
