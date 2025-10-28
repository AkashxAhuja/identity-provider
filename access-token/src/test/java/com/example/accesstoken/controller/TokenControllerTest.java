package com.example.accesstoken.controller;

import com.example.accesstoken.dto.JwkKey;
import com.example.accesstoken.dto.JwksResponse;
import com.example.accesstoken.dto.TokenRequest;
import com.example.accesstoken.dto.TokenResponse;
import com.example.accesstoken.dto.TokenRevocationRequest;
import com.example.accesstoken.dto.TokenValidationRequest;
import com.example.accesstoken.dto.TokenValidationResponse;
import com.example.accesstoken.exception.InvalidClientException;
import com.example.accesstoken.service.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private TokenController controller;

    @Test
    void issueTokenReturnsCreatedResponse() {
        TokenResponse response = new TokenResponse("token", "Bearer", 3600L, Instant.now(), List.of("scope"));
        when(tokenService.generateToken(any(TokenRequest.class), eq("identity-admin"), eq("change-me")))
                .thenReturn(response);

        var entity = controller.issueToken(basic("identity-admin", "change-me"),
                "client_credentials",
                "tokens.read tokens.write",
                null,
                "api://device-management");

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entity.getBody()).isEqualTo(response);

        ArgumentCaptor<TokenRequest> captor = ArgumentCaptor.forClass(TokenRequest.class);
        verify(tokenService).generateToken(captor.capture(), eq("identity-admin"), eq("change-me"));
        TokenRequest captured = captor.getValue();
        assertThat(captured.getGrantType()).isEqualTo("client_credentials");
        assertThat(captured.getScope()).isEqualTo("tokens.read tokens.write");
        assertThat(captured.getAudience()).isEqualTo("api://device-management");
        assertThat(captured.getResource()).isNull();
    }

    @Test
    void issueTokenRequiresBasicAuthorizationHeader() {
        assertThatThrownBy(() -> controller.issueToken(null, "client_credentials", null, null, null))
                .isInstanceOf(InvalidClientException.class)
                .hasMessageContaining("Client authentication required");
    }

    @Test
    void introspectDelegatesToService() {
        TokenValidationRequest request = new TokenValidationRequest();
        request.setToken("token");
        TokenValidationResponse response = new TokenValidationResponse(true, "sub", "client", List.of(), Instant.now());
        when(tokenService.validate("token")).thenReturn(response);

        var entity = controller.introspect(request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(response);
        verify(tokenService).validate("token");
    }

    @Test
    void revokeDelegatesAndReturnsNoContent() {
        TokenRevocationRequest request = new TokenRevocationRequest();
        request.setToken("token");

        var entity = controller.revoke(request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(entity.getBody()).isNull();
        verify(tokenService).revoke("token");
    }

    @Test
    void jwksReturnsKeys() {
        JwksResponse response = new JwksResponse(List.of(new JwkKey("RSA", "kid", "RS256", "sig", "n", "e")));
        when(tokenService.jwks()).thenReturn(response);

        var entity = controller.jwks();

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(response);
        verify(tokenService).jwks();
    }

    private String basic(String clientId, String clientSecret) {
        String value = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
