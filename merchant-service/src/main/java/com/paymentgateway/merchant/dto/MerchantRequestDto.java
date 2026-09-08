package com.paymentgateway.merchant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantRequestDto {

    @NotBlank(message = "Merchant business name is required")
    @Size(min = 2, max = 100, message = "Business name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Business email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;
}
