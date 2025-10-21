package com.minimart.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateUserRequest {

    @JsonProperty("name")
    private String userName;
    
    @JsonProperty("phone_number")
    private String phone;
    
    @JsonProperty("image")
    private String profileImage;

    // Constructors
    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String userName, String phone, String profileImage) {
        this.userName = userName;
        this.phone = phone;
        this.profileImage = profileImage;
    }

    // Getters and Setters
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}