package com.example.fxexposure.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.fxexposure.dto.ExposureRequest;
import com.example.fxexposure.dto.LoginRequest;
import com.example.fxexposure.enums.CashFlowDirection;
import com.example.fxexposure.enums.ExposureType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExposureControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .email("admin@example.com")
                .password("Admin@123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        jwtToken = jsonNode.get("data").get("token").asText();
    }

    @Test
    void testExposureCrudOperations() throws Exception {
        ExposureRequest createReq = ExposureRequest.builder()
                .companyEntity("Test Subsidiary France")
                .exposureType(ExposureType.RECEIVABLE_INVOICE)
                .cashFlowDirection(CashFlowDirection.INFLOW)
                .currency("EUR")
                .amount(new BigDecimal("750000.00"))
                .valueDate(LocalDate.now().plusDays(40))
                .description("French Automotive Contract")
                .build();

        // Create Exposure
        MvcResult createResult = mockMvc.perform(post("/api/exposures")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyEntity").value("Test Subsidiary France"))
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long createdId = createdNode.get("data").get("id").asLong();

        // Get By ID
        mockMvc.perform(get("/api/exposures/" + createdId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdId));

        // Search
        mockMvc.perform(get("/api/exposures/search?currency=EUR")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // Delete
        mockMvc.perform(delete("/api/exposures/" + createdId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }
}

