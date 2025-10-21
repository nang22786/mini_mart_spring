package com.minimart.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthResponse {

    private boolean success;
    private String message;
    private UserDTO user;

    @JsonProperty("access_token")
    private String accessToken;

    // Constructors
    public AuthResponse() {
    }

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthResponse(boolean success, String message, UserDTO user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public AuthResponse(boolean success, String message, UserDTO user, String accessToken) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.accessToken = accessToken;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}