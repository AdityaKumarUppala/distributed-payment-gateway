package com.paymentgateway.payment.repository;

import com.paymentgateway.payment.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);
}
