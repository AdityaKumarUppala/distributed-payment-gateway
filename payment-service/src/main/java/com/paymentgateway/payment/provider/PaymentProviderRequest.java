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
public class PaymentProviderRequest {
    private String paymentReference;
    private Long customerId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
}
