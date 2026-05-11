package com.demo.gateway.service;

import com.demo.gateway.config.MigrationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MigrationState {
    private final AtomicBoolean ordersEnabled;
    private final AtomicBoolean inventoryEnabled;

    public MigrationState(MigrationProperties properties) {
        this.ordersEnabled = new AtomicBoolean(properties.getOrders().isEnabled());
        this.inventoryEnabled = new AtomicBoolean(properties.getInventory().isEnabled());
    }

    public boolean isOrdersEnabled() {
        return ordersEnabled.get();
    }

    public boolean isInventoryEnabled() {
        return inventoryEnabled.get();
    }

    public void enableOrders() {
        ordersEnabled.set(true);
    }

    public void disableOrders() {
        ordersEnabled.set(false);
    }

    public void enableInventory() {
        inventoryEnabled.set(true);
    }

    public void disableInventory() {
        inventoryEnabled.set(false);
    }

    public Map<String, Boolean> status() {
        return Map.of(
                "ordersEnabled", isOrdersEnabled(),
                "inventoryEnabled", isInventoryEnabled()
        );
    }
}
