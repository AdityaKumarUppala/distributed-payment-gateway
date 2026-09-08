package com.paymentgateway.payment.service;

import com.paymentgateway.payment.entity.OutboxEvent;
import com.paymentgateway.payment.entity.OutboxStatus;
import com.paymentgateway.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Background worker implementing the Transactional Outbox Pattern.
 * Guarantees At-Least-Once delivery from the Outbox table to Kafka topics
 * with synchronous future verification and isolated per-record transactions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public static final String TOPIC_PAYMENT_EVENTS = "payment.events";

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:2000}")
    public void publishPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to publish to Kafka", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            publishSingleEvent(event);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishSingleEvent(OutboxEvent event) {
        try {
            // Synchronously wait with 5s timeout to confirm broker ACK
            kafkaTemplate.send(TOPIC_PAYMENT_EVENTS, event.getAggregateId(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
            log.info("Successfully published outbox event {} (Type: {}) to topic {}",
                    event.getId(), event.getEventType(), TOPIC_PAYMENT_EVENTS);
        } catch (Exception e) {
            log.error("Failed to publish outbox event {} to Kafka. Retrying in next polling cycle. Error: {}",
                    event.getId(), e.getMessage());
        }
    }
}
