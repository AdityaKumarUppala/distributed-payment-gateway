package com.paymentgateway.merchant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.merchant.dto.MerchantRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MerchantServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldCreateMerchantAndValidateApiKey() throws Exception {
        MerchantRequestDto request = MerchantRequestDto.builder()
                .name("Acme Corp")
                .email("billing@acme.com")
                .build();

        // 1. Create Merchant
        String responseContent = mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Acme Corp")))
                .andExpect(jsonPath("$.email", is("billing@acme.com")))
                .andExpect(jsonPath("$.apiKey", startsWith("mcht_live_")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(responseContent);
        String apiKey = root.get("apiKey").asText();
        Long merchantId = root.get("id").asLong();

        // 2. Fetch Merchant by ID
        mockMvc.perform(get("/api/v1/merchants/" + merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Acme Corp")));

        // 3. Validate API Key via Header
        mockMvc.perform(get("/api/v1/merchants/validate-key")
                        .header("X-API-Key", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.merchantId", is(merchantId.intValue())));

        // 4. Validate for payment
        mockMvc.perform(get("/api/v1/merchants/" + merchantId + "/validate"))
                .andExpect(status().isOk());

        // 5. Reject duplicate email
        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_RESOURCE")));
    }
}
