package com.example.accesstoken.dto;

import java.time.Instant;
import java.util.List;

public class TokenResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private Instant issuedAt;
    private List<String> scope;

    public TokenResponse() {
    }

    public TokenResponse(String accessToken, String tokenType, long expiresIn, Instant issuedAt, List<String> scope) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.issuedAt = issuedAt;
        this.scope = scope;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }
}
