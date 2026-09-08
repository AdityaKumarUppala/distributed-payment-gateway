package com.paymentgateway.payment.provider;

public interface PaymentProvider {
    PaymentProviderResult processPayment(PaymentProviderRequest request);
    RefundProviderResult refundPayment(RefundProviderRequest request);
}
