package com.paymentgateway.merchant.dto;

import com.paymentgateway.merchant.entity.Merchant;
import com.paymentgateway.merchant.entity.MerchantStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantResponseDto {
    private Long id;
    private String name;
    private String email;
    private String apiKey;
    private MerchantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MerchantResponseDto fromEntity(Merchant merchant) {
        return MerchantResponseDto.builder()
                .id(merchant.getId())
                .name(merchant.getName())
                .email(merchant.getEmail())
                .apiKey(merchant.getApiKey())
                .status(merchant.getStatus())
                .createdAt(merchant.getCreatedAt())
                .updatedAt(merchant.getUpdatedAt())
                .build();
    }
}
