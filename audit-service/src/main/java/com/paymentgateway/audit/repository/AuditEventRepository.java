package com.paymentgateway.audit.repository;

import com.paymentgateway.audit.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByAggregateIdOrderByCreatedAtAsc(String aggregateId);
    List<AuditEvent> findByAggregateTypeOrderByCreatedAtDesc(String aggregateType);
    List<AuditEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(String aggregateType, String aggregateId);
}
