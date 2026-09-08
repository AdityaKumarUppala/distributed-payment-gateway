package com.paymentgateway.customer.exception;

public class InvalidCustomerStateException extends RuntimeException {
    public InvalidCustomerStateException(String message) {
        super(message);
    }
}
