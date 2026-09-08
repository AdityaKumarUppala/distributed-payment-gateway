package com.paymentgateway.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldRejectProtectedPaymentEndpointWithoutApiKey() {
        webTestClient.post()
                .uri("/api/v1/payments")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().value("X-Correlation-ID", correlationId -> {
                    assertNotNull(correlationId);
                })
                .expectBody()
                .jsonPath("$.error").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Missing required 'X-API-Key'"));
    }
}
