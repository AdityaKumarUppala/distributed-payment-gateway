package com.paymentgateway.payment.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundProviderResult {
    private PaymentProviderStatus status;
    private String providerRefundId;
    private String message;
}
