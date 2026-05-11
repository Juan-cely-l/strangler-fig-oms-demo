package com.demo.legacy.service;

import com.demo.common.dto.CreateOrderRequest;
import com.demo.common.dto.OrderItemRequest;
import com.demo.common.dto.OrderResponse;
import com.demo.legacy.entity.InventoryItem;
import com.demo.legacy.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void createsOrderAndReservesInventoryInSameService() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId("CUST-TEST")
                .items(List.of(OrderItemRequest.builder()
                        .sku("LAPTOP-001")
                        .quantity(2)
                        .unitPrice(new BigDecimal("1200.00"))
                        .build()))
                .build();

        OrderResponse response = orderService.createOrder(request, "test-correlation");

        InventoryItem inventory = inventoryRepository.findById("LAPTOP-001").orElseThrow();
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(inventory.getAvailableQuantity()).isEqualTo(98);
        assertThat(inventory.getReservedQuantity()).isEqualTo(2);
    }
}
