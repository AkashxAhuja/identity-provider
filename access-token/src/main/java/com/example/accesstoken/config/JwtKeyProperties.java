package com.example.accesstoken.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class JwtKeyProperties {

    private String jwsPrivateKey;
    private String jwsPublicKey;
    private String jwePublicKey;
    private String jwePrivateKey;

    public String getJwsPrivateKey() {
        return jwsPrivateKey;
    }

    public void setJwsPrivateKey(String jwsPrivateKey) {
        this.jwsPrivateKey = jwsPrivateKey;
    }

    public String getJwsPublicKey() {
        return jwsPublicKey;
    }

    public void setJwsPublicKey(String jwsPublicKey) {
        this.jwsPublicKey = jwsPublicKey;
    }

    public String getJwePublicKey() {
        return jwePublicKey;
    }

    public void setJwePublicKey(String jwePublicKey) {
        this.jwePublicKey = jwePublicKey;
    }

    public String getJwePrivateKey() {
        return jwePrivateKey;
    }

    public void setJwePrivateKey(String jwePrivateKey) {
        this.jwePrivateKey = jwePrivateKey;
    }
}
