package com.demo.orders.repository;

import com.demo.orders.outbox.OutboxEvent;
import com.demo.orders.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop10ByStatusInOrderByCreatedAtAsc(Collection<OutboxStatus> statuses);
}
