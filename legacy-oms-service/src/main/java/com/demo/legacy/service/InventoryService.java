package com.demo.legacy.service;

import com.demo.common.dto.InventoryResponse;
import com.demo.common.dto.OrderItemRequest;
import com.demo.legacy.entity.InventoryItem;
import com.demo.legacy.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public InventoryResponse findBySku(String sku) {
        InventoryItem item = inventoryRepository.findById(sku)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "SKU not found: " + sku));
        return toResponse(item);
    }

    public boolean canReserve(List<OrderItemRequest> items) {
        return items.stream().allMatch(item -> inventoryRepository.findById(item.getSku())
                .map(inventory -> inventory.getAvailableQuantity() >= item.getQuantity())
                .orElse(false));
    }

    public void reserve(List<OrderItemRequest> items) {
        for (OrderItemRequest item : items) {
            InventoryItem inventory = inventoryRepository.findWithLockBySku(item.getSku())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "SKU not found: " + item.getSku()));
            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for SKU " + item.getSku());
            }
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
            inventory.setUpdatedAt(Instant.now());
            inventoryRepository.save(inventory);
        }
    }

    private InventoryResponse toResponse(InventoryItem item) {
        return InventoryResponse.builder()
                .sku(item.getSku())
                .availableQuantity(item.getAvailableQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .backend("LEGACY")
                .build();
    }
}
