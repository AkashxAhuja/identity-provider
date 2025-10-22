package com.example.accesstoken.dto;

import jakarta.validation.constraints.NotBlank;

public class TokenValidationRequest {

    @NotBlank
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
