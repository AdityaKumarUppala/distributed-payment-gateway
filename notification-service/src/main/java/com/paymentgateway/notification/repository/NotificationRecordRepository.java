package com.paymentgateway.notification.repository;

import com.paymentgateway.notification.entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {
    List<NotificationRecord> findByReferenceId(String referenceId);
    boolean existsByReferenceIdAndEventType(String referenceId, String eventType);
}
