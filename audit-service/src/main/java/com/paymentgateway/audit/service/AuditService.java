package com.paymentgateway.audit.service;

import com.paymentgateway.audit.entity.AuditEvent;

import java.util.List;

public interface AuditService {
    void recordEvent(String topic, String key, String payload);
    List<AuditEvent> getAllEvents();
    List<AuditEvent> getEventsByAggregateId(String aggregateId);
    List<AuditEvent> getEventsByAggregateType(String aggregateType);
}
