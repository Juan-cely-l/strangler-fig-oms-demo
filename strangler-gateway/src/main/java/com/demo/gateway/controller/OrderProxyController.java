package com.demo.gateway.controller;

import com.demo.common.dto.CreateOrderRequest;
import com.demo.common.util.CorrelationConstants;
import com.demo.common.util.CorrelationIds;
import com.demo.gateway.service.RoutingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderProxyController {
    private final RoutingService routingService;

    @PostMapping("/orders")
    public ResponseEntity<String> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader(value = CorrelationConstants.HEADER_NAME, required = false) String correlationId) {
        String resolved = resolve(correlationId);
        return withCorrelation(routingService.createOrder(request, resolved), resolved);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<String> getOrder(
            @PathVariable("id") Long id,
            @RequestHeader(value = CorrelationConstants.HEADER_NAME, required = false) String correlationId) {
        String resolved = resolve(correlationId);
        return withCorrelation(routingService.getOrder(id, resolved), resolved);
    }

    private String resolve(String headerValue) {
        String correlationId = CorrelationIds.resolve(headerValue);
        MDC.put("correlationId", correlationId);
        return correlationId;
    }

    private ResponseEntity<String> withCorrelation(ResponseEntity<String> response, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(response.getHeaders());
        headers.set(CorrelationConstants.HEADER_NAME, correlationId);

        return ResponseEntity.status(response.getStatusCode())
                .headers(headers)
                .body(response.getBody());
    }
}
