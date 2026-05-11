package com.demo.gateway.controller;

import com.demo.common.util.CorrelationConstants;
import com.demo.common.util.CorrelationIds;
import com.demo.gateway.service.RoutingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InventoryProxyController {
    private final RoutingService routingService;

    @GetMapping("/inventory/{sku}")
    public ResponseEntity<String> getInventory(
            @PathVariable("sku") String sku,
            @RequestHeader(value = CorrelationConstants.HEADER_NAME, required = false) String correlationId) {
        String resolved = resolve(correlationId);
        ResponseEntity<String> response = routingService.getInventory(sku, resolved);
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(response.getHeaders());
        headers.set(CorrelationConstants.HEADER_NAME, resolved);

        return ResponseEntity.status(response.getStatusCode())
                .headers(headers)
                .body(response.getBody());
    }

    private String resolve(String headerValue) {
        String correlationId = CorrelationIds.resolve(headerValue);
        MDC.put("correlationId", correlationId);
        return correlationId;
    }
}
