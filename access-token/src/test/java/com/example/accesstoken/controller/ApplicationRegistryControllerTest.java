package com.example.accesstoken.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationRegistryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void issuesTokenForBasicRegistryClient() throws Exception {
        mockMvc.perform(post("/applications/authenticate")
                        .header(HttpHeaders.AUTHORIZATION, basic("mib-service", "mib-secret"))
                        .param("scope", "devices.read"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    void rejectsInvalidRegistryClient() throws Exception {
        mockMvc.perform(post("/applications/authenticate")
                        .header(HttpHeaders.AUTHORIZATION, basic("mib-service", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void issuesTokenForPartner() throws Exception {
        mockMvc.perform(post("/applications/authenticate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer partner-token-123")
                        .param("scope", "partners.read"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token", notNullValue()));
    }

    private String basic(String clientId, String clientSecret) {
        String value = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
