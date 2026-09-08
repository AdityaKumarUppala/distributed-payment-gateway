package com.paymentgateway.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "merchant-service", url = "${services.merchant-service.url:http://localhost:8082}")
public interface MerchantClient {

    @GetMapping("/api/v1/merchants/{id}/validate")
    void validateMerchant(@PathVariable("id") Long id);
}
