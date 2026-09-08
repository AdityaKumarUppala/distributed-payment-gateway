package com.paymentgateway.audit.consumer;

import com.paymentgateway.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditKafkaConsumer {

    private final AuditService auditService;

    @KafkaListener(
            topics = {"payment.events", "refund.events", "customer.events", "merchant.events"},
            groupId = "${spring.kafka.consumer.group-id:audit-service-group}"
    )
    public void onEventReceived(
            @Payload String payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic) {
        log.info("Audit consumer processing event from topic {}: key={}", topic, key);
        auditService.recordEvent(topic, key, payload);
    }
}
