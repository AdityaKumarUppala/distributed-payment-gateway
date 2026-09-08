package com.paymentgateway.payment.dto;

import com.paymentgateway.payment.entity.Payment;
import com.paymentgateway.payment.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {
    private Long id;
    private String paymentReference;
    private Long customerId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String idempotencyKey;
    private String providerReference;
    private String failureReason;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentResponseDto fromEntity(Payment payment) {
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .customerId(payment.getCustomerId())
                .merchantId(payment.getMerchantId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .idempotencyKey(payment.getIdempotencyKey())
                .providerReference(payment.getProviderReference())
                .failureReason(payment.getFailureReason())
                .version(payment.getVersion())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
