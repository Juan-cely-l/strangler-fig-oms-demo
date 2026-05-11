package com.demo.legacy;

import com.demo.legacy.entity.InventoryItem;
import com.demo.legacy.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

@SpringBootApplication
public class LegacyOmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegacyOmsApplication.class, args);
    }

    @Bean
    CommandLineRunner seedLegacyInventory(InventoryRepository inventoryRepository) {
        return args -> {
            seed(inventoryRepository, "LAPTOP-001", 100);
            seed(inventoryRepository, "MOUSE-001", 500);
            seed(inventoryRepository, "KEYBOARD-001", 200);
        };
    }

    private void seed(InventoryRepository inventoryRepository, String sku, int quantity) {
        if (!inventoryRepository.existsById(sku)) {
            inventoryRepository.save(InventoryItem.builder()
                    .sku(sku)
                    .availableQuantity(quantity)
                    .reservedQuantity(0)
                    .updatedAt(Instant.now())
                    .build());
        }
    }
}
