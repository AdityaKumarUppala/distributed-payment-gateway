package com.paymentgateway.payment.service;

import com.paymentgateway.payment.dto.PaymentRequestDto;
import com.paymentgateway.payment.dto.PaymentResponseDto;
import com.paymentgateway.payment.dto.PaymentStatusUpdateDto;

import java.util.List;

public interface PaymentService {
    PaymentResponseDto processPayment(PaymentRequestDto requestDto, String idempotencyKey, Long authenticatedMerchantId);
    PaymentResponseDto getPaymentById(Long id, Long authenticatedMerchantId);
    PaymentResponseDto getPaymentByReference(String paymentReference, Long authenticatedMerchantId);
    List<PaymentResponseDto> getPaymentsByCustomerId(Long customerId);
    List<PaymentResponseDto> getPaymentsByMerchantId(Long merchantId, Long authenticatedMerchantId);
    PaymentResponseDto updatePaymentStatus(String paymentReference, PaymentStatusUpdateDto updateDto);
    PaymentResponseDto cancelPayment(String paymentReference, Long authenticatedMerchantId);
}
