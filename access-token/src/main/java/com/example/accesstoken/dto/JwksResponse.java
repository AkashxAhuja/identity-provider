package com.example.accesstoken.dto;

import java.util.List;

public class JwksResponse {

    private List<JwkKey> keys;

    public JwksResponse() {
    }

    public JwksResponse(List<JwkKey> keys) {
        this.keys = keys;
    }

    public List<JwkKey> getKeys() {
        return keys;
    }

    public void setKeys(List<JwkKey> keys) {
        this.keys = keys;
    }
}
