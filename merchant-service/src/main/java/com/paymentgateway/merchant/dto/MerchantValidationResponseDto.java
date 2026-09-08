package com.paymentgateway.merchant.dto;

import com.paymentgateway.merchant.entity.MerchantStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantValidationResponseDto {
    private Long merchantId;
    private String name;
    private MerchantStatus status;
    private boolean valid;
}
