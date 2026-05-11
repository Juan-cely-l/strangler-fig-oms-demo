package com.demo.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    @Id
    @Column(length = 36)
    private String eventId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private String result;

    @Column(nullable = false)
    private Instant processedAt;
}
