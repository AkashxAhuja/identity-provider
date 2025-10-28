package com.example.accesstoken.service;

import com.example.accesstoken.dto.TokenRequest;
import com.example.accesstoken.dto.TokenResponse;
import com.example.accesstoken.dto.TokenValidationResponse;
import com.example.accesstoken.exception.InvalidClientException;
import com.example.accesstoken.exception.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectiveOperationException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final String CLIENT_ID = "device-service";
    private static final String CLIENT_SECRET = "super-secret";

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(new ObjectMapper());
        tokenService.initialize();
    }

    @Test
    void generateTokenReturnsSignedResponseForValidClient() {
        TokenRequest request = buildRequest();

        TokenResponse response = tokenService.generateToken(request, CLIENT_ID, CLIENT_SECRET);

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getScope()).containsExactlyInAnyOrderElementsOf(request.getScopes());
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
    void generateTokenValidatesRequestedScopes() {
        TokenRequest request = buildRequest();
        request.setScope("devices.read unknown.scope");

        assertThatThrownBy(() -> tokenService.generateToken(request, CLIENT_ID, CLIENT_SECRET))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requested scopes");
    }

    @Test
    void generateTokenRequiresClientAuthentication() {
        TokenRequest request = buildRequest();

        assertThatThrownBy(() -> tokenService.generateToken(request, null, CLIENT_SECRET))
                .isInstanceOf(InvalidClientException.class)
                .hasMessageContaining("Client authentication required");

        assertThatThrownBy(() -> tokenService.generateToken(request, CLIENT_ID, ""))
                .isInstanceOf(InvalidClientException.class)
                .hasMessageContaining("Client authentication required");
    }

    @Test
    void validateReturnsInactiveForUnknownTokenWithValidSignature() throws Exception {
        TokenRequest request = buildRequest();
        TokenResponse issued = tokenService.generateToken(request, CLIENT_ID, CLIENT_SECRET);

        Map<String, ?> issuedTokens = getIssuedTokens();
        issuedTokens.remove(issued.getAccessToken());

        TokenValidationResponse response = tokenService.validate(issued.getAccessToken());

        assertThat(response.isActive()).isFalse();
        assertThat(response.getClientId()).isNull();
    }

    @Test
    void validateRejectsMalformedToken() {
        assertThatThrownBy(() -> tokenService.validate("malformed"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Malformed JWT");
    }

    @Test
    void validateRejectsTamperedSignature() {
        TokenResponse issued = tokenService.generateToken(buildRequest(), CLIENT_ID, CLIENT_SECRET);
        String[] segments = issued.getAccessToken().split("\\.");
        String tamperedPayload = segments[1].substring(0, segments[1].length() - 2) + "ab";
        String tampered = segments[0] + "." + tamperedPayload + "." + segments[2];

        assertThatThrownBy(() -> tokenService.validate(tampered))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void validateRejectsUnknownSigningKey() {
        TokenResponse issued = tokenService.generateToken(buildRequest(), CLIENT_ID, CLIENT_SECRET);
        try {
            Field keyIdField = TokenService.class.getDeclaredField("keyId");
            keyIdField.setAccessible(true);
            keyIdField.set(tokenService, "different");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }

        assertThatThrownBy(() -> tokenService.validate(issued.getAccessToken()))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Unknown signing key");
    }

    @Test
    void validateRejectsExpiredTokens() throws Exception {
        TokenResponse issued = tokenService.generateToken(buildRequest(), CLIENT_ID, CLIENT_SECRET);
        Map<String, Object> issuedTokens = getIssuedTokens();
        Object metadata = issuedTokens.get(issued.getAccessToken());
        Class<?> metadataClass = Class.forName("com.example.accesstoken.service.TokenService$TokenMetadata");
        Method clientId = metadataClass.getDeclaredMethod("clientId");
        Method subject = metadataClass.getDeclaredMethod("subject");
        Method scopes = metadataClass.getDeclaredMethod("scopes");
        Object expired = metadataClass.getDeclaredConstructor(String.class, String.class, Set.class, Instant.class)
                .newInstance(clientId.invoke(metadata), subject.invoke(metadata), scopes.invoke(metadata), Instant.now().minusSeconds(5));
        issuedTokens.put(issued.getAccessToken(), expired);

        TokenValidationResponse response = tokenService.validate(issued.getAccessToken());

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void validateRejectsRevokedTokens() {
        TokenResponse issued = tokenService.generateToken(buildRequest(), CLIENT_ID, CLIENT_SECRET);
        tokenService.revoke(issued.getAccessToken());

        TokenValidationResponse response = tokenService.validate(issued.getAccessToken());

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void revokeThrowsWhenTokenNotIssued() {
        TokenResponse issued = tokenService.generateToken(buildRequest(), CLIENT_ID, CLIENT_SECRET);
        getIssuedTokens().remove(issued.getAccessToken());

        assertThatThrownBy(() -> tokenService.revoke(issued.getAccessToken()))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token was not issued");
    }

    @Test
    void jwksReturnsRsaKeyDetails() {
        assertThat(tokenService.jwks().keys()).hasSize(1);
        assertThat(tokenService.jwks().keys().get(0).kty()).isEqualTo("RSA");
    }

    @Test
    void verifySignatureHandlesInvalidBase64() {
        String badSignature = "header.payload.invalid";

        assertThatThrownBy(() -> tokenService.validate(badSignature))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Unable to verify token");
    }

    @Test
    void signRemovesLeadingZeroInBase64() throws Exception {
        byte[] value = new byte[]{0, 1, 2, 3};
        Method method = TokenService.class.getDeclaredMethod("toBase64Url", byte[].class);
        method.setAccessible(true);
        String encoded = (String) method.invoke(tokenService, (Object) value);
        assertThat(encoded).isEqualTo(Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[]{1, 2, 3}));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getIssuedTokens() throws Exception {
        Field field = TokenService.class.getDeclaredField("issuedTokens");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(tokenService);
    }

    private TokenRequest buildRequest() {
        TokenRequest request = new TokenRequest();
        request.setGrantType("client_credentials");
        request.setScope("devices.read devices.write");
        request.setResource("device-management");
        return request;
    }
}
