package com.paymentgateway.customer.dto;

import com.paymentgateway.customer.entity.Customer;
import com.paymentgateway.customer.entity.CustomerStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponseDto fromEntity(Customer customer) {
        return CustomerResponseDto.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
