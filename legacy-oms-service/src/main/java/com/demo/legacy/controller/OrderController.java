package com.demo.legacy.controller;

import com.demo.common.dto.CreateOrderRequest;
import com.demo.common.dto.OrderResponse;
import com.demo.common.util.CorrelationConstants;
import com.demo.common.util.CorrelationIds;
import com.demo.legacy.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader(value = CorrelationConstants.HEADER_NAME, required = false) String correlationId) {
        return orderService.createOrder(request, resolve(correlationId));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable("id") Long id) {
        return orderService.findById(id);
    }

    private String resolve(String headerValue) {
        String correlationId = CorrelationIds.resolve(headerValue);
        MDC.put("correlationId", correlationId);
        return correlationId;
    }
}
