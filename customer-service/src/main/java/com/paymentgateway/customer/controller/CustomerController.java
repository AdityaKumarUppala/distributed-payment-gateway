package com.paymentgateway.customer.controller;

import com.paymentgateway.customer.dto.CustomerRequestDto;
import com.paymentgateway.customer.dto.CustomerResponseDto;
import com.paymentgateway.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Endpoints for customer registration, retrieval, and status management")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Create a new customer account")
    public ResponseEntity<CustomerResponseDto> createCustomer(@Valid @RequestBody CustomerRequestDto requestDto) {
        CustomerResponseDto response = customerService.createCustomer(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve customer profile by ID")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable("id") Long id) {
        CustomerResponseDto response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all customers")
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{id}/validate")
    @Operation(summary = "Validate that customer exists and is in ACTIVE status (used for payment validation)")
    public ResponseEntity<Void> validateCustomer(@PathVariable("id") Long id) {
        customerService.validateCustomerForPayment(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update customer account status (ACTIVE, SUSPENDED, INACTIVE)")
    public ResponseEntity<CustomerResponseDto> updateCustomerStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status) {
        CustomerResponseDto response = customerService.updateCustomerStatus(id, status);
        return ResponseEntity.ok(response);
    }
}
