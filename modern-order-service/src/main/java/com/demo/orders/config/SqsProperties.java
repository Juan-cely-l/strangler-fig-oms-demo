package com.demo.orders.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.sqs")
public class SqsProperties {
    private String endpoint = "http://localhost:4566";
    private String region = "us-east-1";
    private String queueName = "order-created-queue";
    private String inventoryResultQueueName = "inventory-result-queue";
    private boolean autoCreateQueue = true;
    private boolean resultListenerEnabled = true;
    private long resultPollIntervalMs = 2000;
}
