package com.demo.legacy.repository;

import com.demo.legacy.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {
}
