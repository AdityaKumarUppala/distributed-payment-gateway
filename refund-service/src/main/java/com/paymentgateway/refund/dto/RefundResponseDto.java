package com.paymentgateway.refund.dto;

import com.paymentgateway.refund.entity.Refund;
import com.paymentgateway.refund.entity.RefundStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponseDto {
    private Long id;
    private String refundReference;
    private String paymentReference;
    private Long paymentId;
    private BigDecimal amount;
    private RefundStatus status;
    private String reason;
    private String providerRefundId;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RefundResponseDto fromEntity(Refund refund) {
        return RefundResponseDto.builder()
                .id(refund.getId())
                .refundReference(refund.getRefundReference())
                .paymentReference(refund.getPaymentReference())
                .paymentId(refund.getPaymentId())
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .providerRefundId(refund.getProviderRefundId())
                .version(refund.getVersion())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .build();
    }
}
