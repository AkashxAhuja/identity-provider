package com.example.accesstoken.service;

import com.example.accesstoken.dto.TokenRequest;
import com.example.accesstoken.dto.TokenResponse;
import com.example.accesstoken.dto.TokenValidationResponse;
import com.example.accesstoken.exception.InvalidClientException;
import com.example.accesstoken.exception.InvalidTokenException;
import com.example.accesstoken.repository.ApplicationSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TokenServiceTest {

    private static final String CLIENT_ID = "device-service";
    private static final String CLIENT_SECRET = "super-secret";

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ApplicationSessionRepository sessionRepository;

    @Test
    void generateTokenReturnsEncryptedJweForValidClient() {
        TokenRequest request = buildRequest();

        TokenResponse response = tokenService.generateToken(request, CLIENT_ID, CLIENT_SECRET);

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getScope()).containsExactlyInAnyOrderElementsOf(request.getScopes());
        assertThat(response.getAccessToken().split("\\.")).hasSize(5);
        assertThat(tokenService.validate(response.getAccessToken()).isActive()).isTrue();
    }

    @Test
    void generateTokenRejectsUnsupportedGrantType() {
        TokenRequest request = buildRequest();
        request.setGrantType("password");

        assertThatThrownBy(() -> tokenService.generateToken(request, CLIENT_ID, CLIENT_SECRET))
                .isInstanceOf(InvalidClientException.class)
                .hasMessageContaining("Unsupported grant type");
    }

    @Test
    void generateTokenRejectsUnknownClient() {
        TokenRequest request = buildRequest();

        assertThatThrownBy(() -> tokenService.generateToken(request, "unknown", CLIENT_SECRET))
                .isInstanceOf(InvalidClientException.class)
                .hasMessageContaining("Invalid client credentials");
    }

    @Test
    void generateTokenRejectsInvalidSecret() {
        TokenRequest request = buildRequest();

        assertThatThrownBy(() -> tokenService.generateToken(request, CLIENT_ID, "wrong"))
                .isInstanceOf(InvalidClientException.class)
                .hasMessageContaining("Invalid client credentials");
    }

    @Test
    void validateRejectsMalformedToken() {
        assertThatThrownBy(() -> tokenService.validate("malformed"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Unable to verify token");
    }

    @Test
    void validateReturnsInactiveWhenSessionMissing() {
        TokenResponse issued = tokenService.generateToken(buildRequest(), CLIENT_ID, CLIENT_SECRET);
        sessionRepository.deleteAll();

        TokenValidationResponse response = tokenService.validate(issued.getAccessToken());

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void revokeMarksSessionInactive() {
        TokenResponse issued = tokenService.generateToken(buildRequest(), CLIENT_ID, CLIENT_SECRET);
        tokenService.revoke(issued.getAccessToken());

        TokenValidationResponse response = tokenService.validate(issued.getAccessToken());

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void revokeThrowsWhenTokenNotIssued() {
        assertThatThrownBy(() -> tokenService.revoke("missing"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token was not issued");
    }

    @Test
    void jwksReturnsRsaKeyDetails() {
        assertThat(tokenService.jwks().keys()).hasSize(1);
        assertThat(tokenService.jwks().keys().get(0).kty()).isEqualTo("RSA");
    }

    private TokenRequest buildRequest() {
        TokenRequest request = new TokenRequest();
        request.setGrantType("client_credentials");
        request.setScope("devices.read devices.write");
        request.setResource("device-management");
        return request;
    }
}