package com.paymentgateway.customer.service;

import com.paymentgateway.customer.dto.CustomerRequestDto;
import com.paymentgateway.customer.dto.CustomerResponseDto;
import com.paymentgateway.customer.entity.Customer;
import com.paymentgateway.customer.entity.CustomerStatus;
import com.paymentgateway.customer.exception.DuplicateResourceException;
import com.paymentgateway.customer.exception.InvalidCustomerStateException;
import com.paymentgateway.customer.exception.ResourceNotFoundException;
import com.paymentgateway.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerResponseDto createCustomer(CustomerRequestDto requestDto) {
        log.info("Creating customer with email: {}", requestDto.getEmail());

        if (customerRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException("Customer with email '" + requestDto.getEmail() + "' already exists");
        }

        Customer customer = Customer.builder()
                .name(requestDto.getName())
                .email(requestDto.getEmail())
                .phone(requestDto.getPhone())
                .status(CustomerStatus.ACTIVE)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer successfully created with id: {}", savedCustomer.getId());

        return CustomerResponseDto.fromEntity(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(Long id) {
        log.info("Fetching customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return CustomerResponseDto.fromEntity(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerByEmail(String email) {
        log.info("Fetching customer with email: {}", email);
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));
        return CustomerResponseDto.fromEntity(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(CustomerResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerResponseDto updateCustomerStatus(Long id, String statusStr) {
        log.info("Updating customer id: {} status to: {}", id, statusStr);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        try {
            CustomerStatus newStatus = CustomerStatus.valueOf(statusStr.toUpperCase());
            customer.setStatus(newStatus);
            Customer updated = customerRepository.save(customer);
            return CustomerResponseDto.fromEntity(updated);
        } catch (IllegalArgumentException e) {
            throw new InvalidCustomerStateException("Invalid status value: " + statusStr + ". Allowed: ACTIVE, SUSPENDED, INACTIVE");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCustomerForPayment(Long id) {
        log.info("Validating customer id: {} for payment readiness", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + id + " does not exist"));

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new InvalidCustomerStateException("Customer account with id " + id + " is not ACTIVE (current status: " + customer.getStatus() + ")");
        }
    }
}
