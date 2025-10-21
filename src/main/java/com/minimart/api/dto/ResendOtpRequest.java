package com.minimart.api.dto;

public class ResendOtpRequest {

    private String email;

    // Constructors
    public ResendOtpRequest() {
    }

    public ResendOtpRequest(String email) {
        this.email = email;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}