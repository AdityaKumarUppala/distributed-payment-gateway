package com.paymentgateway.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock implementation of external Payment Acquiring Gateway (e.g., Stripe, Adyen).
 * Simulates real-world provider outcomes (Success, Failure, Insufficient Funds, Timeouts)
 * deterministically based on test trigger amounts.
 */
@Component
@Slf4j
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public PaymentProviderResult processPayment(PaymentProviderRequest request) {
        log.info("Sending payment request to provider for ref: {}, amount: {} {}",
                request.getPaymentReference(), request.getAmount(), request.getCurrency());

        BigDecimal amount = request.getAmount();
        String amountStr = amount.toPlainString();

        // Deterministic simulation rules for edge cases
        if (amountStr.endsWith(".91")) {
            log.warn("Mock provider simulating INSUFFICIENT_FUNDS");
            return PaymentProviderResult.builder()
                    .status(PaymentProviderStatus.INSUFFICIENT_FUNDS)
                    .message("The card issuer declined the transaction due to insufficient funds.")
                    .build();
        } else if (amountStr.endsWith(".92")) {
            log.warn("Mock provider simulating TIMEOUT");
            return PaymentProviderResult.builder()
                    .status(PaymentProviderStatus.TIMEOUT)
                    .message("Network timeout connecting to card network.")
                    .build();
        } else if (amountStr.endsWith(".93")) {
            log.warn("Mock provider simulating PROVIDER_ERROR");
            return PaymentProviderResult.builder()
                    .status(PaymentProviderStatus.PROVIDER_ERROR)
                    .message("Acquiring bank returned HTTP 500 Internal Error.")
                    .build();
        } else if (amountStr.endsWith(".94")) {
            log.warn("Mock provider simulating FAILED");
            return PaymentProviderResult.builder()
                    .status(PaymentProviderStatus.FAILED)
                    .message("Card lost or stolen / hard decline.")
                    .build();
        }

        // Standard Success
        String providerReference = "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Mock provider approved transaction. Provider Ref: {}", providerReference);

        return PaymentProviderResult.builder()
                .status(PaymentProviderStatus.SUCCESS)
                .providerReference(providerReference)
                .message("Payment captured successfully by acquiring network.")
                .build();
    }

    @Override
    public RefundProviderResult refundPayment(RefundProviderRequest request) {
        log.info("Sending refund request to provider for paymentRef: {}, refundRef: {}, amount: {}",
                request.getPaymentReference(), request.getRefundReference(), request.getAmount());

        BigDecimal amount = request.getAmount();
        String amountStr = amount.toPlainString();

        if (amountStr.endsWith(".93") || amountStr.endsWith(".94")) {
            log.warn("Mock provider simulating REFUND FAILED");
            return RefundProviderResult.builder()
                    .status(PaymentProviderStatus.FAILED)
                    .message("Acquiring network rejected refund request.")
                    .build();
        }

        String providerRefundId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Mock provider approved refund. Provider Refund Ref: {}", providerRefundId);

        return RefundProviderResult.builder()
                .status(PaymentProviderStatus.SUCCESS)
                .providerRefundId(providerRefundId)
                .message("Refund processed successfully by acquiring network.")
                .build();
    }
}
