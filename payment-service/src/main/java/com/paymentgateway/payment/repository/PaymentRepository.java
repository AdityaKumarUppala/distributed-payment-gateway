package com.paymentgateway.payment.repository;

import com.paymentgateway.payment.entity.Payment;
import com.paymentgateway.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    Optional<Payment> findByMerchantIdAndIdempotencyKey(Long merchantId, String idempotencyKey);
    List<Payment> findByCustomerId(Long customerId);
    List<Payment> findByMerchantId(Long merchantId);
    List<Payment> findByStatus(PaymentStatus status);
}
