package com.demo.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.backends")
public class BackendProperties {
    private String legacyUrl = "http://localhost:8081";
    private String modernOrderUrl = "http://localhost:8082";
    private String inventoryUrl = "http://localhost:8083";
}
