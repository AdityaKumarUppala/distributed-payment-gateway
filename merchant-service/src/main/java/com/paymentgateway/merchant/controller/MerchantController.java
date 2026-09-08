package com.paymentgateway.merchant.controller;

import com.paymentgateway.merchant.dto.MerchantRequestDto;
import com.paymentgateway.merchant.dto.MerchantResponseDto;
import com.paymentgateway.merchant.dto.MerchantValidationResponseDto;
import com.paymentgateway.merchant.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchant Management", description = "Endpoints for merchant onboarding, API key verification, and status management")
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    @Operation(summary = "Register a new merchant account and generate an API Key")
    public ResponseEntity<MerchantResponseDto> createMerchant(@Valid @RequestBody MerchantRequestDto requestDto) {
        MerchantResponseDto response = merchantService.createMerchant(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve merchant details by ID")
    public ResponseEntity<MerchantResponseDto> getMerchantById(@PathVariable("id") Long id) {
        MerchantResponseDto response = merchantService.getMerchantById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all registered merchants")
    public ResponseEntity<List<MerchantResponseDto>> getAllMerchants() {
        return ResponseEntity.ok(merchantService.getAllMerchants());
    }

    @GetMapping("/validate-key")
    @Operation(summary = "Validate API Key (used by API Gateway / Security Filters)")
    public ResponseEntity<MerchantValidationResponseDto> validateApiKey(
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader,
            @RequestParam(value = "apiKey", required = false) String apiKeyParam) {
        String apiKey = apiKeyHeader != null ? apiKeyHeader : apiKeyParam;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.ok(MerchantValidationResponseDto.builder().valid(false).build());
        }
        MerchantValidationResponseDto response = merchantService.validateMerchantApiKey(apiKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/validate")
    @Operation(summary = "Validate that merchant exists and is ACTIVE (used by Payment Service)")
    public ResponseEntity<Void> validateMerchant(@PathVariable("id") Long id) {
        merchantService.validateMerchantForPayment(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update merchant account status (ACTIVE, SUSPENDED, INACTIVE)")
    public ResponseEntity<MerchantResponseDto> updateMerchantStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status) {
        MerchantResponseDto response = merchantService.updateMerchantStatus(id, status);
        return ResponseEntity.ok(response);
    }
}
