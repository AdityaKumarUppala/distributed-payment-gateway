package com.paymentgateway.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payment.client.CustomerClient;
import com.paymentgateway.payment.client.MerchantClient;
import com.paymentgateway.payment.dto.PaymentRequestDto;
import com.paymentgateway.payment.dto.PaymentResponseDto;
import com.paymentgateway.payment.dto.PaymentStatusUpdateDto;
import com.paymentgateway.payment.entity.OutboxEvent;
import com.paymentgateway.payment.entity.OutboxStatus;
import com.paymentgateway.payment.entity.Payment;
import com.paymentgateway.payment.entity.PaymentEvent;
import com.paymentgateway.payment.entity.PaymentStatus;
import com.paymentgateway.payment.exception.IdempotencyConflictException;
import com.paymentgateway.payment.exception.ResourceNotFoundException;
import com.paymentgateway.payment.exception.UnauthorizedAccessException;
import com.paymentgateway.payment.provider.*;
import com.paymentgateway.payment.repository.OutboxEventRepository;
import com.paymentgateway.payment.repository.PaymentEventRepository;
import com.paymentgateway.payment.repository.PaymentRepository;
import com.paymentgateway.payment.statemachine.PaymentStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentStateMachine stateMachine;
    private final PaymentProvider paymentProvider;
    private final CustomerClient customerClient;
    private final MerchantClient merchantClient;
    private final ObjectMapper objectMapper;
    private final RateLimiterService rateLimiterService;
    private final IdempotencyRedisService idempotencyRedisService;

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto requestDto, String idempotencyKey, Long authenticatedMerchantId) {
        // Enforce Merchant Authorization Boundary (Prevent IDOR / Cross-Tenant Impersonation)
        if (authenticatedMerchantId != null) {
            if (requestDto.getMerchantId() != null && !requestDto.getMerchantId().equals(authenticatedMerchantId)) {
                throw new UnauthorizedAccessException(String.format(
                        "Access Denied: Authenticated merchant ID [%d] cannot create payments for merchant ID [%d]",
                        authenticatedMerchantId, requestDto.getMerchantId()));
            }
            requestDto.setMerchantId(authenticatedMerchantId);
        }

        log.info("Processing payment for merchant: {}, customer: {}, amount: {} {}, idempotencyKey: {}",
                requestDto.getMerchantId(), requestDto.getCustomerId(), requestDto.getAmount(), requestDto.getCurrency(), idempotencyKey);

        // 0. Rate Limiting Check (Redis)
        rateLimiterService.checkRateLimit(requestDto.getMerchantId());

        // 1. Redis Fast-Path Idempotency Cache Lookup
        Optional<PaymentResponseDto> cachedResponse = idempotencyRedisService.getCachedPayment(
                requestDto.getMerchantId(), idempotencyKey);
        if (cachedResponse.isPresent()) {
            PaymentResponseDto cached = cachedResponse.get();
            log.info("Fast-path returning cached idempotency payment: {}", cached.getPaymentReference());
            return cached;
        }

        // 1b. DB Idempotency Check (ACID Source of Truth)
        Optional<Payment> existingOpt = paymentRepository.findByMerchantIdAndIdempotencyKey(
                requestDto.getMerchantId(), idempotencyKey);

        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();
            log.warn("Duplicate request detected for Idempotency-Key: {} and merchant: {}", idempotencyKey, requestDto.getMerchantId());

            if (!existing.getAmount().stripTrailingZeros().equals(requestDto.getAmount().stripTrailingZeros()) ||
                !existing.getCurrency().equalsIgnoreCase(requestDto.getCurrency()) ||
                !existing.getCustomerId().equals(requestDto.getCustomerId())) {
                throw new IdempotencyConflictException(
                        "Idempotency key '" + idempotencyKey + "' was already used with different payment parameters.");
            }

            return PaymentResponseDto.fromEntity(existing);
        }

        // 2. Synchronous External Validations (Performed OUTSIDE DB transaction)
        log.info("Validating customer ID: {} with Customer Service", requestDto.getCustomerId());
        customerClient.validateCustomer(requestDto.getCustomerId());

        log.info("Validating merchant ID: {} with Merchant Service", requestDto.getMerchantId());
        merchantClient.validateMerchant(requestDto.getMerchantId());

        // 3. Generate unique payment reference
        String paymentReference = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        // 4. Invoke Payment Provider (Performed OUTSIDE DB transaction to prevent HikariCP connection starvation)
        PaymentProviderRequest providerRequest = PaymentProviderRequest.builder()
                .paymentReference(paymentReference)
                .customerId(requestDto.getCustomerId())
                .merchantId(requestDto.getMerchantId())
                .amount(requestDto.getAmount())
                .currency(requestDto.getCurrency().toUpperCase())
                .build();

        PaymentProviderResult providerResult = paymentProvider.processPayment(providerRequest);

        // 5. Atomic Local DB Write (Short-lived Transaction for Payment + Events + Outbox)
        Payment finalPayment = persistPaymentTransaction(requestDto, idempotencyKey, paymentReference, providerResult);

        // 6. Asynchronously Cache Result in Redis after DB commit
        PaymentResponseDto responseDto = PaymentResponseDto.fromEntity(finalPayment);
        idempotencyRedisService.cachePayment(requestDto.getMerchantId(), idempotencyKey, responseDto);

        return responseDto;
    }

    /**
     * Isolated short-lived ACID database transaction.
     * Guarantees atomic insertion of Payment, internal event audit logs, and transactional outbox.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment persistPaymentTransaction(PaymentRequestDto requestDto, String idempotencyKey,
                                             String paymentReference, PaymentProviderResult providerResult) {
        BigDecimal normalizedAmount = requestDto.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
        PaymentStatus initialStatus = PaymentStatus.CREATED;
        Payment payment = Payment.builder()
                .paymentReference(paymentReference)
                .customerId(requestDto.getCustomerId())
                .merchantId(requestDto.getMerchantId())
                .amount(normalizedAmount)
                .currency(requestDto.getCurrency().toUpperCase())
                .status(initialStatus)
                .idempotencyKey(idempotencyKey)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        recordPaymentEvent(savedPayment, null, PaymentStatus.CREATED, "PAYMENT_CREATED", "Payment initialized in system");
        saveOutboxEvent("PaymentCreated", savedPayment);

        // State Machine Transition: CREATED -> PROCESSING -> (SUCCESS / FAILED)
        stateMachine.validateTransition(savedPayment.getStatus(), PaymentStatus.PROCESSING, paymentReference);
        savedPayment.setStatus(PaymentStatus.PROCESSING);
        recordPaymentEvent(savedPayment, PaymentStatus.CREATED, PaymentStatus.PROCESSING, "PAYMENT_PROCESSING", "Dispatched to payment provider");

        if (providerResult.getStatus() == PaymentProviderStatus.SUCCESS) {
            stateMachine.validateTransition(savedPayment.getStatus(), PaymentStatus.SUCCESS, paymentReference);
            savedPayment.setStatus(PaymentStatus.SUCCESS);
            savedPayment.setProviderReference(providerResult.getProviderReference());
            savedPayment.setFailureReason(null);
            recordPaymentEvent(savedPayment, PaymentStatus.PROCESSING, PaymentStatus.SUCCESS, "PAYMENT_SUCCEEDED", providerResult.getMessage());
            saveOutboxEvent("PaymentSucceeded", savedPayment);
            log.info("Payment {} finalized as SUCCESS with providerRef: {}", paymentReference, providerResult.getProviderReference());
        } else {
            stateMachine.validateTransition(savedPayment.getStatus(), PaymentStatus.FAILED, paymentReference);
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setFailureReason(providerResult.getStatus() + ": " + providerResult.getMessage());
            recordPaymentEvent(savedPayment, PaymentStatus.PROCESSING, PaymentStatus.FAILED, "PAYMENT_FAILED", providerResult.getMessage());
            saveOutboxEvent("PaymentFailed", savedPayment);
            log.warn("Payment {} finalized as FAILED: {}", paymentReference, providerResult.getMessage());
        }

        return paymentRepository.save(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentById(Long id, Long authenticatedMerchantId) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + id));

        validateMerchantOwnership(payment, authenticatedMerchantId);
        return PaymentResponseDto.fromEntity(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByReference(String paymentReference, Long authenticatedMerchantId) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + paymentReference));

        validateMerchantOwnership(payment, authenticatedMerchantId);
        return PaymentResponseDto.fromEntity(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByCustomerId(Long customerId) {
        return paymentRepository.findByCustomerId(customerId).stream()
                .map(PaymentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByMerchantId(Long merchantId, Long authenticatedMerchantId) {
        if (authenticatedMerchantId != null && !merchantId.equals(authenticatedMerchantId)) {
            throw new UnauthorizedAccessException(String.format(
                    "Access Denied: Merchant [%d] cannot query payment history of merchant [%d]",
                    authenticatedMerchantId, merchantId));
        }

        return paymentRepository.findByMerchantId(merchantId).stream()
                .map(PaymentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentResponseDto updatePaymentStatus(String paymentReference, PaymentStatusUpdateDto updateDto) {
        log.info("Updating payment reference {} status to {}", paymentReference, updateDto.getTargetStatus());
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + paymentReference));

        PaymentStatus oldStatus = payment.getStatus();
        PaymentStatus newStatus = updateDto.getTargetStatus();

        stateMachine.validateTransition(oldStatus, newStatus, paymentReference);

        payment.setStatus(newStatus);
        recordPaymentEvent(payment, oldStatus, newStatus, "STATUS_UPDATE_" + newStatus.name(), updateDto.getDescription());

        Payment updated = paymentRepository.save(payment);
        return PaymentResponseDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public PaymentResponseDto cancelPayment(String paymentReference, Long authenticatedMerchantId) {
        log.info("Cancelling payment reference {}", paymentReference);
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + paymentReference));

        validateMerchantOwnership(payment, authenticatedMerchantId);

        PaymentStatus oldStatus = payment.getStatus();
        stateMachine.validateTransition(oldStatus, PaymentStatus.CANCELLED, paymentReference);

        payment.setStatus(PaymentStatus.CANCELLED);
        recordPaymentEvent(payment, oldStatus, PaymentStatus.CANCELLED, "PAYMENT_CANCELLED", "Payment cancelled by client");
        saveOutboxEvent("PaymentCancelled", payment);

        Payment updated = paymentRepository.save(payment);
        return PaymentResponseDto.fromEntity(updated);
    }

    private void validateMerchantOwnership(Payment payment, Long authenticatedMerchantId) {
        if (authenticatedMerchantId != null && !payment.getMerchantId().equals(authenticatedMerchantId)) {
            throw new UnauthorizedAccessException(String.format(
                    "Access Denied: Merchant [%d] does not have permission to view/modify payment belonging to merchant [%d]",
                    authenticatedMerchantId, payment.getMerchantId()));
        }
    }

    private void recordPaymentEvent(Payment payment, PaymentStatus oldStatus, PaymentStatus newStatus, String eventType, String description) {
        PaymentEvent event = PaymentEvent.builder()
                .paymentId(payment.getId())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .eventType(eventType)
                .description(description)
                .build();
        paymentEventRepository.save(event);
    }

    private void saveOutboxEvent(String eventType, Payment payment) {
        try {
            String payloadJson = objectMapper.writeValueAsString(PaymentResponseDto.fromEntity(payment));
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateType("PAYMENT")
                    .aggregateId(payment.getPaymentReference())
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for payment {}: ", payment.getPaymentReference(), e);
        }
    }
}
