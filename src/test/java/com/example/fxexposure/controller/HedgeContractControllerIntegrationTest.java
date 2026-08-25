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

import com.example.fxexposure.dto.HedgeContractRequest;
import com.example.fxexposure.dto.LoginRequest;
import com.example.fxexposure.dto.SettlementRequest;
import com.example.fxexposure.enums.HedgeDirection;
import com.example.fxexposure.enums.HedgeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HedgeContractControllerIntegrationTest {

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
    void testHedgeLifecycleAndSettlement() throws Exception {
        HedgeContractRequest bookReq = HedgeContractRequest.builder()
                .hedgeType(HedgeType.FORWARD)
                .direction(HedgeDirection.SELL)
                .primaryCurrency("EUR")
                .secondaryCurrency("USD")
                .primaryAmount(new BigDecimal("500000.00"))
                .strikeRate(new BigDecimal("1.085000"))
                .tradeDate(LocalDate.now())
                .valueDate(LocalDate.now().plusDays(30))
                .counterpartyBank("HSBC Global")
                .build();

        // Book Deal
        MvcResult bookResult = mockMvc.perform(post("/api/hedges")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.counterpartyBank").value("HSBC Global"))
                .andReturn();

        JsonNode bookedNode = objectMapper.readTree(bookResult.getResponse().getContentAsString());
        long dealId = bookedNode.get("data").get("id").asLong();

        // Revalue single hedge
        mockMvc.perform(post("/api/hedges/" + dealId + "/revalue")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Settle Deal
        SettlementRequest settleReq = SettlementRequest.builder()
                .settlementRate(new BigDecimal("1.075000"))
                .notes("Integration test settlement")
                .build();

        mockMvc.perform(post("/api/hedges/" + dealId + "/settle")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SETTLED"));

        // Delete Deal
        mockMvc.perform(delete("/api/hedges/" + dealId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    void testAnalyticsEndpoints() throws Exception {
        mockMvc.perform(get("/api/analytics/net-exposure")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseCurrency").value("USD"));

        mockMvc.perform(get("/api/analytics/maturity-ladder")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/dashboard/overview")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseCurrency").value("USD"));
    }
}

