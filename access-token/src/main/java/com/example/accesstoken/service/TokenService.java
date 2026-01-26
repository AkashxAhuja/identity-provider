package com.example.accesstoken.service;

import com.example.accesstoken.dto.JwkKey;
import com.example.accesstoken.dto.JwksResponse;
import com.example.accesstoken.dto.TokenRequest;
import com.example.accesstoken.dto.TokenResponse;
import com.example.accesstoken.dto.TokenValidationResponse;
import com.example.accesstoken.exception.InvalidClientException;
import com.example.accesstoken.exception.InvalidTokenException;
import com.example.accesstoken.model.ApplicationClient;
import com.example.accesstoken.model.ApplicationSession;
import com.example.accesstoken.model.AuthMode;
import com.example.accesstoken.repository.ApplicationClientRepository;
import com.example.accesstoken.repository.ApplicationSessionRepository;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class TokenService {

    private static final String SUPPORTED_GRANT_TYPE = "client_credentials";
    private static final long DEFAULT_TOKEN_TTL_SECONDS = 3600L;
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final HashingService hashingService;
    private final ApplicationClientRepository clientRepository;
    private final ApplicationSessionRepository sessionRepository;
    private final JwtKeyProvider keyProvider;

    public TokenService(HashingService hashingService,
                        ApplicationClientRepository clientRepository,
                        ApplicationSessionRepository sessionRepository,
                        JwtKeyProvider keyProvider) {
        this.hashingService = hashingService;
        this.clientRepository = clientRepository;
        this.sessionRepository = sessionRepository;
        this.keyProvider = keyProvider;
    }

    public TokenResponse generateToken(TokenRequest request, String clientId, String clientSecret) {
        if (!SUPPORTED_GRANT_TYPE.equals(request.getGrantType())) {
            throw new InvalidClientException("Unsupported grant type: " + request.getGrantType());
        }

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new InvalidClientException("Client authentication required");
        }

        ApplicationClient client = clientRepository.findByClientIdAndAuthModeAndActiveTrue(clientId, AuthMode.BASIC)
                .orElseThrow(() -> new InvalidClientException("Invalid client credentials"));

        if (!hashingService.hashToBase64(clientSecret).equals(client.getClientSecret())) {
            throw new InvalidClientException("Invalid client credentials");
        }

        Set<String> requestedScopes = Set.copyOf(request.getScopes());
        Set<String> allowedScopes = client.getAllowedScopeSet();
        if (!allowedScopes.isEmpty() && !allowedScopes.containsAll(requestedScopes)) {
            throw new IllegalArgumentException("Requested scopes exceed client permissions");
        }

        Optional<String> audience = resolveAudience(request);
        Set<String> scopesToUse = requestedScopes.isEmpty() && !allowedScopes.isEmpty()
                ? allowedScopes
                : requestedScopes;

        return issueToken(client.getClientId(), client.getClientId(), scopesToUse, audience);
    }

    public TokenResponse generateTokenForApplicationClient(ApplicationClient client, TokenRequest request) {
        if (client == null) {
            throw new InvalidClientException("Client authentication required");
        }

        Set<String> requestedScopes = Set.copyOf(request.getScopes());
        Set<String> allowedScopes = client.getAllowedScopeSet();
        if (!allowedScopes.isEmpty() && !allowedScopes.containsAll(requestedScopes)) {
            throw new IllegalArgumentException("Requested scopes exceed client permissions");
        }

        Set<String> scopesToUse = requestedScopes.isEmpty() && !allowedScopes.isEmpty()
                ? allowedScopes
                : requestedScopes;

        String subject = client.getClientId();
        if (subject == null || subject.isBlank()) {
            subject = "application-client-" + client.getId();
        }
        String clientId = client.getClientId() == null || client.getClientId().isBlank()
                ? subject
                : client.getClientId();
        Optional<String> audience = resolveAudience(request);
        return issueToken(clientId, subject, scopesToUse, audience);
    }

    public TokenValidationResponse validate(String token) {
        ApplicationSession session = sessionRepository.findByTokenAndActiveTrue(token).orElse(null);
        decryptAndVerify(token);

        if (session == null) {
            return new TokenValidationResponse(false, null, null, null, null);
        }

        if (!session.isActive() || session.getExpiresAt().isBefore(Instant.now())) {
            return new TokenValidationResponse(false, null, null, null, null);
        }

        return new TokenValidationResponse(true, session.getSubject(), session.getClientId(),
                List.copyOf(session.getScopeSet()), session.getExpiresAt());
    }

    private TokenResponse issueToken(String clientId, String subject, Set<String> requestedScopes, Optional<String> audience) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(DEFAULT_TOKEN_TTL_SECONDS);
        String scope = String.join(" ", requestedScopes);
        String jti = UUID.randomUUID().toString();

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer("com.example.access-token")
                .subject(subject)
                .expirationTime(java.util.Date.from(expiresAt))
                .issueTime(java.util.Date.from(issuedAt))
                .claim("client_id", clientId)
                .jwtID(jti);
        audience.ifPresent(value -> claimsBuilder.audience(List.of(value)));
        if (!scope.isBlank()) {
            claimsBuilder.claim("scope", scope);
        }

        String token = signAndEncrypt(claimsBuilder.build());

        ApplicationSession session = new ApplicationSession();
        session.setToken(token);
        session.setClientId(clientId);
        session.setSubject(subject);
        session.setScope(scope);
        session.setIssuedAt(issuedAt);
        session.setExpiresAt(expiresAt);
        session.setJti(jti);
        session.setActive(true);
        sessionRepository.save(session);

        return new TokenResponse(token, "Bearer", DEFAULT_TOKEN_TTL_SECONDS, issuedAt, List.copyOf(requestedScopes));
    }

    public void revoke(String token) {
        ApplicationSession session = sessionRepository.findByToken(token).orElse(null);
        if (session == null) {
            decryptAndVerify(token);
            throw new InvalidTokenException("Token was not issued by this authorization server");
        }

        session.setActive(false);
        sessionRepository.save(session);
    }

    public JwksResponse jwks() {
        RSAPublicKey publicKey = keyProvider.getJwsPublicKey();
        String modulus = toBase64Url(publicKey.getModulus().toByteArray());
        String exponent = toBase64Url(publicKey.getPublicExponent().toByteArray());
        JwkKey key = new JwkKey("RSA", keyProvider.getJwsKeyId(), "RS256", "sig", modulus, exponent);
        return new JwksResponse(List.of(key));
    }

    private SignedJWT decryptAndVerify(String token) {
        try {
            JWEObject jweObject = JWEObject.parse(token);
            jweObject.decrypt(new RSADecrypter(keyProvider.getJwePrivateKey()));
            SignedJWT signedJWT = jweObject.getPayload().toSignedJWT();
            if (signedJWT == null) {
                throw new InvalidTokenException("Malformed JWT");
            }
            boolean valid = signedJWT.verify(new RSASSAVerifier(keyProvider.getJwsPublicKey()));
            if (!valid) {
                throw new InvalidTokenException("JWT signature could not be verified");
            }
            if (!keyProvider.getJwsKeyId().equals(signedJWT.getHeader().getKeyID())) {
                throw new InvalidTokenException("Unknown signing key");
            }
            return signedJWT;
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidTokenException("Unable to verify token: " + ex.getMessage());
        }
    }

    private String signAndEncrypt(JWTClaimsSet claims) {
        try {
            JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keyProvider.getJwsKeyId())
                    .type(JOSEObjectType.JWT)
                    .build();
            SignedJWT signedJWT = new SignedJWT(jwsHeader, claims);
            signedJWT.sign(new RSASSASigner(keyProvider.getJwsPrivateKey()));

            JWEHeader jweHeader = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                    .contentType("JWT")
                    .build();
            JWEObject jweObject = new JWEObject(jweHeader, new Payload(signedJWT));
            jweObject.encrypt(new RSAEncrypter(keyProvider.getJwePublicKey()));
            return jweObject.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to sign and encrypt JWT", ex);
        }
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

    private String toBase64Url(byte[] value) {
        if (value.length > 1 && value[0] == 0) {
            byte[] withoutSignByte = new byte[value.length - 1];
            System.arraycopy(value, 1, withoutSignByte, 0, withoutSignByte.length);
            return BASE64_URL_ENCODER.encodeToString(withoutSignByte);
        }
        return BASE64_URL_ENCODER.encodeToString(value);
    }
}
