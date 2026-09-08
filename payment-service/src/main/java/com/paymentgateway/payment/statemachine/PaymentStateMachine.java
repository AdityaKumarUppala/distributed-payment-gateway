package com.paymentgateway.payment.statemachine;

import com.paymentgateway.payment.entity.PaymentStatus;
import com.paymentgateway.payment.exception.InvalidPaymentStateException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic Finite State Machine governing all payment lifecycle transitions.
 * Prevents illegal state jumps and race-condition status corruptions.
 */
@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PaymentStatus.CREATED, EnumSet.of(
                PaymentStatus.PROCESSING,
                PaymentStatus.CANCELLED,
                PaymentStatus.EXPIRED
        ));

        ALLOWED_TRANSITIONS.put(PaymentStatus.PROCESSING, EnumSet.of(
                PaymentStatus.SUCCESS,
                PaymentStatus.FAILED
        ));

        ALLOWED_TRANSITIONS.put(PaymentStatus.SUCCESS, EnumSet.of(
                PaymentStatus.REFUND_PENDING
        ));

        ALLOWED_TRANSITIONS.put(PaymentStatus.REFUND_PENDING, EnumSet.of(
                PaymentStatus.REFUNDED,
                PaymentStatus.PARTIALLY_REFUNDED,
                PaymentStatus.SUCCESS // In case a pending refund fails, payment returns to SUCCESS
        ));

        ALLOWED_TRANSITIONS.put(PaymentStatus.PARTIALLY_REFUNDED, EnumSet.of(
                PaymentStatus.REFUND_PENDING,
                PaymentStatus.REFUNDED
        ));

        // Terminal states: No outgoing transitions
        ALLOWED_TRANSITIONS.put(PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED_TRANSITIONS.put(PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED_TRANSITIONS.put(PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED_TRANSITIONS.put(PaymentStatus.EXPIRED, EnumSet.noneOf(PaymentStatus.class));
    }

    public boolean canTransition(PaymentStatus from, PaymentStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<PaymentStatus> allowedTargets = ALLOWED_TRANSITIONS.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }

    public void validateTransition(PaymentStatus from, PaymentStatus to, String paymentReference) {
        if (!canTransition(from, to)) {
            throw new InvalidPaymentStateException(String.format(
                    "Invalid state transition for payment '%s': Cannot move from %s to %s. Allowed targets: %s",
                    paymentReference,
                    from,
                    to,
                    ALLOWED_TRANSITIONS.getOrDefault(from, Set.of())
            ));
        }
    }
}
