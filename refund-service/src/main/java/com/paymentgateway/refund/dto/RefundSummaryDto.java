package com.paymentgateway.refund.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundSummaryDto {
    private String paymentReference;
    private BigDecimal originalAmount;
    private BigDecimal totalRefunded;
    private BigDecimal remainingRefundable;
    private String currency;
    private List<RefundResponseDto> refunds;
}
