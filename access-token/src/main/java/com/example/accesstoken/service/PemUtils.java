package com.example.accesstoken.service;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class PemUtils {

    private PemUtils() {
    }

    static RSAPrivateKey readPrivateKey(String pem) {
        try {
            String sanitized = stripPem(pem, "PRIVATE KEY");
            byte[] decoded = Base64.getDecoder().decode(sanitized);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse RSA private key", ex);
        }
    }

    static RSAPublicKey readPublicKey(String pem) {
        try {
            String sanitized = stripPem(pem, "PUBLIC KEY");
            byte[] decoded = Base64.getDecoder().decode(sanitized);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse RSA public key", ex);
        }
    }

    private static String stripPem(String pem, String type) {
        if (pem == null) {
            throw new IllegalStateException("Key material is missing");
        }
        return pem.replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
    }
}
