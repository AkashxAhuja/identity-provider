package com.example.accesstoken.service;

import com.example.accesstoken.exception.InvalidClientException;
import com.example.accesstoken.model.ApplicationClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ApplicationRegistryServiceTest {

    @Autowired
    private ApplicationRegistryService registryService;

    @Test
    void authenticatesBasicClient() {
        String header = basic("mib-service", "mib-secret");

        ApplicationClient client = registryService.authenticateFromAuthorizationHeader(header);

        assertThat(client.getClientId()).isEqualTo("mib-service");
    }

    @Test
    void rejectsInvalidBasicSecret() {
        String header = basic("mib-service", "wrong");

        assertThatThrownBy(() -> registryService.authenticateFromAuthorizationHeader(header))
                .isInstanceOf(InvalidClientException.class)
                .hasMessageContaining("Invalid client credentials");
    }

    @Test
    void authenticatesStaticTokenClient() {
        ApplicationClient client = registryService.authenticateFromAuthorizationHeader("Bearer partner-token-123");

        assertThat(client.getAuthMode()).isNotNull();
        assertThat(client.getClientSecret()).isNotBlank();
    }

    @Test
    void rejectsUnknownStaticToken() {
        assertThatThrownBy(() -> registryService.authenticateFromAuthorizationHeader("Bearer missing"))
                .isInstanceOf(InvalidClientException.class)
                .hasMessageContaining("Invalid client credentials");
    }

    private String basic(String clientId, String clientSecret) {
        String value = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
