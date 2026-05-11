package com.demo.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String customerId;
    private String status;
    private Instant createdAt;
    private String backend;

    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();
}
