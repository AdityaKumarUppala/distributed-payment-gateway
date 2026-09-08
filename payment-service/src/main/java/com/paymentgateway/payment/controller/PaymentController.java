package com.paymentgateway.payment.controller;

import com.paymentgateway.payment.dto.PaymentRequestDto;
import com.paymentgateway.payment.dto.PaymentResponseDto;
import com.paymentgateway.payment.dto.PaymentStatusUpdateDto;
import com.paymentgateway.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Core payment processing, idempotency validation, and payment lifecycle management")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Authorize and process a payment with Idempotency-Key guarantee")
    public ResponseEntity<PaymentResponseDto> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Authenticated-Merchant-Id", required = false) Long authenticatedMerchantId,
            @Valid @RequestBody PaymentRequestDto requestDto) {
        PaymentResponseDto response = paymentService.processPayment(requestDto, idempotencyKey, authenticatedMerchantId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{paymentReference}")
    @Operation(summary = "Retrieve payment status and details by payment reference")
    public ResponseEntity<PaymentResponseDto> getPaymentByReference(
            @PathVariable("paymentReference") String paymentReference,
            @RequestHeader(value = "X-Authenticated-Merchant-Id", required = false) Long authenticatedMerchantId) {
        PaymentResponseDto response = paymentService.getPaymentByReference(paymentReference, authenticatedMerchantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Retrieve payment by database ID")
    public ResponseEntity<PaymentResponseDto> getPaymentById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Authenticated-Merchant-Id", required = false) Long authenticatedMerchantId) {
        PaymentResponseDto response = paymentService.getPaymentById(id, authenticatedMerchantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Search payments by Customer ID or Merchant ID")
    public ResponseEntity<List<PaymentResponseDto>> searchPayments(
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "merchantId", required = false) Long merchantId,
            @RequestHeader(value = "X-Authenticated-Merchant-Id", required = false) Long authenticatedMerchantId) {
        if (customerId != null) {
            return ResponseEntity.ok(paymentService.getPaymentsByCustomerId(customerId));
        } else if (merchantId != null) {
            return ResponseEntity.ok(paymentService.getPaymentsByMerchantId(merchantId, authenticatedMerchantId));
        } else if (authenticatedMerchantId != null) {
            return ResponseEntity.ok(paymentService.getPaymentsByMerchantId(authenticatedMerchantId, authenticatedMerchantId));
        }
        return ResponseEntity.badRequest().build();
    }

    @PatchMapping("/{paymentReference}/status")
    @Operation(summary = "Update payment status (Internal/Refund transition endpoint)")
    public ResponseEntity<PaymentResponseDto> updatePaymentStatus(
            @PathVariable("paymentReference") String paymentReference,
            @Valid @RequestBody PaymentStatusUpdateDto updateDto) {
        PaymentResponseDto response = paymentService.updatePaymentStatus(paymentReference, updateDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentReference}/cancel")
    @Operation(summary = "Cancel a created payment before processing")
    public ResponseEntity<PaymentResponseDto> cancelPayment(
            @PathVariable("paymentReference") String paymentReference,
            @RequestHeader(value = "X-Authenticated-Merchant-Id", required = false) Long authenticatedMerchantId) {
        PaymentResponseDto response = paymentService.cancelPayment(paymentReference, authenticatedMerchantId);
        return ResponseEntity.ok(response);
    }
}
