package com.minimart.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResetPasswordRequest {

    private String email;
    private String code;
    
    @JsonProperty("new_password")
    private String newPassword;

    // Constructors
    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String email, String code, String newPassword) {
        this.email = email;
        this.code = code;
        this.newPassword = newPassword;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}