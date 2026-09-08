package com.paymentgateway.payment.entity;

public enum PaymentStatus {
    CREATED,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUND_PENDING,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CANCELLED,
    EXPIRED
}
