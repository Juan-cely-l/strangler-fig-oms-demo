package com.demo.legacy.controller;

import com.demo.common.dto.InventoryResponse;
import com.demo.legacy.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/{sku}")
    public InventoryResponse getInventory(@PathVariable("sku") String sku) {
        return inventoryService.findBySku(sku);
    }
}
