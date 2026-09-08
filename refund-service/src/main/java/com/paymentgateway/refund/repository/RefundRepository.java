package com.paymentgateway.refund.repository;

import com.paymentgateway.refund.entity.Refund;
import com.paymentgateway.refund.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRefundReference(String refundReference);

    List<Refund> findByPaymentReference(String paymentReference);

    List<Refund> findByPaymentReferenceAndStatus(String paymentReference, RefundStatus status);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.paymentReference = :paymentReference AND r.status = 'SUCCESS'")
    BigDecimal sumSuccessfulRefundsByPaymentReference(@Param("paymentReference") String paymentReference);
}
