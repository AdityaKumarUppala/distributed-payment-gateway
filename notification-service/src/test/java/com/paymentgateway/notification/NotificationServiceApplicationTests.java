package com.paymentgateway.notification;

import com.paymentgateway.notification.entity.NotificationRecord;
import com.paymentgateway.notification.service.NotificationDispatcherService;
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
public class NotificationServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @MockBean
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldDispatchNotificationAndDeduplicateRepeats() throws Exception {
        String payload = """
                {
                    "paymentReference": "pay_test_notif_001",
                    "amount": 150.00,
                    "currency": "USD",
                    "status": "SUCCESS"
                }
                """;

        // 1. Process Event
        dispatcherService.handlePaymentOrRefundEvent("payment.events", "pay_test_notif_001", payload);

        List<NotificationRecord> records = dispatcherService.getNotificationsByReference("pay_test_notif_001");
        assertEquals(2, records.size()); // 1 EMAIL + 1 SMS

        // 2. Duplicate Event (Idempotent replay)
        dispatcherService.handlePaymentOrRefundEvent("payment.events", "pay_test_notif_001", payload);
        List<NotificationRecord> recordsAfterDup = dispatcherService.getNotificationsByReference("pay_test_notif_001");
        assertEquals(2, recordsAfterDup.size()); // Count remains 2, no duplicates created

        // 3. Query via REST API
        mockMvc.perform(get("/api/v1/notifications/reference/pay_test_notif_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].channel", is("EMAIL")))
                .andExpect(jsonPath("$[1].channel", is("SMS")));
    }
}
