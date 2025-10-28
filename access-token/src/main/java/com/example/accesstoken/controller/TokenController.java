package com.example.accesstoken.controller;

import com.example.accesstoken.dto.JwksResponse;
import com.example.accesstoken.dto.TokenRequest;
import com.example.accesstoken.dto.TokenResponse;
import com.example.accesstoken.dto.TokenRevocationRequest;
import com.example.accesstoken.dto.TokenValidationRequest;
import com.example.accesstoken.dto.TokenValidationResponse;
import com.example.accesstoken.service.TokenService;
import com.example.accesstoken.exception.InvalidClientException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/oauth")
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<TokenResponse> issueToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "resource", required = false) String resource,
            @RequestParam(value = "audience", required = false) String audience) {
        TokenRequest request = new TokenRequest();
        request.setGrantType(grantType);
        request.setScope(scope);
        request.setResource(resource);
        request.setAudience(audience);

        ClientCredentials credentials = extractClientCredentials(authorization);
        TokenResponse response = tokenService.generateToken(request, credentials.clientId(), credentials.clientSecret());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/token/introspect")
    public ResponseEntity<TokenValidationResponse> introspect(@Valid @RequestBody TokenValidationRequest request) {
        TokenValidationResponse response = tokenService.validate(request.getToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/token/revoke")
    public ResponseEntity<Void> revoke(@Valid @RequestBody TokenRevocationRequest request) {
        tokenService.revoke(request.getToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<JwksResponse> jwks() {
        return ResponseEntity.ok(tokenService.jwks());
    }

    private ClientCredentials extractClientCredentials(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new InvalidClientException("Client authentication required");
        }

        if (!authorizationHeader.startsWith("Basic ")) {
            throw new InvalidClientException("Unsupported client authentication method");
        }

        String base64Credentials = authorizationHeader.substring("Basic ".length());
        byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(base64Credentials);
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

        return new ClientCredentials(clientId, clientSecret);
    }

    private record ClientCredentials(String clientId, String clientSecret) {
    }
}
