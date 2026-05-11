package com.demo.inventory.repository;

import com.demo.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findWithLockBySku(String sku);
}
