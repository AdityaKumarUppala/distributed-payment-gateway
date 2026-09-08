package com.paymentgateway.refund.service;

import com.paymentgateway.refund.dto.RefundRequestDto;
import com.paymentgateway.refund.dto.RefundResponseDto;
import com.paymentgateway.refund.dto.RefundSummaryDto;

import java.util.List;

public interface RefundService {
    RefundResponseDto processRefund(String paymentReference, RefundRequestDto requestDto, Long authenticatedMerchantId);
    RefundResponseDto getRefundByReference(String refundReference, Long authenticatedMerchantId);
    List<RefundResponseDto> getRefundsByPaymentReference(String paymentReference, Long authenticatedMerchantId);
    RefundSummaryDto getRefundSummary(String paymentReference, Long authenticatedMerchantId);
}
