package com.demo.orders.event;

import com.demo.orders.outbox.OutboxEvent;
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
public class SqsEventPublisher implements EventPublisher {
    private final SqsClient sqsClient;
    private final SqsQueueService sqsQueueService;

    @Override
    public void publish(OutboxEvent event) {
        long start = System.nanoTime();
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(sqsQueueService.queueUrl())
                .messageBody(event.getPayload())
                .messageAttributes(Map.of(
                        "eventId", stringAttribute(event.getEventId()),
                        "eventType", stringAttribute(event.getEventType()),
                        "correlationId", stringAttribute(event.getCorrelationId())
                ))
                .build());
        log.info("[SQS] eventId={} correlationId={} publishLatencyMs={}",
                event.getEventId(), event.getCorrelationId(), elapsedMs(start));
    }

    private MessageAttributeValue stringAttribute(String value) {
        return MessageAttributeValue.builder().dataType("String").stringValue(value).build();
    }

    private long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
