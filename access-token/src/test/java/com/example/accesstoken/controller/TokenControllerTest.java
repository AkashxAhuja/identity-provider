package com.example.accesstoken.controller;

import com.example.accesstoken.dto.JwkKey;
import com.example.accesstoken.dto.JwksResponse;
import com.example.accesstoken.dto.TokenRequest;
import com.example.accesstoken.dto.TokenResponse;
import com.example.accesstoken.dto.TokenRevocationRequest;
import com.example.accesstoken.dto.TokenValidationRequest;
import com.example.accesstoken.dto.TokenValidationResponse;
import com.example.accesstoken.service.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        TokenRequest request = new TokenRequest();
        TokenResponse response = new TokenResponse("token", "Bearer", 30L, Instant.now(), List.of("scope"));
        when(tokenService.generateToken(request)).thenReturn(response);

        var entity = controller.issueToken(request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entity.getBody()).isEqualTo(response);
        verify(tokenService).generateToken(request);
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
}
