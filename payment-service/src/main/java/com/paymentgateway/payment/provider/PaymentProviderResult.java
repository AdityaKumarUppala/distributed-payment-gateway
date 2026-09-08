package com.paymentgateway.payment.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProviderResult {
    private PaymentProviderStatus status;
    private String providerReference;
    private String message;
}
