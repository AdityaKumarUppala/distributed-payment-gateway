package com.paymentgateway.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.payment.client.CustomerClient;
import com.paymentgateway.payment.client.MerchantClient;
import com.paymentgateway.payment.dto.PaymentRequestDto;
import com.paymentgateway.payment.dto.PaymentStatusUpdateDto;
import com.paymentgateway.payment.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaymentServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerClient customerClient;

    @MockBean
    private MerchantClient merchantClient;

    @MockBean
    private org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        doNothing().when(customerClient).validateCustomer(anyLong());
        doNothing().when(merchantClient).validateMerchant(anyLong());
    }

    @Test
    void contextLoads() {
    }

    @Test
    void shouldProcessPaymentSuccessfully() throws Exception {
        PaymentRequestDto request = PaymentRequestDto.builder()
                .customerId(1L)
                .merchantId(10L)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        String response = mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-success-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.paymentReference", startsWith("pay_")))
                .andExpect(jsonPath("$.providerReference", startsWith("ch_")))
                .andExpect(jsonPath("$.amount", is(100.00)))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        String paymentRef = root.get("paymentReference").asText();

        // Query by reference
        mockMvc.perform(get("/api/v1/payments/" + paymentRef))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    void shouldHandleIdempotentRequestsCorrectly() throws Exception {
        PaymentRequestDto request = PaymentRequestDto.builder()
                .customerId(1L)
                .merchantId(10L)
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .build();

        // 1. Initial Request
        String response1 = mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idem-repeat-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode root1 = objectMapper.readTree(response1);
        String ref1 = root1.get("paymentReference").asText();

        // 2. Exact Repeat Request with same key -> Idempotent replay, same payment returned
        String response2 = mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idem-repeat-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode root2 = objectMapper.readTree(response2);
        String ref2 = root2.get("paymentReference").asText();

        org.junit.jupiter.api.Assertions.assertEquals(ref1, ref2);

        // 3. Repeat Request with same key but DIFFERENT amount -> 422 Conflict
        PaymentRequestDto alteredRequest = PaymentRequestDto.builder()
                .customerId(1L)
                .merchantId(10L)
                .amount(new BigDecimal("999.00"))
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idem-repeat-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alteredRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error", is("IDEMPOTENCY_CONFLICT")));
    }

    @Test
    void shouldHandlePaymentProviderFailure() throws Exception {
        // Amount ending in .91 triggers INSUFFICIENT_FUNDS in MockPaymentProvider
        PaymentRequestDto request = PaymentRequestDto.builder()
                .customerId(1L)
                .merchantId(10L)
                .amount(new BigDecimal("75.91"))
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-fail-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.failureReason", containsString("INSUFFICIENT_FUNDS")));
    }

    @Test
    void shouldEnforceStateMachineRules() throws Exception {
        PaymentRequestDto request = PaymentRequestDto.builder()
                .customerId(2L)
                .merchantId(10L)
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .build();

        String response = mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "state-machine-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ref = objectMapper.readTree(response).get("paymentReference").asText();

        // SUCCESS -> REFUNDED directly without REFUND_PENDING is invalid
        PaymentStatusUpdateDto illegalTransition = PaymentStatusUpdateDto.builder()
                .targetStatus(PaymentStatus.REFUNDED)
                .description("Illegal direct refund jump")
                .build();

        mockMvc.perform(patch("/api/v1/payments/" + ref + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalTransition)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_PAYMENT_STATE")));

        // Valid transition: SUCCESS -> REFUND_PENDING
        PaymentStatusUpdateDto validTransition = PaymentStatusUpdateDto.builder()
                .targetStatus(PaymentStatus.REFUND_PENDING)
                .description("Initiating refund")
                .build();

        mockMvc.perform(patch("/api/v1/payments/" + ref + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransition)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUND_PENDING")));
    }
}
