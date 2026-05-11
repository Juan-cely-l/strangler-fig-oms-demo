package com.demo.inventory.listener;

import com.demo.inventory.config.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsQueueService {
    private final SqsClient sqsClient;
    private final SqsProperties properties;
    private volatile String queueUrl;

    public String queueUrl() {
        if (queueUrl == null) {
            synchronized (this) {
                if (queueUrl == null) {
                    queueUrl = resolveQueueUrl();
                }
            }
        }
        return queueUrl;
    }

    private String resolveQueueUrl() {
        try {
            return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(properties.getQueueName())
                    .build()).queueUrl();
        } catch (QueueDoesNotExistException ex) {
            if (!properties.isAutoCreateQueue()) {
                throw ex;
            }
            log.info("[SQS] creating queue={}", properties.getQueueName());
            return sqsClient.createQueue(CreateQueueRequest.builder()
                    .queueName(properties.getQueueName())
                    .build()).queueUrl();
        }
    }
}
