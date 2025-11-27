package com.example.accesstoken.service;

import com.example.accesstoken.exception.InvalidClientException;
import com.example.accesstoken.model.ApplicationClient;
import com.example.accesstoken.model.AuthMode;
import com.example.accesstoken.repository.ApplicationClientRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class ApplicationRegistryService {

    private final ApplicationClientRepository repository;
    private final HashingService hashingService;

    public ApplicationRegistryService(ApplicationClientRepository repository, HashingService hashingService) {
        this.repository = repository;
        this.hashingService = hashingService;
    }

    public ApplicationClient authenticateFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new InvalidClientException("Client authentication required");
        }

        if (authorizationHeader.startsWith("Basic ")) {
            return authenticateBasicClient(authorizationHeader.substring("Basic ".length()));
        }

        return authenticateStaticTokenClient(authorizationHeader);
    }

    private ApplicationClient authenticateBasicClient(String encodedCredentials) {
        byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(encodedCredentials);
        } catch (IllegalArgumentException ex) {
            throw new InvalidClientException("Malformed client credentials");
        }

        String token = new String(decodedBytes, StandardCharsets.UTF_8);
        int delimiterIndex = token.indexOf(':');
        if (delimiterIndex <= 0 || delimiterIndex == token.length() - 1) {
            throw new InvalidClientException("Malformed client credentials");
        }

        String clientId = token.substring(0, delimiterIndex);
        String clientSecret = token.substring(delimiterIndex + 1);
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new InvalidClientException("Client authentication required");
        }

        ApplicationClient client = repository.findByClientIdAndActiveTrue(clientId)
                .filter(found -> found.getAuthMode() == AuthMode.BASIC)
                .orElseThrow(() -> new InvalidClientException("Invalid client credentials"));

        if (isExpired(client)) {
            throw new InvalidClientException("Client is inactive or expired");
        }

        String hashedSecret = hashingService.hashToBase64(clientSecret);
        if (!hashedSecret.equals(client.getClientSecretHash())) {
            throw new InvalidClientException("Invalid client credentials");
        }

        return client;
    }

    private ApplicationClient authenticateStaticTokenClient(String authorizationHeader) {
        String token = extractTokenValue(authorizationHeader);
        if (token.isBlank()) {
            throw new InvalidClientException("Client authentication required");
        }

        String hashedToken = hashingService.hashToBase64(token);
        ApplicationClient client = repository.findByStaticTokenHashAndActiveTrue(hashedToken)
                .filter(found -> found.getAuthMode() == AuthMode.STATIC_TOKEN)
                .orElseThrow(() -> new InvalidClientException("Invalid client credentials"));

        if (isExpired(client)) {
            throw new InvalidClientException("Client is inactive or expired");
        }

        return client;
    }

    private boolean isExpired(ApplicationClient client) {
        Instant expiresAt = client.getExpiresAt();
        return !client.isActive() || (expiresAt != null && expiresAt.isBefore(Instant.now()));
    }

    private String extractTokenValue(String authorizationHeader) {
        if (authorizationHeader.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return authorizationHeader.substring("Bearer ".length());
        }
        if (authorizationHeader.regionMatches(true, 0, "Partner ", 0, "Partner ".length())) {
            return authorizationHeader.substring("Partner ".length());
        }
        if (authorizationHeader.regionMatches(true, 0, HttpHeaders.AUTHORIZATION, 0, HttpHeaders.AUTHORIZATION.length())) {
            return authorizationHeader.substring(HttpHeaders.AUTHORIZATION.length()).trim();
        }
        return authorizationHeader;
    }
}
