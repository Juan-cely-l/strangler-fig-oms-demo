package com.demo.orders.outbox;

import com.demo.orders.event.EventPublisher;
import com.demo.orders.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.publisher-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final EventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop10ByStatusInOrderByCreatedAtAsc(
                List.of(OutboxStatus.NEW, OutboxStatus.FAILED));

        for (OutboxEvent event : pending) {
            long start = System.nanoTime();
            try {
                eventPublisher.publish(event);
                event.markPublished();
                log.info("[OUTBOX] eventId={} orderId={} correlationId={} status=PUBLISHED outboxAgeMs={} publishCycleMs={}",
                        event.getEventId(),
                        event.getAggregateId(),
                        event.getCorrelationId(),
                        Duration.between(event.getCreatedAt(), Instant.now()).toMillis(),
                        elapsedMs(start));
            } catch (RuntimeException ex) {
                event.markFailed(ex.getMessage());
                log.warn("[OUTBOX] eventId={} orderId={} correlationId={} status=FAILED attempts={} reason={}",
                        event.getEventId(),
                        event.getAggregateId(),
                        event.getCorrelationId(),
                        event.getAttempts(),
                        ex.getMessage());
            }
        }
    }

    private long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
