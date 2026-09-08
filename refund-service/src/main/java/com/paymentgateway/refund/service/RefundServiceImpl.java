package com.paymentgateway.refund.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.refund.client.PaymentClient;
import com.paymentgateway.refund.client.dto.PaymentClientResponseDto;
import com.paymentgateway.refund.client.dto.PaymentStatusUpdateDto;
import com.paymentgateway.refund.dto.RefundRequestDto;
import com.paymentgateway.refund.dto.RefundResponseDto;
import com.paymentgateway.refund.dto.RefundSummaryDto;
import com.paymentgateway.refund.entity.OutboxEvent;
import com.paymentgateway.refund.entity.OutboxStatus;
import com.paymentgateway.refund.entity.Refund;
import com.paymentgateway.refund.entity.RefundStatus;
import com.paymentgateway.refund.exception.InvalidPaymentStateException;
import com.paymentgateway.refund.exception.InvalidRefundException;
import com.paymentgateway.refund.exception.ResourceNotFoundException;
import com.paymentgateway.refund.repository.OutboxEventRepository;
import com.paymentgateway.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentClient paymentClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RefundResponseDto processRefund(String paymentReference, RefundRequestDto requestDto, Long authenticatedMerchantId) {
        log.info("Processing refund request for payment: {}, amount: {}, authMerchant: {}",
                paymentReference, requestDto.getAmount(), authenticatedMerchantId);

        // 1. Fetch Payment from Payment Service
        PaymentClientResponseDto payment = paymentClient.getPaymentByReference(paymentReference);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found with reference: " + paymentReference);
        }

        // Enforce Merchant Authorization (Prevent Cross-Merchant Refund IDOR)
        if (authenticatedMerchantId != null && payment.getMerchantId() != null && !payment.getMerchantId().equals(authenticatedMerchantId)) {
            throw new com.paymentgateway.refund.exception.UnauthorizedAccessException(String.format(
                    "Access Denied: Merchant [%d] cannot refund a payment belonging to merchant [%d]",
                    authenticatedMerchantId, payment.getMerchantId()));
        }

        String currentStatus = payment.getStatus();
        if (!"SUCCESS".equals(currentStatus) && !"PARTIALLY_REFUNDED".equals(currentStatus)) {
            throw new InvalidPaymentStateException(
                    "Cannot refund payment with status '" + currentStatus + "'. Payment must be in SUCCESS or PARTIALLY_REFUNDED state.");
        }

        // 2. Concurrency & Over-Refund Guard: Calculate remaining balance
        BigDecimal totalRefundedSoFar = refundRepository.sumSuccessfulRefundsByPaymentReference(paymentReference);
        BigDecimal remainingRefundable = payment.getAmount().subtract(totalRefundedSoFar);

        BigDecimal normalizedRefundAmount = requestDto.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
        if (normalizedRefundAmount.compareTo(remainingRefundable) > 0) {
            throw new InvalidRefundException(String.format(
                    "Requested refund amount %s exceeds remaining refundable balance %s for payment %s (Original: %s, Already Refunded: %s)",
                    normalizedRefundAmount, remainingRefundable, paymentReference, payment.getAmount(), totalRefundedSoFar));
        }

        // 3. Mark Payment status as REFUND_PENDING
        paymentClient.updatePaymentStatus(paymentReference, PaymentStatusUpdateDto.builder()
                .targetStatus("REFUND_PENDING")
                .description("Refund initiation for amount: " + normalizedRefundAmount)
                .build());

        // 4. Create Refund entity
        String refundReference = "ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String providerRefundId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Refund refund = Refund.builder()
                .refundReference(refundReference)
                .paymentReference(paymentReference)
                .paymentId(payment.getId())
                .amount(normalizedRefundAmount)
                .status(RefundStatus.SUCCESS)
                .reason(requestDto.getReason() != null ? requestDto.getReason() : "Customer requested refund")
                .providerRefundId(providerRefundId)
                .build();

        Refund savedRefund = refundRepository.save(refund);

        // 5. Calculate new total refunded and update Payment status
        BigDecimal newTotalRefunded = totalRefundedSoFar.add(requestDto.getAmount());
        String finalPaymentStatus = (newTotalRefunded.compareTo(payment.getAmount()) >= 0) ? "REFUNDED" : "PARTIALLY_REFUNDED";

        paymentClient.updatePaymentStatus(paymentReference, PaymentStatusUpdateDto.builder()
                .targetStatus(finalPaymentStatus)
                .description("Refund succeeded. Status updated to " + finalPaymentStatus)
                .build());

        // 6. Stage Outbox event
        saveOutboxEvent("RefundSucceeded", savedRefund);
        log.info("Refund {} completed successfully. Payment {} updated to {}",
                refundReference, paymentReference, finalPaymentStatus);

        return RefundResponseDto.fromEntity(savedRefund);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundByReference(String refundReference, Long authenticatedMerchantId) {
        Refund refund = refundRepository.findByRefundReference(refundReference)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with reference: " + refundReference));
        validateMerchantForPaymentRef(refund.getPaymentReference(), authenticatedMerchantId);
        return RefundResponseDto.fromEntity(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponseDto> getRefundsByPaymentReference(String paymentReference, Long authenticatedMerchantId) {
        validateMerchantForPaymentRef(paymentReference, authenticatedMerchantId);
        return refundRepository.findByPaymentReference(paymentReference).stream()
                .map(RefundResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RefundSummaryDto getRefundSummary(String paymentReference, Long authenticatedMerchantId) {
        validateMerchantForPaymentRef(paymentReference, authenticatedMerchantId);
        PaymentClientResponseDto payment = paymentClient.getPaymentByReference(paymentReference);
        List<RefundResponseDto> refunds = refundRepository.findByPaymentReference(paymentReference).stream()
                .map(RefundResponseDto::fromEntity)
                .collect(Collectors.toList());
        BigDecimal totalRefunded = refundRepository.sumSuccessfulRefundsByPaymentReference(paymentReference);
        BigDecimal remaining = payment.getAmount().subtract(totalRefunded);

        return RefundSummaryDto.builder()
                .paymentReference(paymentReference)
                .originalAmount(payment.getAmount())
                .totalRefunded(totalRefunded)
                .remainingRefundable(remaining)
                .currency(payment.getCurrency())
                .refunds(refunds)
                .build();
    }

    private void validateMerchantForPaymentRef(String paymentReference, Long authenticatedMerchantId) {
        if (authenticatedMerchantId != null) {
            PaymentClientResponseDto payment = paymentClient.getPaymentByReference(paymentReference);
            if (payment != null && payment.getMerchantId() != null && !payment.getMerchantId().equals(authenticatedMerchantId)) {
                throw new com.paymentgateway.refund.exception.UnauthorizedAccessException(String.format(
                        "Access Denied: Merchant [%d] cannot access refunds for payment belonging to merchant [%d]",
                        authenticatedMerchantId, payment.getMerchantId()));
            }
        }
    }

    private void saveOutboxEvent(String eventType, Refund refund) {
        try {
            String payloadJson = objectMapper.writeValueAsString(RefundResponseDto.fromEntity(refund));
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateType("REFUND")
                    .aggregateId(refund.getRefundReference())
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for refund {}: ", refund.getRefundReference(), e);
        }
    }
}
