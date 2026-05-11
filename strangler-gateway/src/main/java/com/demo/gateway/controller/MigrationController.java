package com.demo.gateway.controller;

import com.demo.gateway.service.MigrationState;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/migration")
@RequiredArgsConstructor
public class MigrationController {
    private final MigrationState migrationState;

    @PostMapping("/orders/enable")
    public Map<String, Boolean> enableOrders() {
        migrationState.enableOrders();
        return migrationState.status();
    }

    @PostMapping("/orders/disable")
    public Map<String, Boolean> disableOrders() {
        migrationState.disableOrders();
        return migrationState.status();
    }

    @PostMapping("/inventory/enable")
    public Map<String, Boolean> enableInventory() {
        migrationState.enableInventory();
        return migrationState.status();
    }

    @PostMapping("/inventory/disable")
    public Map<String, Boolean> disableInventory() {
        migrationState.disableInventory();
        return migrationState.status();
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return migrationState.status();
    }
}
