package com.paymentgateway.merchant.exception;

public class InvalidMerchantStateException extends RuntimeException {
    public InvalidMerchantStateException(String message) {
        super(message);
    }
}
