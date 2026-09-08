package com.paymentgateway.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.audit.entity.AuditEvent;
import com.paymentgateway.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void recordEvent(String topic, String key, String payload) {
        log.info("Recording immutable audit event from topic {}: key={}", topic, key);

        String aggregateType = "UNKNOWN";
        if (topic.contains("payment")) aggregateType = "PAYMENT";
        else if (topic.contains("refund")) aggregateType = "REFUND";
        else if (topic.contains("customer")) aggregateType = "CUSTOMER";
        else if (topic.contains("merchant")) aggregateType = "MERCHANT";

        String eventType = "EVENT_" + aggregateType;
        String aggregateId = key != null ? key : "unknown";

        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root.has("status")) {
                eventType = aggregateType + "_" + root.get("status").asText();
            }
            if (aggregateId.equals("unknown")) {
                if (root.has("paymentReference")) aggregateId = root.get("paymentReference").asText();
                else if (root.has("refundReference")) aggregateId = root.get("refundReference").asText();
            }
        } catch (Exception e) {
            log.warn("Could not parse event JSON for metadata extraction; storing raw payload", e);
        }

        AuditEvent auditEvent = AuditEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .build();

        auditEventRepository.save(auditEvent);
        log.info("Persisted immutable audit record ID: {} for aggregate {} [{}]",
                auditEvent.getId(), aggregateType, aggregateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> getAllEvents() {
        return auditEventRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> getEventsByAggregateId(String aggregateId) {
        return auditEventRepository.findByAggregateIdOrderByCreatedAtAsc(aggregateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> getEventsByAggregateType(String aggregateType) {
        return auditEventRepository.findByAggregateTypeOrderByCreatedAtDesc(aggregateType.toUpperCase());
    }
}
