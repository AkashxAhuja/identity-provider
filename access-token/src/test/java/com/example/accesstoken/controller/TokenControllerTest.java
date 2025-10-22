package com.example.accesstoken.controller;

import com.example.accesstoken.dto.TokenResponse;
import com.example.accesstoken.dto.TokenValidationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void tokenLifecycleAndJwks() throws Exception {
        String tokenRequest = """
                {
                  \"grantType\": \"client_credentials\",
                  \"clientId\": \"device-service\",
                  \"clientSecret\": \"super-secret\",
                  \"subject\": \"device-service\",
                  \"scopes\": [\"devices.manage\"],
                  \"audience\": \"identity-provider\",
                  \"expiresIn\": 600
                }
                """;

        MvcResult tokenResult = mockMvc.perform(post("/oauth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(tokenResult.getResponse().getContentAsString(), TokenResponse.class);
        assertThat(tokenResponse.getAccessToken()).isNotBlank();
        assertThat(tokenResponse.getScope()).containsExactly("devices.manage");

        String introspectRequest = """
                {
                  \"token\": \"%s\"
                }
                """.formatted(tokenResponse.getAccessToken());

        MvcResult introspectionResult = mockMvc.perform(post("/oauth/token/introspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(introspectRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        TokenValidationResponse validationResponse = objectMapper.readValue(introspectionResult.getResponse().getContentAsString(), TokenValidationResponse.class);
        assertThat(validationResponse.getClientId()).isEqualTo("device-service");
        assertThat(validationResponse.getScope()).containsExactly("devices.manage");

        mockMvc.perform(post("/oauth/token/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(introspectRequest))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/oauth/token/introspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(introspectRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        MvcResult jwksResult = mockMvc.perform(get("/oauth/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andReturn();

        JsonNode jwks = objectMapper.readTree(jwksResult.getResponse().getContentAsString());
        assertThat(jwks.path("keys").isArray()).isTrue();
        assertThat(jwks.path("keys").get(0).path("n").asText()).isNotBlank();
        assertThat(jwks.path("keys").get(0).path("e").asText()).isNotBlank();
    }
}
