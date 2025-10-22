package com.example.identity.controller;

import com.example.identity.dto.DeviceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deviceLifecycleHappyPath() throws Exception {
        String registrationPayload = """
                {
                  \"cid\": \"C12345678\",
                  \"deviceOs\": \"ANDROID\",
                  \"deviceOsVer\": \"14\",
                  \"model\": \"Pixel 8\",
                  \"appVersion\": \"1.4.3\",
                  \"lang\": \"en\",
                  \"imeNo\": \"352011119999999\",
                  \"deviceOsId\": \"os-id-123\",
                  \"userAgent\": \"MyApp/1.4.3 Android\",
                  \"isTouchEnabled\": true,
                  \"biometricType\": \"NONE\",
                  \"deviceNickName\": \"Primary Pixel\"
                }
                """;

        MvcResult registrationResult = mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "register-1")
                        .content(registrationPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cid").value("C12345678"))
                .andReturn();

        DeviceResponse response = objectMapper.readValue(registrationResult.getResponse().getContentAsString(), DeviceResponse.class);
        assertThat(response.getDeviceId()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo("A");

        mockMvc.perform(post("/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "register-1")
                        .content(registrationPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(response.getDeviceId()))
                .andExpect(jsonPath("$.status").value("A"));

        String biometricPayload = """
                {
                  \"cid\": \"C12345678\",
                  \"deviceId\": \"%s\",
                  \"biometricType\": \"FINGERPRINT\",
                  \"isTouchEnabled\": true
                }
                """.formatted(response.getDeviceId());

        mockMvc.perform(post("/devices/biometrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(biometricPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricType").value("FINGERPRINT"))
                .andExpect(jsonPath("$.isTouchEnabled").value(true));

        String detagPayload = """
                {
                  \"cid\": \"C12345678\",
                  \"deviceId\": \"%s\",
                  \"reason\": \"user request\",
                  \"revokeSessions\": true,
                  \"hardDelete\": false
                }
                """.formatted(response.getDeviceId());

        mockMvc.perform(post("/devices/detag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(detagPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("I"));
    }
}
