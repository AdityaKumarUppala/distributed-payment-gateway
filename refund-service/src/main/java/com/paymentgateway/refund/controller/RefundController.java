package com.paymentgateway.refund.controller;

import com.paymentgateway.refund.dto.RefundRequestDto;
import com.paymentgateway.refund.dto.RefundResponseDto;
import com.paymentgateway.refund.dto.RefundSummaryDto;
import com.paymentgateway.refund.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Refund Management", description = "Endpoints for initiating full and partial refunds, tracking refund history, and checking refundable balances")
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/payments/{paymentReference}/refunds")
    @Operation(summary = "Initiate a full or partial refund against a successful payment")
    public ResponseEntity<RefundResponseDto> createRefund(
            @PathVariable("paymentReference") String paymentReference,
            @RequestHeader(value = "X-Authenticated-Merchant-Id", required = false) Long authenticatedMerchantId,
            @Valid @RequestBody RefundRequestDto requestDto) {
        RefundResponseDto response = refundService.processRefund(paymentReference, requestDto, authenticatedMerchantId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/refunds/{refundReference}")
    @Operation(summary = "Retrieve refund details by refund reference")
    public ResponseEntity<RefundResponseDto> getRefundByReference(
            @PathVariable("refundReference") String refundReference,
            @RequestHeader(value = "X-Authenticated-Merchant-Id", required = false) Long authenticatedMerchantId) {
        RefundResponseDto response = refundService.getRefundByReference(refundReference, authenticatedMerchantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments/{paymentReference}/refunds")
    @Operation(summary = "List all refunds issued against a specific payment")
    public ResponseEntity<List<RefundResponseDto>> getRefundsForPayment(
            @PathVariable("paymentReference") String paymentReference,
            @RequestHeader(value = "X-Authenticated-Merchant-Id", required = false) Long authenticatedMerchantId) {
        return ResponseEntity.ok(refundService.getRefundsByPaymentReference(paymentReference, authenticatedMerchantId));
    }

    @GetMapping("/payments/{paymentReference}/refunds/summary")
    @Operation(summary = "Get refundable balance summary (original amount, refunded total, remaining balance)")
    public ResponseEntity<RefundSummaryDto> getRefundSummary(
            @PathVariable("paymentReference") String paymentReference,
            @RequestHeader(value = "X-Authenticated-Merchant-Id", required = false) Long authenticatedMerchantId) {
        return ResponseEntity.ok(refundService.getRefundSummary(paymentReference, authenticatedMerchantId));
    }
}
