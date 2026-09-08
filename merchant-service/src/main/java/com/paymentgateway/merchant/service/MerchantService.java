package com.paymentgateway.merchant.service;

import com.paymentgateway.merchant.dto.MerchantRequestDto;
import com.paymentgateway.merchant.dto.MerchantResponseDto;
import com.paymentgateway.merchant.dto.MerchantValidationResponseDto;

import java.util.List;

public interface MerchantService {
    MerchantResponseDto createMerchant(MerchantRequestDto requestDto);
    MerchantResponseDto getMerchantById(Long id);
    MerchantResponseDto getMerchantByApiKey(String apiKey);
    List<MerchantResponseDto> getAllMerchants();
    MerchantResponseDto updateMerchantStatus(Long id, String status);
    MerchantValidationResponseDto validateMerchantApiKey(String apiKey);
    void validateMerchantForPayment(Long id);
}
