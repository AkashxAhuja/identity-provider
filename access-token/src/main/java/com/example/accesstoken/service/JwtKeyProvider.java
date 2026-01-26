package com.example.accesstoken.service;

import com.example.accesstoken.config.JwtKeyProperties;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class JwtKeyProvider {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final RSAPrivateKey jwsPrivateKey;
    private final RSAPublicKey jwsPublicKey;
    private final RSAPublicKey jwePublicKey;
    private final RSAPrivateKey jwePrivateKey;
    private final String jwsKeyId;

    public JwtKeyProvider(JwtKeyProperties properties) {
        this.jwsPrivateKey = PemUtils.readPrivateKey(properties.getJwsPrivateKey());
        this.jwsPublicKey = PemUtils.readPublicKey(properties.getJwsPublicKey());
        this.jwePublicKey = PemUtils.readPublicKey(properties.getJwePublicKey());
        this.jwePrivateKey = PemUtils.readPrivateKey(properties.getJwePrivateKey());
        this.jwsKeyId = generateKeyId(jwsPublicKey);
    }

    public RSAPrivateKey getJwsPrivateKey() {
        return jwsPrivateKey;
    }

    public RSAPublicKey getJwsPublicKey() {
        return jwsPublicKey;
    }

    public RSAPublicKey getJwePublicKey() {
        return jwePublicKey;
    }

    public RSAPrivateKey getJwePrivateKey() {
        return jwePrivateKey;
    }

    public String getJwsKeyId() {
        return jwsKeyId;
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
}
