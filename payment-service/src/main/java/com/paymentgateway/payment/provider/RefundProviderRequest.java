package com.paymentgateway.payment.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundProviderRequest {
    private String paymentReference;
    private String refundReference;
    private String providerReference;
    private BigDecimal amount;
    private String currency;
}
