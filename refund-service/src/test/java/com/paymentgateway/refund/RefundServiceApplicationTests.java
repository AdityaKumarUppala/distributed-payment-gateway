package com.paymentgateway.refund;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.refund.client.PaymentClient;
import com.paymentgateway.refund.client.dto.PaymentClientResponseDto;
import com.paymentgateway.refund.client.dto.PaymentStatusUpdateDto;
import com.paymentgateway.refund.dto.RefundRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RefundServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentClient paymentClient;

    @MockBean
    private org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;

    private final String paymentRef = "pay_test_refund_100";

    @BeforeEach
    void setUp() {
        PaymentClientResponseDto mockPayment = PaymentClientResponseDto.builder()
                .id(100L)
                .paymentReference(paymentRef)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status("SUCCESS")
                .build();

        when(paymentClient.getPaymentByReference(paymentRef)).thenReturn(mockPayment);
        when(paymentClient.updatePaymentStatus(eq(paymentRef), any(PaymentStatusUpdateDto.class))).thenReturn(mockPayment);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void shouldProcessPartialAndFullRefundsWithOverRefundGuard() throws Exception {
        // 1. Partial Refund #1: $30.00
        RefundRequestDto refund1 = RefundRequestDto.builder()
                .amount(new BigDecimal("30.00"))
                .reason("Customer dissatisfaction partial refund")
                .build();

        mockMvc.perform(post("/api/v1/payments/" + paymentRef + "/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refund1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(30.00)))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.refundReference", startsWith("ref_")));

        // 2. Partial Refund #2: $20.00
        RefundRequestDto refund2 = RefundRequestDto.builder()
                .amount(new BigDecimal("20.00"))
                .reason("Second partial adjustment")
                .build();

        mockMvc.perform(post("/api/v1/payments/" + paymentRef + "/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refund2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(20.00)))
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        // 3. Verify Summary: Total refunded = $50, Remaining = $50
        mockMvc.perform(get("/api/v1/payments/" + paymentRef + "/refunds/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalAmount", is(100.00)))
                .andExpect(jsonPath("$.totalRefunded", is(50.00)))
                .andExpect(jsonPath("$.remainingRefundable", is(50.00)))
                .andExpect(jsonPath("$.refunds", hasSize(2)));

        // 4. Over-Refund Attempt: Attempt to refund $60.00 when only $50.00 is left -> Rejection
        RefundRequestDto overRefund = RefundRequestDto.builder()
                .amount(new BigDecimal("60.00"))
                .reason("Greedy refund attempt exceeding balance")
                .build();

        mockMvc.perform(post("/api/v1/payments/" + paymentRef + "/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overRefund)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_REFUND_REQUEST")));

        // 5. Final Full Refund: $50.00 (reaches exactly $100 total)
        RefundRequestDto finalRefund = RefundRequestDto.builder()
                .amount(new BigDecimal("50.00"))
                .reason("Final refund settling remainder")
                .build();

        mockMvc.perform(post("/api/v1/payments/" + paymentRef + "/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalRefund)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(50.00)))
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        // 6. Summary check: Remaining is now $0.00
        mockMvc.perform(get("/api/v1/payments/" + paymentRef + "/refunds/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRefunded", is(100.00)))
                .andExpect(jsonPath("$.remainingRefundable", is(0.00)))
                .andExpect(jsonPath("$.refunds", hasSize(3)));
    }
}
