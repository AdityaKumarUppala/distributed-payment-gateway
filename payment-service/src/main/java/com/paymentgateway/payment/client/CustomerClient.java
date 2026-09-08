package com.paymentgateway.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", url = "${services.customer-service.url:http://localhost:8081}")
public interface CustomerClient {

    @GetMapping("/api/v1/customers/{id}/validate")
    void validateCustomer(@PathVariable("id") Long id);
}
