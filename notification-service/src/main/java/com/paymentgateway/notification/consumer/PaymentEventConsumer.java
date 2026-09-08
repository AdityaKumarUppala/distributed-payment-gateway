package com.paymentgateway.notification.consumer;

import com.paymentgateway.notification.service.NotificationDispatcherService;
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
public class PaymentEventConsumer {

    private final NotificationDispatcherService dispatcherService;

    @KafkaListener(
            topics = {"payment.events", "refund.events"},
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void onEventReceived(
            @Payload String payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic) {
        log.info("Kafka consumer received event on topic {}: key={}", topic, key);
        dispatcherService.handlePaymentOrRefundEvent(topic, key, payload);
    }
}
