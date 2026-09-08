package com.paymentgateway.audit;

import com.paymentgateway.audit.entity.AuditEvent;
import com.paymentgateway.audit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuditServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditService auditService;

    @MockBean
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldRecordAndQueryAuditEvents() throws Exception {
        String paymentCreatedPayload = """
                {"paymentReference":"pay_audit_test_001","status":"CREATED","amount":500.00}
                """;
        String paymentSuccessPayload = """
                {"paymentReference":"pay_audit_test_001","status":"SUCCESS","amount":500.00}
                """;

        // 1. Record events
        auditService.recordEvent("payment.events", "pay_audit_test_001", paymentCreatedPayload);
        auditService.recordEvent("payment.events", "pay_audit_test_001", paymentSuccessPayload);

        List<AuditEvent> trail = auditService.getEventsByAggregateId("pay_audit_test_001");
        assertEquals(2, trail.size());
        assertEquals("PAYMENT_CREATED", trail.get(0).getEventType());
        assertEquals("PAYMENT_SUCCESS", trail.get(1).getEventType());

        // 2. Query timeline via REST API
        mockMvc.perform(get("/api/v1/audit-events/aggregate/pay_audit_test_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventType", is("PAYMENT_CREATED")))
                .andExpect(jsonPath("$[1].eventType", is("PAYMENT_SUCCESS")));
    }
}
