package com.example.accesstoken.dto;

import java.time.Instant;
import java.util.List;

public class TokenValidationResponse {

    private boolean active;
    private String subject;
    private String clientId;
    private List<String> scope;
    private Instant expiresAt;

    public TokenValidationResponse() {
    }

    public TokenValidationResponse(boolean active, String subject, String clientId, List<String> scope, Instant expiresAt) {
        this.active = active;
        this.subject = subject;
        this.clientId = clientId;
        this.scope = scope;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
