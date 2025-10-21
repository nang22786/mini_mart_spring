package com.minimart.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalDateTime;

@JsonPropertyOrder({"id", "name", "email", "phone_number", "role", "status", "image", "created_at", "updated_at", "isActive"})
public class UserDTO {

    @JsonProperty("id")
    private Long userId;
    
    @JsonProperty("name")
    private String userName;
    
    private String email;
    
    @JsonProperty("phone_number")
    private String phone;
    
    private String role;
    private String status;
    
    @JsonProperty("image")
    private String profileImage;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public UserDTO() {
    }

    public UserDTO(Long userId, String userName, String email, String phone,
                   String role, String status, String profileImage,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.profileImage = profileImage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsActive() {
        return "active".equals(status);
    }

    public void setIsActive(Boolean isActive) {
        this.status = isActive ? "active" : "inactive";
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}