package com.example.accesstoken.service;

import com.example.accesstoken.dto.JwkKey;
import com.example.accesstoken.dto.JwksResponse;
import com.example.accesstoken.dto.TokenRequest;
import com.example.accesstoken.dto.TokenResponse;
import com.example.accesstoken.dto.TokenValidationResponse;
import com.example.accesstoken.exception.InvalidClientException;
import com.example.accesstoken.exception.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private static final String SUPPORTED_GRANT_TYPE = "client_credentials";
    private static final long DEFAULT_TOKEN_TTL_SECONDS = 3600L;
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private KeyPair keyPair;
    private String keyId;

    private final Map<String, RegisteredClient> registeredClients = new ConcurrentHashMap<>();
    private final Map<String, TokenMetadata> issuedTokens = new ConcurrentHashMap<>();
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    public TokenService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initialize() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
            this.keyId = generateKeyId((RSAPublicKey) keyPair.getPublic());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to initialize RSA key pair", e);
        }

        registerClient("device-service", "super-secret", Set.of("devices.read", "devices.write", "devices.manage"));
        registerClient("identity-admin", "change-me", Set.of("tokens.read", "tokens.write"));
    }

    public TokenResponse generateToken(TokenRequest request, String clientId, String clientSecret) {
        if (!SUPPORTED_GRANT_TYPE.equals(request.getGrantType())) {
            throw new InvalidClientException("Unsupported grant type: " + request.getGrantType());
        }

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new InvalidClientException("Client authentication required");
        }

        RegisteredClient client = registeredClients.get(clientId);
        if (client == null || !client.matchesSecret(clientSecret)) {
            throw new InvalidClientException("Invalid client credentials");
        }

        Set<String> requestedScopes = Set.copyOf(request.getScopes());
        if (!client.allowedScopes().containsAll(requestedScopes)) {
            throw new IllegalArgumentException("Requested scopes exceed client permissions");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(DEFAULT_TOKEN_TTL_SECONDS);
        String scope = String.join(" ", requestedScopes);
        String jti = UUID.randomUUID().toString();
        String subject = client.clientId();
        Optional<String> audience = resolveAudience(request);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", "com.example.access-token");
        payload.put("sub", subject);
        audience.ifPresent(value -> payload.put("aud", value));
        payload.put("exp", expiresAt.getEpochSecond());
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("client_id", client.clientId());
        if (!scope.isBlank()) {
            payload.put("scope", scope);
        }
        payload.put("jti", jti);

        String token = sign(payload);
        TokenMetadata metadata = new TokenMetadata(client.clientId(), subject, requestedScopes, expiresAt);
        issuedTokens.put(token, metadata);
        revokedTokens.remove(token);

        return new TokenResponse(token, "Bearer", DEFAULT_TOKEN_TTL_SECONDS, issuedAt, List.copyOf(requestedScopes));
    }

    public TokenValidationResponse validate(String token) {
        TokenMetadata metadata = issuedTokens.get(token);
        if (metadata == null) {
            verifySignature(token); // ensure invalid tokens still trigger signature validation errors
            return new TokenValidationResponse(false, null, null, null, null);
        }

        if (revokedTokens.contains(token)) {
            return new TokenValidationResponse(false, null, null, null, null);
        }

        verifySignature(token);

        if (metadata.expiresAt().isBefore(Instant.now())) {
            return new TokenValidationResponse(false, null, null, null, null);
        }

        return new TokenValidationResponse(true, metadata.subject(), metadata.clientId(), List.copyOf(metadata.scopes()), metadata.expiresAt());
    }

    public void revoke(String token) {
        if (!issuedTokens.containsKey(token)) {
            verifySignature(token);
            throw new InvalidTokenException("Token was not issued by this authorization server");
        }

        revokedTokens.add(token);
    }

    public JwksResponse jwks() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String modulus = toBase64Url(publicKey.getModulus().toByteArray());
        String exponent = toBase64Url(publicKey.getPublicExponent().toByteArray());
        JwkKey key = new JwkKey("RSA", keyId, "RS256", "sig", modulus, exponent);
        return new JwksResponse(List.of(key));
    }

    private void verifySignature(String token) {
        String[] segments = token.split("\\.");
        if (segments.length != 3) {
            throw new InvalidTokenException("Malformed JWT");
        }

        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(keyPair.getPublic());
            signature.update((segments[0] + "." + segments[1]).getBytes(StandardCharsets.US_ASCII));
            byte[] signatureBytes = BASE64_URL_DECODER.decode(segments[2]);
            if (!signature.verify(signatureBytes)) {
                throw new InvalidTokenException("JWT signature could not be verified");
            }

            byte[] headerBytes = BASE64_URL_DECODER.decode(segments[0]);
            Map<?, ?> header = objectMapper.readValue(headerBytes, Map.class);
            Object kid = header.get("kid");
            if (kid == null || !Objects.equals(kid.toString(), keyId)) {
                throw new InvalidTokenException("Unknown signing key");
            }
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Unable to verify token: " + e.getMessage());
        }
    }

    private String sign(Map<String, Object> payload) {
        Map<String, Object> header = Map.of(
                "alg", "RS256",
                "typ", "JWT",
                "kid", keyId
        );

        try {
            byte[] headerBytes = objectMapper.writeValueAsBytes(header);
            byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
            String encodedHeader = BASE64_URL_ENCODER.encodeToString(headerBytes);
            String encodedPayload = BASE64_URL_ENCODER.encodeToString(payloadBytes);
            String signingInput = encodedHeader + "." + encodedPayload;

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            byte[] signed = signature.sign();
            String encodedSignature = BASE64_URL_ENCODER.encodeToString(signed);
            return signingInput + "." + encodedSignature;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    private void registerClient(String clientId, String clientSecret, Set<String> scopes) {
        registeredClients.put(clientId, new RegisteredClient(clientId, hashSecret(clientSecret), scopes));
    }

    private Optional<String> resolveAudience(TokenRequest request) {
        if (request.getResource() != null && !request.getResource().isBlank()) {
            return Optional.of(request.getResource());
        }
        if (request.getAudience() != null && !request.getAudience().isBlank()) {
            return Optional.of(request.getAudience());
        }
        return Optional.empty();
    }

    private String hashSecret(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash client secret", e);
        }
    }

    private String generateKeyId(RSAPublicKey publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = publicKey.getEncoded();
            byte[] hash = digest.digest(encoded);
            return BASE64_URL_ENCODER.encodeToString(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to derive key identifier", e);
        }
    }

    private String toBase64Url(byte[] value) {
        if (value.length > 1 && value[0] == 0) {
            byte[] withoutSignByte = new byte[value.length - 1];
            System.arraycopy(value, 1, withoutSignByte, 0, withoutSignByte.length);
            return BASE64_URL_ENCODER.encodeToString(withoutSignByte);
        }
        return BASE64_URL_ENCODER.encodeToString(value);
    }

    private record RegisteredClient(String clientId, String hashedSecret, Set<String> allowedScopes) {
        boolean matchesSecret(String rawSecret) {
            return hashedSecret.equals(hash(rawSecret));
        }

        private String hash(String rawSecret) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashed = digest.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hashed);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Unable to hash client secret", e);
            }
        }
    }

    private record TokenMetadata(String clientId, String subject, Set<String> scopes, Instant expiresAt) {
    }
}
