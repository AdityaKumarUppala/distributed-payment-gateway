package com.paymentgateway.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.customer.dto.CustomerRequestDto;
import com.paymentgateway.customer.entity.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CustomerServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldCreateCustomerAndFetchById() throws Exception {
        CustomerRequestDto request = CustomerRequestDto.builder()
                .name("Alice Johnson")
                .email("alice@example.com")
                .phone("+14155551234")
                .build();

        // 1. Create Customer
        String responseContent = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Alice Johnson")))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andReturn().getResponse().getContentAsString();

        // 2. Fetch Customer by ID
        mockMvc.perform(get("/api/v1/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice Johnson")));

        // 3. Validate Customer
        mockMvc.perform(get("/api/v1/customers/1/validate"))
                .andExpect(status().isOk());

        // 4. Duplicate email rejection
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_RESOURCE")));
    }
}
