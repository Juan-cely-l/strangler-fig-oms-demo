package com.demo.inventory;

import com.demo.inventory.config.SqsProperties;
import com.demo.inventory.entity.InventoryItem;
import com.demo.inventory.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Instant;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(SqsProperties.class)
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }

    @Bean
    CommandLineRunner seedInventory(InventoryRepository inventoryRepository) {
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
