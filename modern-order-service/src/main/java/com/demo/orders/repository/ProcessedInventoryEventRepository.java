package com.demo.orders.repository;

import com.demo.orders.entity.ProcessedInventoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedInventoryEventRepository extends JpaRepository<ProcessedInventoryEvent, String> {
}
