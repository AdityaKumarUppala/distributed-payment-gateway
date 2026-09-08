package com.paymentgateway.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.notification.entity.NotificationRecord;
import com.paymentgateway.notification.repository.NotificationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherService {

    private final NotificationRecordRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handlePaymentOrRefundEvent(String topic, String key, String payload) {
        log.info("Processing incoming event from topic {}: key={}", topic, key);

        try {
            JsonNode root = objectMapper.readTree(payload);
            String reference = key != null ? key : (root.has("paymentReference") ? root.get("paymentReference").asText() : "unknown");
            String status = root.has("status") ? root.get("status").asText() : "UNKNOWN";
            String amount = root.has("amount") ? root.get("amount").asText() : "0.00";
            String currency = root.has("currency") ? root.get("currency").asText() : "USD";

            String eventType = topic.contains("refund") ? "REFUND_" + status : "PAYMENT_" + status;

            // Idempotent consumer check: Ensure duplicate deliveries don't send duplicate emails
            if (repository.existsByReferenceIdAndEventType(reference, eventType)) {
                log.warn("Duplicate notification suppressed for reference {} and eventType {}", reference, eventType);
                return;
            }

            String subject = String.format("Transaction Update: %s for %s %s", eventType, amount, currency);
            String message = String.format("Hello, your transaction with reference [%s] has reached status: %s. Amount: %s %s.",
                    reference, status, amount, currency);

            // 1. Simulate sending Email
            log.info("[EMAIL DISPATCH SIMULATOR] To: customer@example.com | Subject: {} | Body: {}", subject, message);
            NotificationRecord emailRecord = NotificationRecord.builder()
                    .referenceId(reference)
                    .eventType(eventType)
                    .channel("EMAIL")
                    .recipient("customer@example.com")
                    .subject(subject)
                    .message(message)
                    .status("SENT")
                    .build();
            repository.save(emailRecord);

            // 2. Simulate sending SMS
            log.info("[SMS DISPATCH SIMULATOR] To: +14155551234 | Text: {}", message);
            NotificationRecord smsRecord = NotificationRecord.builder()
                    .referenceId(reference)
                    .eventType(eventType)
                    .channel("SMS")
                    .recipient("+14155551234")
                    .subject("SMS ALERT")
                    .message(message)
                    .status("SENT")
                    .build();
            repository.save(smsRecord);

        } catch (Exception e) {
            log.error("Failed to parse and dispatch notification for payload: {}", payload, e);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationRecord> getAllNotifications() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<NotificationRecord> getNotificationsByReference(String reference) {
        return repository.findByReferenceId(reference);
    }
}
