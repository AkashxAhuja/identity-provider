package com.example.accesstoken.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TokenRequest {

    @NotBlank
    private String grantType;

    private String scope;

    private String resource;

    private String audience;

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public List<String> getScopes() {
        if (scope == null || scope.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(scope.split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
