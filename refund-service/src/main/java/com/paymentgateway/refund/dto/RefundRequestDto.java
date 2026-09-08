package com.paymentgateway.refund.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequestDto {

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
    @jakarta.validation.constraints.Digits(integer = 12, fraction = 2, message = "Refund amount must have at most 12 integer digits and 2 decimal places")
    private BigDecimal amount;

    private String reason;
}
