package com.minimart.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RefreshTokenRequest {

    @JsonProperty("refresh_token")
    private String refreshToken;

    // Constructors
    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getters and Setters
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}