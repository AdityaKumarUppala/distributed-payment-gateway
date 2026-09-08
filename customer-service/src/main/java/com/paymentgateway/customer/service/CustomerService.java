package com.paymentgateway.customer.service;

import com.paymentgateway.customer.dto.CustomerRequestDto;
import com.paymentgateway.customer.dto.CustomerResponseDto;

import java.util.List;

public interface CustomerService {
    CustomerResponseDto createCustomer(CustomerRequestDto requestDto);
    CustomerResponseDto getCustomerById(Long id);
    CustomerResponseDto getCustomerByEmail(String email);
    List<CustomerResponseDto> getAllCustomers();
    CustomerResponseDto updateCustomerStatus(Long id, String status);
    void validateCustomerForPayment(Long id);
}
