package com.paymentgateway.payment.dto;

import com.paymentgateway.payment.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusUpdateDto {

    @NotNull(message = "Target status is required")
    private PaymentStatus targetStatus;

    private String description;
}
