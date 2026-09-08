package com.paymentgateway.refund.client;

import com.paymentgateway.refund.client.dto.PaymentClientResponseDto;
import com.paymentgateway.refund.client.dto.PaymentStatusUpdateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${services.payment-service.url:http://localhost:8083}")
public interface PaymentClient {

    @GetMapping("/api/v1/payments/{paymentReference}")
    PaymentClientResponseDto getPaymentByReference(@PathVariable("paymentReference") String paymentReference);

    @PatchMapping("/api/v1/payments/{paymentReference}/status")
    PaymentClientResponseDto updatePaymentStatus(
            @PathVariable("paymentReference") String paymentReference,
            @RequestBody PaymentStatusUpdateDto updateDto);
}
