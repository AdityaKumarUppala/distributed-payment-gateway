package com.paymentgateway.merchant.service;

import com.paymentgateway.merchant.dto.MerchantRequestDto;
import com.paymentgateway.merchant.dto.MerchantResponseDto;
import com.paymentgateway.merchant.dto.MerchantValidationResponseDto;
import com.paymentgateway.merchant.entity.Merchant;
import com.paymentgateway.merchant.entity.MerchantStatus;
import com.paymentgateway.merchant.exception.DuplicateResourceException;
import com.paymentgateway.merchant.exception.InvalidMerchantStateException;
import com.paymentgateway.merchant.exception.ResourceNotFoundException;
import com.paymentgateway.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;

    @Override
    @Transactional
    public MerchantResponseDto createMerchant(MerchantRequestDto requestDto) {
        log.info("Creating merchant account for email: {}", requestDto.getEmail());

        if (merchantRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException("Merchant with email '" + requestDto.getEmail() + "' already exists");
        }

        String apiKey = generateApiKey();

        Merchant merchant = Merchant.builder()
                .name(requestDto.getName())
                .email(requestDto.getEmail())
                .apiKey(apiKey)
                .status(MerchantStatus.ACTIVE)
                .build();

        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant created with ID: {}", saved.getId());

        return MerchantResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantResponseDto getMerchantById(Long id) {
        log.info("Fetching merchant by ID: {}", id);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with ID: " + id));
        return MerchantResponseDto.fromEntity(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantResponseDto getMerchantByApiKey(String apiKey) {
        Merchant merchant = merchantRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found for the provided API Key"));
        return MerchantResponseDto.fromEntity(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantResponseDto> getAllMerchants() {
        return merchantRepository.findAll().stream()
                .map(MerchantResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MerchantResponseDto updateMerchantStatus(Long id, String statusStr) {
        log.info("Updating merchant ID: {} status to {}", id, statusStr);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with ID: " + id));

        try {
            MerchantStatus newStatus = MerchantStatus.valueOf(statusStr.toUpperCase());
            merchant.setStatus(newStatus);
            Merchant updated = merchantRepository.save(merchant);
            return MerchantResponseDto.fromEntity(updated);
        } catch (IllegalArgumentException e) {
            throw new InvalidMerchantStateException("Invalid status value: " + statusStr + ". Allowed: ACTIVE, SUSPENDED, INACTIVE");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantValidationResponseDto validateMerchantApiKey(String apiKey) {
        log.info("Validating merchant API key");
        return merchantRepository.findByApiKey(apiKey)
                .map(m -> MerchantValidationResponseDto.builder()
                        .merchantId(m.getId())
                        .name(m.getName())
                        .status(m.getStatus())
                        .valid(m.getStatus() == MerchantStatus.ACTIVE)
                        .build())
                .orElse(MerchantValidationResponseDto.builder()
                        .valid(false)
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public void validateMerchantForPayment(Long id) {
        log.info("Validating merchant ID: {} for payment processing", id);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant with ID " + id + " does not exist"));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new InvalidMerchantStateException("Merchant account with ID " + id + " is not ACTIVE (current status: " + merchant.getStatus() + ")");
        }
    }

    private String generateApiKey() {
        return "mcht_live_" + UUID.randomUUID().toString().replace("-", "");
    }
}
