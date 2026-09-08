package com.paymentgateway.refund.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refund_reference", columnList = "refund_reference", unique = true),
    @Index(name = "idx_refund_payment_ref", columnList = "payment_reference"),
    @Index(name = "idx_refund_payment_id", columnList = "payment_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_reference", nullable = false, unique = true, length = 64)
    private String refundReference;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_reference", nullable = false, length = 64)
    private String paymentReference;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RefundStatus status = RefundStatus.CREATED;

    @Column(length = 255)
    private String reason;

    @Column(name = "provider_refund_id", length = 64)
    private String providerRefundId;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
