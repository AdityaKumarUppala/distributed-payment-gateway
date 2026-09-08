package com.paymentgateway.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Merchant ID is required")
    private Long merchantId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @jakarta.validation.constraints.Digits(integer = 12, fraction = 2, message = "Amount must have at most 12 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency code is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO-4217 code (e.g. USD, EUR, GBP)")
    private String currency;
}
