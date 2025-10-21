package com.minimart.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp", schema = "final")
public class Otp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "code", nullable = false, length = 6)
    private String code;
    
    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;
    
    @Column(name = "verified")
    private Boolean verified;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public Otp() {
    }
    
    public Otp(Long id, Long userId, String code, LocalDateTime expireAt, Boolean verified, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.code = code;
        this.expireAt = expireAt;
        this.verified = verified;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public LocalDateTime getExpireAt() {
        return expireAt;
    }
    
    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }
    
    public Boolean getVerified() {
        return verified;
    }
    
    public void setVerified(Boolean verified) {
        this.verified = verified;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (verified == null) {
            verified = false;
        }
    }
}