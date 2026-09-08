package com.paymentgateway.gateway.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantValidationResponseDto {
    private Long merchantId;
    private String name;
    private String status;
    private boolean valid;
}
