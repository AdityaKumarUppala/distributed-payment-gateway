package com.paymentgateway.refund.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentClientResponseDto {
    private Long id;
    private String paymentReference;
    private Long customerId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String providerReference;
    private Long version;
}
